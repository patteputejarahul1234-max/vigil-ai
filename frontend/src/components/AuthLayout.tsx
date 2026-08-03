import React from 'react'

/**
 * Shared shell for every auth screen. The signature element is the
 * "verification ring" mark — a nod to Vigil AI's core mechanic
 * (proof-of-execution gets AI-verified, not just checked off).
 */
export function AuthLayout({
  eyebrow,
  title,
  subtitle,
  children,
  footer,
}: {
  eyebrow: string
  title: string
  subtitle?: string
  children: React.ReactNode
  footer?: React.ReactNode
}) {
  return (
    <div className="min-h-screen w-full flex items-center justify-center px-4 py-12 bg-ink relative overflow-hidden">
      {/* ambient grid, quiet */}
      <div
        className="absolute inset-0 opacity-[0.04] pointer-events-none"
        style={{
          backgroundImage:
            'linear-gradient(#ECEEF3 1px, transparent 1px), linear-gradient(90deg, #ECEEF3 1px, transparent 1px)',
          backgroundSize: '48px 48px',
        }}
      />

      <div className="w-full max-w-md relative">
        <div className="flex items-center gap-3 mb-10 justify-center">
          <VerificationMark />
          <span className="font-display font-semibold text-lg tracking-tight">Vigil AI</span>
        </div>

        <div className="bg-ink-surface border border-ink-border rounded-2xl p-8 shadow-2xl shadow-black/40">
          <p className="font-mono text-xs uppercase tracking-widest text-signal-teal mb-2">{eyebrow}</p>
          <h1 className="font-display text-2xl font-semibold mb-1">{title}</h1>
          {subtitle && <p className="text-text-muted text-sm mb-6">{subtitle}</p>}
          {!subtitle && <div className="mb-6" />}
          {children}
        </div>

        {footer && <div className="mt-6 text-center text-sm text-text-muted">{footer}</div>}
      </div>
    </div>
  )
}

function VerificationMark() {
  return (
    <div className="relative w-8 h-8 flex items-center justify-center">
      <div className="absolute inset-0 rounded-full border-2 border-signal-teal/40 animate-pulse" />
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
        <path
          d="M3 8.5L6.5 12L13 4.5"
          stroke="#14B8A6"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </div>
  )
}
