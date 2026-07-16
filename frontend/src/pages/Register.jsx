import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await register(name, email, password)
      navigate('/dashboard')
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.errors?.password || 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-md mx-auto mt-16 p-8 bg-white dark:bg-gray-800 rounded-xl shadow">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-gray-100">Create your account</h1>
      {error && <p className="text-red-500 text-sm mb-4">{typeof error === 'string' ? error : JSON.stringify(error)}</p>}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">Name</label>
          <input
              required
              value={name}
              onChange={(e) => setName(e.target.value.replace(/[^a-zA-Z\s]/g, ''))}
              className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
          />
        </div>
        <div>
          <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">Email</label>
          <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
            className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100" />
        </div>
        <div>
          <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">Password (min 6 chars)</label>
          <PasswordInput
              required
              minLength={6}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <button disabled={loading} className="w-full bg-brand-600 hover:bg-brand-700 text-white py-2 rounded-md font-medium">
          {loading ? 'Creating account...' : 'Register'}
        </button>
      </form>
      <p className="text-sm text-gray-500 mt-4">
        Already have an account? <Link to="/login" className="text-brand-600 hover:underline">Login</Link>
      </p>
    </div>
  )
}
