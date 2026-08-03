import React, { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { notificationApi } from '../api/client'

/**
 * Shared authenticated-area shell: top bar with workspace nav,
 * notification bell, and logout. Every Stage 2 page renders inside this.
 */
export function AppShell({ children, title }: { children: React.ReactNode; title?: string }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [unread, setUnread] = useState(0)

  useEffect(() => {
    notificationApi.unreadCount().then((res) => setUnread(res.data.count)).catch(() => {})
    const interval = setInterval(() => {
      notificationApi.unreadCount().then((res) => setUnread(res.data.count)).catch(() => {})
    }, 30000)
    return () => clearInterval(interval)
  }, [])

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-ink text-text-primary">
      <header className="border-b border-ink-border">
        <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
          <Link to="/workspaces" className="font-display font-semibold text-lg tracking-tight">
            Vigil AI
          </Link>
          <div className="flex items-center gap-6">
            <Link to="/notifications" className="relative text-text-muted hover:text-text-primary transition-colors">
              <BellIcon />
              {unread > 0 && (
                <span className="absolute -top-1.5 -right-1.5 bg-signal-amber text-ink text-[10px] font-bold rounded-full w-4 h-4 flex items-center justify-center">
                  {unread > 9 ? '9+' : unread}
                </span>
              )}
            </Link>
            <span className="text-sm text-text-muted">{user?.fullName}</span>
            <button onClick={handleLogout} className="text-sm text-text-muted hover:text-signal-danger transition-colors">
              Log out
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 py-10">
        {title && <h1 className="font-display text-2xl font-semibold mb-8">{title}</h1>}
        {children}
      </main>
    </div>
  )
}

function BellIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
      <path d="M18 8a6 6 0 10-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.73 21a2 2 0 01-3.46 0" />
    </svg>
  )
}
