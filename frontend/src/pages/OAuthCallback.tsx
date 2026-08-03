import React, { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { AuthLayout } from '../components/AuthLayout'
import { useAuth } from '../context/AuthContext'

export default function OAuthCallback() {
  const [params] = useSearchParams()
  const { setTokens, refreshUser } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    const accessToken = params.get('accessToken')
    const refreshToken = params.get('refreshToken')

    if (accessToken && refreshToken) {
      setTokens(accessToken, refreshToken)
      refreshUser().then(() => navigate('/dashboard'))
    } else {
      navigate('/login')
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return <AuthLayout eyebrow="Signing you in" title="One moment…" subtitle="Finishing up your login." children={null} />
}
