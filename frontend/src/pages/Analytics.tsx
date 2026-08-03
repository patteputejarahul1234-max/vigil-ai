import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { AppShell } from '../components/AppShell'
import { analyticsApi, AnalyticsSummary } from '../api/client'

export default function Analytics() {
  const { workspaceId } = useParams()
  const id = Number(workspaceId)
  const [data, setData] = useState<AnalyticsSummary | null>(null)

  useEffect(() => {
    analyticsApi.getForWorkspace(id).then((res) => setData(res.data))
  }, [id])

  if (!data) return <AppShell><p className="text-text-muted">Loading…</p></AppShell>

  return (
    <AppShell title="Analytics">
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8">
        <StatCard label="Projects" value={data.totalProjects} />
        <StatCard label="Total tasks" value={data.totalTasks} />
        <StatCard label="Completion rate" value={`${data.completionRate}%`} accent />
        <StatCard label="Overdue" value={data.overdueTasks} danger={data.overdueTasks > 0} />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <BreakdownCard title="Tasks by status" data={data.tasksByStatus} />
        <BreakdownCard title="Tasks by priority" data={data.tasksByPriority} />
      </div>
    </AppShell>
  )
}

function StatCard({ label, value, accent, danger }: { label: string; value: string | number; accent?: boolean; danger?: boolean }) {
  return (
    <div className="bg-ink-surface border border-ink-border rounded-2xl p-5">
      <p className="text-xs text-text-muted font-mono uppercase tracking-widest mb-2">{label}</p>
      <p className={`text-2xl font-display font-semibold ${accent ? 'text-signal-teal' : danger ? 'text-signal-danger' : ''}`}>
        {value}
      </p>
    </div>
  )
}

function BreakdownCard({ title, data }: { title: string; data: Record<string, number> }) {
  const max = Math.max(1, ...Object.values(data))
  return (
    <div className="bg-ink-surface border border-ink-border rounded-2xl p-6">
      <h3 className="font-display font-semibold mb-4">{title}</h3>
      <div className="space-y-3">
        {Object.entries(data).map(([key, value]) => (
          <div key={key}>
            <div className="flex justify-between text-xs text-text-muted mb-1">
              <span>{key.replace('_', ' ')}</span>
              <span>{value}</span>
            </div>
            <div className="h-2 bg-ink rounded-full overflow-hidden">
              <div className="h-full bg-signal-teal rounded-full" style={{ width: `${(value / max) * 100}%` }} />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
