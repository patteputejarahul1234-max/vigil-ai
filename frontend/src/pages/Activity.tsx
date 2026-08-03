import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { AppShell } from '../components/AppShell'
import { activityApi, ActivityLogEntry } from '../api/client'

export default function Activity() {
  const { workspaceId } = useParams()
  const id = Number(workspaceId)
  const [logs, setLogs] = useState<ActivityLogEntry[]>([])

  useEffect(() => {
    activityApi.listForWorkspace(id).then((res) => setLogs(res.data))
  }, [id])

  return (
    <AppShell title="Activity">
      {logs.length === 0 ? (
        <p className="text-text-muted">No activity yet.</p>
      ) : (
        <div className="space-y-2">
          {logs.map((l) => (
            <div key={l.id} className="bg-ink-surface border border-ink-border rounded-xl p-4 text-sm">
              <span className="font-medium">{l.actorName}</span>{' '}
              <span className="text-text-muted">{l.details || l.action}</span>
              <p className="text-xs text-text-muted mt-1 font-mono">{new Date(l.createdAt).toLocaleString()}</p>
            </div>
          ))}
        </div>
      )}
    </AppShell>
  )
}
