import React, { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import {
  assistantApi,
  leaderboardApi,
  LeaderboardEntry,
  Project,
  projectApi,
  Workspace,
  workspaceApi,
  WorkspaceMemberDto,
} from '../api/client'
import { WorkspaceSocket } from '../api/websocket'
import { AppShell } from '../components/AppShell'
import { FormField, PrimaryButton } from '../components/FormField'

export default function WorkspaceDetail() {
  const { workspaceId } = useParams()
  const id = Number(workspaceId)
  const navigate = useNavigate()

  const [workspace, setWorkspace] = useState<Workspace | null>(null)
  const [projects, setProjects] = useState<Project[]>([])
  const [members, setMembers] = useState<WorkspaceMemberDto[]>([])
  const [leaderboard, setLeaderboard] = useState<LeaderboardEntry[]>([])
  const [showProjectForm, setShowProjectForm] = useState(false)
  const [showInviteForm, setShowInviteForm] = useState(false)
  const [projectName, setProjectName] = useState('')
  const [inviteEmail, setInviteEmail] = useState('')
  const [error, setError] = useState('')
  const [assistantQuestion, setAssistantQuestion] = useState('')
  const [assistantAnswer, setAssistantAnswer] = useState('')
  const [asking, setAsking] = useState(false)
  const [liveNotice, setLiveNotice] = useState<string | null>(null)

  function load() {
    workspaceApi.get(id).then((res) => setWorkspace(res.data))
    projectApi.listForWorkspace(id).then((res) => setProjects(res.data))
    workspaceApi.members(id).then((res) => setMembers(res.data))
    leaderboardApi
      .getForWorkspace(id)
      .then((res) => setLeaderboard(res.data))
      .catch(() => {})
  }

  useEffect(load, [id])

  // Real-time WebSocket connection for live workspace updates
  useEffect(() => {
    const socket = new WorkspaceSocket(id, (eventType) => {
      const friendlyName = eventType.replace(/_/g, ' ').toLowerCase()
      setLiveNotice(`Live update: ${friendlyName}`)
      setTimeout(() => setLiveNotice(null), 3500)
      load()
    })
    return () => socket.disconnect()
  }, [id])

  async function handleCreateProject(e: React.FormEvent) {
    e.preventDefault()
    await projectApi.create(id, projectName)
    setProjectName('')
    setShowProjectForm(false)
    load()
  }

  async function handleInvite(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    try {
      await workspaceApi.invite(id, inviteEmail)
      setInviteEmail('')
      setShowInviteForm(false)
      load()
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Could not add this member')
    }
  }

  async function handleAskAssistant(e: React.FormEvent) {
    e.preventDefault()
    setAsking(true)
    setAssistantAnswer('')
    try {
      const res = await assistantApi.ask(id, assistantQuestion)
      setAssistantAnswer(res.data.answer)
    } finally {
      setAsking(false)
    }
  }

  if (!workspace)
    return (
      <AppShell>
        <p className="text-text-muted">Loading…</p>
      </AppShell>
    )

  return (
    <AppShell>
      {/* Live notification pill */}
      {liveNotice && (
        <div className="fixed bottom-6 right-6 z-50 bg-signal-teal text-ink font-semibold px-4 py-2 rounded-full shadow-lg text-xs flex items-center gap-2 animate-bounce">
          <span className="w-2 h-2 rounded-full bg-ink" />
          {liveNotice}
        </div>
      )}

      <div className="flex items-start justify-between mb-8">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="font-display text-2xl font-semibold">{workspace.name}</h1>
            <span className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-signal-teal/10 text-signal-teal border border-signal-teal/30">
              <span className="w-1.5 h-1.5 rounded-full bg-signal-teal animate-pulse" />
              Live WebSocket Sync
            </span>
          </div>
          {workspace.description && (
            <p className="text-text-muted text-sm mt-1">{workspace.description}</p>
          )}
        </div>
        <div className="flex gap-3 text-sm">
          <button
            onClick={() => navigate(`/workspaces/${id}/analytics`)}
            className="text-signal-teal hover:underline"
          >
            Analytics
          </button>
          <button
            onClick={() => navigate(`/workspaces/${id}/activity`)}
            className="text-signal-teal hover:underline"
          >
            Activity
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left 2 Cols: Projects & Team Leaderboard */}
        <div className="lg:col-span-2 space-y-8">
          <div>
            <div className="flex items-center justify-between mb-4">
              <h2 className="font-display font-semibold">Projects</h2>
              <button
                onClick={() => setShowProjectForm(!showProjectForm)}
                className="text-sm bg-signal-teal text-ink font-semibold rounded-lg px-3 py-1.5 hover:brightness-110"
              >
                + New Project
              </button>
            </div>

            {showProjectForm && (
              <form
                onSubmit={handleCreateProject}
                className="bg-ink-surface border border-ink-border rounded-xl p-4 mb-4"
              >
                <FormField label="Project name" value={projectName} onChange={setProjectName} />
                <PrimaryButton>Create project</PrimaryButton>
              </form>
            )}

            {projects.length === 0 ? (
              <p className="text-text-muted text-sm">No projects yet.</p>
            ) : (
              <div className="space-y-3">
                {projects.map((p) => (
                  <Link
                    key={p.id}
                    to={`/projects/${p.id}`}
                    className="block bg-ink-surface border border-ink-border rounded-xl p-4 hover:border-signal-teal/50 transition-colors"
                  >
                    <div className="flex items-center justify-between">
                      <span className="font-medium">{p.name}</span>
                      <span className="text-xs font-mono text-text-muted">{p.status}</span>
                    </div>
                    <div className="mt-2 text-xs text-text-muted">
                      {p.completedTaskCount}/{p.taskCount} tasks done
                    </div>
                  </Link>
                ))}
              </div>
            )}
          </div>

          {/* Gamification & Leaderboard Section */}
          <div className="bg-ink-surface border border-ink-border rounded-2xl p-6">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h2 className="font-display font-semibold text-lg flex items-center gap-2">
                  <span>🏆</span> Team Accountability Leaderboard
                </h2>
                <p className="text-text-muted text-xs mt-0.5">
                  Ranked by verified streak consistency and AI proof completion
                </p>
              </div>
            </div>

            {leaderboard.length === 0 ? (
              <p className="text-text-muted text-sm">No completions recorded yet.</p>
            ) : (
              <div className="divide-y divide-ink-border/50">
                {leaderboard.map((entry) => (
                  <div key={entry.userId} className="py-3 flex items-center justify-between text-sm">
                    <div className="flex items-center gap-3">
                      <span className="w-6 text-center font-bold text-base">
                        {entry.rank === 1 ? '🥇' : entry.rank === 2 ? '🥈' : entry.rank === 3 ? '🥉' : `#${entry.rank}`}
                      </span>
                      <div>
                        <div className="font-medium flex items-center gap-2">
                          {entry.fullName}
                          <span className="text-xs px-2 py-0.5 rounded-full bg-ink border border-ink-border text-text-muted">
                            {entry.badge}
                          </span>
                        </div>
                        <div className="text-xs text-text-muted">
                          {entry.totalVerifiedTasks} AI-verified task{entry.totalVerifiedTasks === 1 ? '' : 's'}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-4">
                      <div className="text-right">
                        <div className="font-mono text-xs font-semibold text-signal-teal flex items-center gap-1 justify-end">
                          <span>🔥</span> {entry.currentStreak} Day{entry.currentStreak === 1 ? '' : 's'}
                        </div>
                        <div className="text-[11px] text-text-muted">
                          Best: {entry.longestStreak}d
                        </div>
                      </div>
                      <div className="w-12 text-right font-mono text-sm font-semibold">
                        {entry.accountabilityScore}%
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Members & Quick Info */}
        <div>
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-display font-semibold">Members</h2>
            <button
              onClick={() => setShowInviteForm(!showInviteForm)}
              className="text-sm text-signal-teal hover:underline"
            >
              + Invite
            </button>
          </div>

          {showInviteForm && (
            <form
              onSubmit={handleInvite}
              className="bg-ink-surface border border-ink-border rounded-xl p-4 mb-4"
            >
              {error && <p className="text-signal-danger text-xs mb-2">{error}</p>}
              <FormField label="Email" type="email" value={inviteEmail} onChange={setInviteEmail} />
              <PrimaryButton>Add member</PrimaryButton>
            </form>
          )}

          <div className="space-y-2">
            {members.map((m) => (
              <div
                key={m.userId}
                className="bg-ink-surface border border-ink-border rounded-lg px-3 py-2 flex justify-between items-center text-sm"
              >
                <span>{m.fullName}</span>
                <span className="text-xs font-mono text-text-muted">{m.role}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Grounded AI Assistant Section */}
      <div className="bg-ink-surface border border-signal-teal/30 rounded-2xl p-6 mt-8">
        <h2 className="font-display font-semibold flex items-center gap-2 mb-1">
          <span className="w-2 h-2 rounded-full bg-signal-teal animate-pulse" />
          Ask Vigil AI
        </h2>
        <p className="text-text-muted text-sm mb-4">
          Ask anything about your tasks in this workspace — grounded in your real data, not guesses.
        </p>

        <form onSubmit={handleAskAssistant} className="flex gap-3 mb-4">
          <input
            value={assistantQuestion}
            onChange={(e) => setAssistantQuestion(e.target.value)}
            placeholder="What should I focus on today?"
            className="flex-1 bg-ink border border-ink-border rounded-lg px-3.5 py-2.5 text-sm focus:border-signal-teal outline-none"
          />
          <button
            type="submit"
            disabled={asking || !assistantQuestion.trim()}
            className="bg-signal-teal text-ink font-semibold rounded-lg px-4 py-2.5 text-sm hover:brightness-110 disabled:opacity-50 whitespace-nowrap"
          >
            {asking ? 'Thinking…' : 'Ask'}
          </button>
        </form>

        {assistantAnswer && (
          <div className="bg-ink border border-ink-border rounded-lg px-4 py-3 text-sm text-text-primary">
            {assistantAnswer}
          </div>
        )}
      </div>
    </AppShell>
  )
}