import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { FormField, PrimaryButton, ErrorBanner, OAuthButtons, Divider } from '../components/FormField'
import { useAuth } from '../context/AuthContext'
import { oauthUrl } from '../api/client'

export default function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(email, password)
      navigate('/dashboard')
    } catch (err: any) {
      setError(err?.response?.data?.message || 'Invalid email or password')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="Welcome back"
      title="Log in to Vigil AI"
      footer={
        <>
          Don't have an account?{' '}
          <Link to="/register" className="text-signal-teal hover:underline">
            Create one
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
        <FormField label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" />
        <FormField label="Password" type="password" value={password} onChange={setPassword} autoComplete="current-password" />
        <div className="flex justify-end mb-4 -mt-2">
          <Link to="/forgot-password" className="text-sm text-text-muted hover:text-signal-teal">
            Forgot password?
          </Link>
        </div>
        <PrimaryButton disabled={submitting}>{submitting ? 'Logging in…' : 'Log in'}</PrimaryButton>
      </form>
    </AuthLayout>
  )
}
