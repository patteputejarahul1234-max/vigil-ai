import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { AppShell } from '../components/AppShell'
import { FormField, PrimaryButton } from '../components/FormField'
import { workspaceApi, Workspace } from '../api/client'

export default function Workspaces() {
  const [workspaces, setWorkspaces] = useState<Workspace[]>([])
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function load() {
    workspaceApi.list().then((res) => setWorkspaces(res.data)).finally(() => setLoading(false))
  }

  useEffect(load, [])

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    try {
      await workspaceApi.create(name, description)
      setName('')
      setDescription('')
      setShowForm(false)
      load()
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AppShell title="Your Workspaces">
      <div className="flex justify-end mb-6">
        <button
          onClick={() => setShowForm(!showForm)}
          className="bg-signal-teal text-ink font-semibold rounded-lg px-4 py-2 text-sm hover:brightness-110 transition-all"
        >
          {showForm ? 'Cancel' : '+ New Workspace'}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="bg-ink-surface border border-ink-border rounded-2xl p-6 mb-8 max-w-md">
          <FormField label="Workspace name" value={name} onChange={setName} placeholder="e.g. Solo Build" />
          <FormField label="Description" value={description} onChange={setDescription} required={false} placeholder="Optional" />
          <PrimaryButton disabled={submitting}>{submitting ? 'Creating…' : 'Create workspace'}</PrimaryButton>
        </form>
      )}

      {loading ? (
        <p className="text-text-muted">Loading…</p>
      ) : workspaces.length === 0 ? (
        <p className="text-text-muted">No workspaces yet — create one to get started.</p>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {workspaces.map((w) => (
            <Link
              key={w.id}
              to={`/workspaces/${w.id}`}
              className="bg-ink-surface border border-ink-border rounded-2xl p-6 hover:border-signal-teal/50 transition-colors"
            >
              <h2 className="font-display font-semibold text-lg mb-1">{w.name}</h2>
              {w.description && <p className="text-text-muted text-sm mb-3">{w.description}</p>}
              <p className="text-xs text-text-muted font-mono">{w.memberCount} member{w.memberCount !== 1 ? 's' : ''}</p>
            </Link>
          ))}
        </div>
      )}
    </AppShell>
  )
}
