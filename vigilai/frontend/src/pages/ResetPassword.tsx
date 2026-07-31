import React, { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { FormField, PrimaryButton, ErrorBanner } from '../components/FormField'
import { authApi } from '../api/client'

export default function ResetPassword() {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const navigate = useNavigate()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await authApi.resetPassword(token, password)
      navigate('/login')
    } catch (err: any) {
      setError(err?.response?.data?.message || 'This reset link is invalid or expired')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout eyebrow="Almost done" title="Set a new password">
      <ErrorBanner message={error} />
      <form onSubmit={handleSubmit}>
        <FormField
          label="New password"
          type="password"
          value={password}
          onChange={setPassword}
          autoComplete="new-password"
          placeholder="At least 8 characters"
        />
        <PrimaryButton disabled={submitting || !token}>{submitting ? 'Updating…' : 'Update password'}</PrimaryButton>
      </form>
    </AuthLayout>
  )
}
