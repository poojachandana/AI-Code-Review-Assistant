import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import PasswordInput from '../components/PasswordInput.jsx'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await login(email, password)
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.message || 'Login failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-md mx-auto mt-16 p-8 bg-white dark:bg-gray-800 rounded-xl shadow">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-gray-100">Welcome back</h1>
      {error && <p className="text-red-500 text-sm mb-4">{error}</p>}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">Email</label>
          <input
            type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
            className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
          />
        </div>
        <div>
          <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">Password</label>
          <PasswordInput
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <button disabled={loading} className="w-full bg-brand-600 hover:bg-brand-700 text-white py-2 rounded-md font-medium">
          {loading ? 'Signing in...' : 'Login'}
        </button>
      </form>
      <p className="text-sm text-gray-500 mt-4">
        No account? <Link to="/register" className="text-brand-600 hover:underline">Register</Link>
      </p>
      <p className="text-sm text-gray-500 mt-1">
        <Link to="/reset-password" className="text-brand-600 hover:underline">Forgot password?</Link>
      </p>
    </div>
  )
}
