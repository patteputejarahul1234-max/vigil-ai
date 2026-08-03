import React, { useEffect, useState } from 'react'
import { AppShell } from '../components/AppShell'
import { notificationApi, AppNotificationDto } from '../api/client'

export default function Notifications() {
  const [items, setItems] = useState<AppNotificationDto[]>([])

  function load() {
    notificationApi.list().then((res) => setItems(res.data))
  }

  useEffect(load, [])

  async function handleRead(id: number) {
    await notificationApi.markRead(id)
    load()
  }

  async function handleReadAll() {
    await notificationApi.markAllRead()
    load()
  }

  return (
    <AppShell title="Notifications">
      {items.length > 0 && (
        <button onClick={handleReadAll} className="text-sm text-signal-teal hover:underline mb-4">
          Mark all as read
        </button>
      )}
      {items.length === 0 ? (
        <p className="text-text-muted">You're all caught up.</p>
      ) : (
        <div className="space-y-2">
          {items.map((n) => (
            <div
              key={n.id}
              onClick={() => !n.read && handleRead(n.id)}
              className={`bg-ink-surface border rounded-xl p-4 cursor-pointer transition-colors ${
                n.read ? 'border-ink-border' : 'border-signal-teal/50'
              }`}
            >
              <p className="text-sm">{n.message}</p>
              <p className="text-xs text-text-muted mt-1 font-mono">{new Date(n.createdAt).toLocaleString()}</p>
            </div>
          ))}
        </div>
      )}
    </AppShell>
  )
}
