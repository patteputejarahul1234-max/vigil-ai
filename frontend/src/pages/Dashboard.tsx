import React from 'react'
import { useAuth } from '../context/AuthContext'
import { Link, useNavigate } from 'react-router-dom'

/**
 * Stage 1 placeholder — profile view. Stage 2 replaces this with the
 * real task/streak dashboard.
 */
export default function Dashboard() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-ink text-text-primary px-6 py-10">
      <div className="max-w-2xl mx-auto">
        <div className="flex items-center justify-between mb-10">
          <span className="font-display font-semibold text-lg">Vigil AI</span>
          <button onClick={handleLogout} className="text-sm text-text-muted hover:text-signal-danger transition-colors">
            Log out
          </button>
        </div>

        <div className="bg-ink-surface border border-ink-border rounded-2xl p-8">
          <p className="font-mono text-xs uppercase tracking-widest text-signal-teal mb-2">Profile</p>
          <h1 className="font-display text-2xl font-semibold mb-6">
            Welcome, {user?.fullName?.split(' ')[0] || 'there'}
          </h1>

          <dl className="space-y-3 text-sm">
            <Row label="Email" value={user?.email} />
            <Row label="Verified" value={user?.emailVerified ? 'Yes' : 'Pending'} />
            <Row label="Role" value={user?.role} />
          </dl>

          <div className="mt-8 pt-6 border-t border-ink-border text-text-muted text-sm">
            <Link to="/workspaces" className="text-signal-teal hover:underline">
              Go to your workspaces →
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

function Row({ label, value }: { label: string; value?: string }) {
  return (
    <div className="flex justify-between">
      <dt className="text-text-muted">{label}</dt>
      <dd>{value}</dd>
    </div>
  )
}
