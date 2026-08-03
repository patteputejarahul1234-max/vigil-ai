import React, { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { AppShell } from '../components/AppShell'
import { fileApi, taskApi, Task, TaskAttachment } from '../api/client'

export default function TaskDetail() {
  const { taskId } = useParams()
  const id = Number(taskId)

  const [task, setTask] = useState<Task | null>(null)
  const [attachments, setAttachments] = useState<TaskAttachment[]>([])
  const [uploading, setUploading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  function load() {
    taskApi.get(id).then((res) => setTask(res.data))
    fileApi.listForTask(id).then((res) => setAttachments(res.data))
  }

  useEffect(load, [id])

  async function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (!file) return
    setUploading(true)
    try {
      await fileApi.upload(id, file)
      load()
    } finally {
      setUploading(false)
      if (fileInputRef.current) fileInputRef.current.value = ''
    }
  }

  async function updateField(patch: Partial<Task>) {
    await taskApi.update(id, patch)
    load()
  }

  if (!task) return <AppShell><p className="text-text-muted">Loading…</p></AppShell>

  return (
    <AppShell>
      <div className="max-w-2xl">
        <h1 className="font-display text-2xl font-semibold mb-1">{task.title}</h1>
        {task.description && <p className="text-text-muted mb-6">{task.description}</p>}

        <div className="flex gap-4 mb-8">
          <div>
            <label className="block text-xs text-text-muted mb-1">Status</label>
            <select
              value={task.status}
              onChange={(e) => updateField({ status: e.target.value as Task['status'] })}
              className="bg-ink-surface border border-ink-border rounded-lg px-3 py-2 text-sm"
            >
              {['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE'].map((s) => (
                <option key={s} value={s}>{s.replace('_', ' ')}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs text-text-muted mb-1">Priority</label>
            <select
              value={task.priority}
              onChange={(e) => updateField({ priority: e.target.value as Task['priority'] })}
              className="bg-ink-surface border border-ink-border rounded-lg px-3 py-2 text-sm"
            >
              {['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((p) => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="bg-ink-surface border border-ink-border rounded-2xl p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-display font-semibold">Attachments</h2>
            <label className="text-sm text-signal-teal hover:underline cursor-pointer">
              {uploading ? 'Uploading…' : '+ Upload file'}
              <input ref={fileInputRef} type="file" className="hidden" onChange={handleFileChange} disabled={uploading} />
            </label>
          </div>

          {attachments.length === 0 ? (
            <p className="text-text-muted text-sm">No files yet — this is where proof-of-execution photos will live in Stage 4.</p>
          ) : (
            <ul className="space-y-2">
              {attachments.map((a) => (
                <li key={a.id} className="flex justify-between items-center text-sm border-b border-ink-border pb-2 last:border-0">
                  <span>{a.fileName}</span>
                  <a href={fileApi.downloadUrl(a.id)} className="text-signal-teal hover:underline text-xs" target="_blank" rel="noreferrer">
                    Download
                  </a>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </AppShell>
  )
}
