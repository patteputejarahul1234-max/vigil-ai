import React from 'react'

export function FormField({
  label,
  type = 'text',
  value,
  onChange,
  placeholder,
  required = true,
  autoComplete,
}: {
  label: string
  type?: string
  value: string
  onChange: (v: string) => void
  placeholder?: string
  required?: boolean
  autoComplete?: string
}) {
  return (
    <label className="block mb-4">
      <span className="block text-sm font-medium text-text-primary mb-1.5">{label}</span>
      <input
        type={type}
        value={value}
        required={required}
        autoComplete={autoComplete}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        className="w-full bg-ink border border-ink-border rounded-lg px-3.5 py-2.5 text-text-primary placeholder:text-text-muted/60 focus:border-signal-teal focus:ring-1 focus:ring-signal-teal outline-none transition-colors"
      />
    </label>
  )
}

export function PrimaryButton({
  children,
  type = 'submit',
  disabled,
  onClick,
}: {
  children: React.ReactNode
  type?: 'submit' | 'button'
  disabled?: boolean
  onClick?: () => void
}) {
  return (
    <button
      type={type}
      disabled={disabled}
      onClick={onClick}
      className="w-full bg-signal-teal text-ink font-semibold rounded-lg py-2.5 hover:brightness-110 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
    >
      {children}
    </button>
  )
}

export function ErrorBanner({ message }: { message: string }) {
  if (!message) return null
  return (
    <div className="mb-4 px-3.5 py-2.5 rounded-lg bg-signal-danger/10 border border-signal-danger/30 text-signal-danger text-sm">
      {message}
    </div>
  )
}

export function OAuthButtons({ onGoogle, onGithub }: { onGoogle: () => void; onGithub: () => void }) {
  return (
    <div className="grid grid-cols-2 gap-3 mb-6">
      <button
        onClick={onGoogle}
        type="button"
        className="flex items-center justify-center gap-2 border border-ink-border rounded-lg py-2.5 text-sm font-medium hover:border-signal-teal/50 transition-colors"
      >
        Google
      </button>
      <button
        onClick={onGithub}
        type="button"
        className="flex items-center justify-center gap-2 border border-ink-border rounded-lg py-2.5 text-sm font-medium hover:border-signal-teal/50 transition-colors"
      >
        GitHub
      </button>
    </div>
  )
}

export function Divider({ label }: { label: string }) {
  return (
    <div className="flex items-center gap-3 mb-6">
      <div className="h-px flex-1 bg-ink-border" />
      <span className="text-xs text-text-muted font-mono uppercase tracking-wide">{label}</span>
      <div className="h-px flex-1 bg-ink-border" />
    </div>
  )
}
