import React, { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { authApi } from '../api/client'

export default function VerifyEmail() {
  const [params] = useSearchParams()
  const token = params.get('token') || ''
  const [status, setStatus] = useState<'checking' | 'success' | 'error'>('checking')

  useEffect(() => {
    if (!token) {
      setStatus('error')
      return
    }
    authApi
      .verifyEmail(token)
      .then(() => setStatus('success'))
      .catch(() => setStatus('error'))
  }, [token])

  const copy = {
    checking: { title: 'Verifying your email…', subtitle: 'One moment.' },
    success: { title: 'Email verified', subtitle: 'Your account is ready to use.' },
    error: { title: 'Verification link invalid', subtitle: 'It may have expired. Request a new one from the login screen.' },
  }[status]

  return (
    <AuthLayout eyebrow="Account status" title={copy.title} subtitle={copy.subtitle}>
      <Link to="/login" className="text-signal-teal hover:underline text-sm">
        Back to log in
      </Link>
    </AuthLayout>
  )
}
