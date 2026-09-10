/**
 * Zero-dependency STOMP-over-WebSocket client for Vigil AI real-time updates.
 */

type MessageHandler = (data: any) => void

export class WorkspaceSocket {
  private ws: WebSocket | null = null
  private subId = 0
  private subscriptions: Map<string, { destination: string; handler: MessageHandler }> = new Map()
  private isConnected = false
  private reconnectTimer: any = null
  private shouldReconnect = true

  constructor(private workspaceId: number, private onEvent: (eventType: string, payload: any) => void) {
    this.connect()
  }

  private getSocketUrl(): string {
    const apiBase = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
    const wsBase = apiBase.replace(/^http/, 'ws')
    return `${wsBase}/ws-direct`
  }

  private connect() {
    try {
      const url = this.getSocketUrl()
      this.ws = new WebSocket(url)

      this.ws.onopen = () => {
        // Send STOMP CONNECT frame
        const connectFrame = 'CONNECT\naccept-version:1.1,1.2\nheart-beat:10000,10000\n\n\0'
        this.ws?.send(connectFrame)
      }

      this.ws.onmessage = (event) => {
        const raw = event.data as string
        if (raw.startsWith('CONNECTED')) {
          this.isConnected = true
          // Subscribe to workspace topic
          this.subscribe(`/topic/workspace/${this.workspaceId}`, (msg) => {
            if (msg && msg.eventType) {
              this.onEvent(msg.eventType, msg.payload)
            }
          })
        } else if (raw.startsWith('MESSAGE')) {
          const bodyIndex = raw.indexOf('\n\n')
          if (bodyIndex !== -1) {
            const body = raw.substring(bodyIndex + 2).replace(/\0$/, '').trim()
            try {
              const parsed = JSON.parse(body)
              if (parsed && parsed.eventType) {
                this.onEvent(parsed.eventType, parsed.payload)
              }
            } catch (err) {
              console.debug('Failed to parse WS payload', err)
            }
          }
        }
      }

      this.ws.onclose = () => {
        this.isConnected = false
        if (this.shouldReconnect) {
          this.reconnectTimer = setTimeout(() => this.connect(), 4000)
        }
      }

      this.ws.onerror = () => {
        this.ws?.close()
      }
    } catch {
      // Graceful fallback
    }
  }

  private subscribe(destination: string, handler: MessageHandler) {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return
    const id = `sub-${this.subId++}`
    this.subscriptions.set(id, { destination, handler })
    const frame = `SUBSCRIBE\nid:${id}\ndestination:${destination}\n\n\0`
    this.ws.send(frame)
  }

  public disconnect() {
    this.shouldReconnect = false
    clearTimeout(this.reconnectTimer)
    if (this.ws) {
      try {
        this.ws.send('DISCONNECT\n\n\0')
        this.ws.close()
      } catch {}
    }
  }
}
