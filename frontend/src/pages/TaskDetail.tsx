import React, { useEffect, useRef, useState } from 'react'
import { useParams } from 'react-router-dom'
import { fileApi, proofApi, ProofSubmissionResult, SmartSuggestion, suggestionApi, Task, taskApi, TaskAttachment } from '../api/client'
import { AppShell } from '../components/AppShell'

export default function TaskDetail() {
  const { taskId } = useParams()
  const id = Number(taskId)

  const [task, setTask] = useState<Task | null>(null)
  const [attachments, setAttachments] = useState<TaskAttachment[]>([])
  const [uploading, setUploading] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  // Proof of Execution state
  const [submittingProof, setSubmittingProof] = useState(false)
  const [proofResult, setProofResult] = useState<ProofSubmissionResult | null>(null)
  const proofInputRef = useRef<HTMLInputElement>(null)

  // Smart Suggestions state
  const [suggestion, setSuggestion] = useState<SmartSuggestion | null>(null)

  function load() {
    taskApi.get(id).then((res) => setTask(res.data))
    fileApi.listForTask(id).then((res) => setAttachments(res.data))
    suggestionApi.getForTask(id).then((res) => setSuggestion(res.data))
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

  async function handleProofSubmit(e: React.ChangeEvent<HTMLInputElement>) {
    const photo = e.target.files?.[0]
    if (!photo) return
    setSubmittingProof(true)
    setProofResult(null)
    try {
      const res = await proofApi.submit(id, photo)
      setProofResult(res.data)
      if (res.data.verified) load() // refresh task so the status shown updates to DONE
    } finally {
      setSubmittingProof(false)
      if (proofInputRef.current) proofInputRef.current.value = ''
    }
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

        {/* --- Proof of Execution: the Stage 4 headline feature --- */}
        <div className="bg-ink-surface border border-signal-teal/30 rounded-2xl p-6 mb-6">
          <div className="flex items-center justify-between mb-2">
            <h2 className="font-display font-semibold flex items-center gap-2">
              <span className="w-2 h-2 rounded-full bg-signal-teal animate-pulse" />
              Proof of Execution
            </h2>
          </div>
          <p className="text-text-muted text-sm mb-4">
            Don't just check it off — submit a photo and let AI verify you actually did it.
          </p>

          <label className="inline-block bg-signal-teal text-ink font-semibold rounded-lg px-4 py-2.5 text-sm cursor-pointer hover:brightness-110 transition-all disabled:opacity-50">
            {submittingProof ? 'Verifying with AI…' : 'Submit Proof Photo'}
            <input
              ref={proofInputRef}
              type="file"
              accept="image/*"
              capture="environment"
              className="hidden"
              onChange={handleProofSubmit}
              disabled={submittingProof}
            />
          </label>

          {proofResult && (
            <div
              className={`mt-4 rounded-lg px-4 py-3 border text-sm ${
                proofResult.verified
                  ? 'bg-signal-teal/10 border-signal-teal/40 text-signal-teal'
                  : 'bg-signal-danger/10 border-signal-danger/40 text-signal-danger'
              }`}
            >
              <div className="flex items-center gap-2 font-semibold">
                {proofResult.verified ? '✓ Verified by AI' : '✗ Not verified'}
              </div>
              <p className="mt-1 text-text-muted">{proofResult.aiReason}</p>
              {proofResult.verified && (
                <p className="mt-1 text-xs text-text-muted">Task marked as Done.</p>
              )}
            </div>
          )}

          {suggestion && (
            <div className="mt-4 rounded-lg px-4 py-3 bg-ink border border-ink-border text-sm">
              <p className="text-xs text-signal-amber font-mono uppercase tracking-wide mb-1">
                Smart Suggestion
              </p>
              <p className="text-text-muted">{suggestion.message}</p>
              {suggestion.hasEnoughData && (
                <p className="text-xs text-text-muted mt-1">
                  Based on {suggestion.completedCount} verified completions.
                </p>
              )}
            </div>
          )}
        </div>

        {/* --- Ordinary attachments, unrelated to proof verification --- */}
        <div className="bg-ink-surface border border-ink-border rounded-2xl p-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-display font-semibold">Attachments</h2>
            <label className="text-sm text-signal-teal hover:underline cursor-pointer">
              {uploading ? 'Uploading…' : '+ Upload file'}
              <input ref={fileInputRef} type="file" className="hidden" onChange={handleFileChange} disabled={uploading} />
            </label>
          </div>

          {attachments.length === 0 ? (
            <p className="text-text-muted text-sm">No files yet.</p>
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