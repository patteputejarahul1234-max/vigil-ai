import React, { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { AppShell } from '../components/AppShell'
import { FormField, PrimaryButton } from '../components/FormField'
import { projectApi, taskApi, Project, Task } from '../api/client'

const STATUS_COLUMNS: Task['status'][] = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE']

const PRIORITY_COLOR: Record<Task['priority'], string> = {
  LOW: 'text-text-muted',
  MEDIUM: 'text-signal-teal',
  HIGH: 'text-signal-amber',
  URGENT: 'text-signal-danger',
}

export default function ProjectDetail() {
  const { projectId } = useParams()
  const id = Number(projectId)

  const [project, setProject] = useState<Project | null>(null)
  const [tasks, setTasks] = useState<Task[]>([])
  const [showForm, setShowForm] = useState(false)
  const [title, setTitle] = useState('')
  const [keyword, setKeyword] = useState('')
  const [priorityFilter, setPriorityFilter] = useState('')

  function load() {
    projectApi.get(id).then((res) => setProject(res.data))
    taskApi.listForProject(id).then((res) => setTasks(res.data))
  }

  useEffect(load, [id])

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    await taskApi.create(id, { title })
    setTitle('')
    setShowForm(false)
    load()
  }

  async function runFilter() {
    const res = await taskApi.search({
      projectId: id,
      keyword: keyword || undefined,
      priority: priorityFilter || undefined,
    })
    setTasks(res.data)
  }

  async function moveTask(taskId: number, status: Task['status']) {
    await taskApi.update(taskId, { status })
    load()
  }

  if (!project) return <AppShell><p className="text-text-muted">Loading…</p></AppShell>

  return (
    <AppShell title={project.name}>
      <div className="flex flex-wrap items-center gap-3 mb-6">
        <input
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
          placeholder="Search tasks…"
          className="bg-ink-surface border border-ink-border rounded-lg px-3 py-2 text-sm w-56 focus:border-signal-teal outline-none"
        />
        <select
          value={priorityFilter}
          onChange={(e) => setPriorityFilter(e.target.value)}
          className="bg-ink-surface border border-ink-border rounded-lg px-3 py-2 text-sm"
        >
          <option value="">Any priority</option>
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
          <option value="URGENT">Urgent</option>
        </select>
        <button onClick={runFilter} className="text-sm text-signal-teal hover:underline">
          Apply filters
        </button>
        <button onClick={() => { setKeyword(''); setPriorityFilter(''); load() }} className="text-sm text-text-muted hover:underline">
          Reset
        </button>

        <div className="flex-1" />
        <button
          onClick={() => setShowForm(!showForm)}
          className="text-sm bg-signal-teal text-ink font-semibold rounded-lg px-3 py-1.5 hover:brightness-110"
        >
          + New Task
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="bg-ink-surface border border-ink-border rounded-xl p-4 mb-6 max-w-md">
          <FormField label="Task title" value={title} onChange={setTitle} />
          <PrimaryButton>Create task</PrimaryButton>
        </form>
      )}

      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        {STATUS_COLUMNS.map((status) => (
          <div key={status}>
            <h3 className="font-mono text-xs uppercase tracking-widest text-text-muted mb-3">
              {status.replace('_', ' ')} ({tasks.filter((t) => t.status === status).length})
            </h3>
            <div className="space-y-3">
              {tasks.filter((t) => t.status === status).map((t) => (
                <div key={t.id} className="bg-ink-surface border border-ink-border rounded-xl p-3">
                  <Link to={`/tasks/${t.id}`} className="font-medium text-sm hover:text-signal-teal transition-colors">
                    {t.title}
                  </Link>
                  <div className="flex items-center justify-between mt-2">
                    <span className={`text-xs font-mono ${PRIORITY_COLOR[t.priority]}`}>{t.priority}</span>
                    {t.attachmentCount > 0 && (
                      <span className="text-xs text-text-muted">{t.attachmentCount} file{t.attachmentCount !== 1 ? 's' : ''}</span>
                    )}
                  </div>
                  <select
                    value={t.status}
                    onChange={(e) => moveTask(t.id, e.target.value as Task['status'])}
                    className="mt-2 w-full text-xs bg-ink border border-ink-border rounded px-2 py-1"
                  >
                    {STATUS_COLUMNS.map((s) => (
                      <option key={s} value={s}>{s.replace('_', ' ')}</option>
                    ))}
                  </select>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </AppShell>
  )
}
