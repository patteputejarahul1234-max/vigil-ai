import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { FormField, PrimaryButton, ErrorBanner } from '../components/FormField'
import { authApi } from '../api/client'

export default function ForgotPassword() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [sent, setSent] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await authApi.forgotPassword(email)
      setSent(true)
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Something went wrong')
    } finally {
      setSubmitting(false)
    }
  }

  if (sent) {
    return (
      <AuthLayout eyebrow="Check your inbox" title="Reset link sent" subtitle={`If an account exists for ${email}, a reset link is on its way.`}>
        <Link to="/login" className="text-signal-teal hover:underline text-sm">
          Back to log in
        </Link>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout eyebrow="Trouble logging in?" title="Reset your password" subtitle="We'll email you a link to set a new one.">
      <ErrorBanner message={error} />
      <form onSubmit={handleSubmit}>
        <FormField label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" />
        <PrimaryButton disabled={submitting}>{submitting ? 'Sending…' : 'Send reset link'}</PrimaryButton>
      </form>
    </AuthLayout>
  )
}
