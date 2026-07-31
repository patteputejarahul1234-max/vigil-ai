import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { FormField, PrimaryButton, ErrorBanner, OAuthButtons, Divider } from '../components/FormField'
import { authApi, oauthUrl } from '../api/client'

export default function Register() {
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)
  const navigate = useNavigate()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await authApi.register(fullName, email, password)
      setDone(true)
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Could not create your account')
    } finally {
      setSubmitting(false)
    }
  }

  if (done) {
    return (
      <AuthLayout eyebrow="One more step" title="Check your inbox" subtitle={`We sent a verification link to ${email}.`}>
        <PrimaryButton type="button" onClick={() => navigate('/login')}>
          Back to log in
        </PrimaryButton>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout
      eyebrow="Get started"
      title="Create your account"
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="text-signal-teal hover:underline">
            Log in
          </Link>
        </>
      }
    >
      <OAuthButtons
        onGoogle={() => (window.location.href = oauthUrl('google'))}
        onGithub={() => (window.location.href = oauthUrl('github'))}
      />
      <Divider label="or with email" />
      <ErrorBanner message={error} />
      <form onSubmit={handleSubmit}>
        <FormField label="Full name" value={fullName} onChange={setFullName} autoComplete="name" />
        <FormField label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" />
        <FormField label="Password" type="password" value={password} onChange={setPassword} autoComplete="new-password" placeholder="At least 8 characters" />
        <PrimaryButton disabled={submitting}>{submitting ? 'Creating account…' : 'Create account'}</PrimaryButton>
      </form>
    </AuthLayout>
  )
}
