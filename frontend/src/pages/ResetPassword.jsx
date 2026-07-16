import React, { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { resetPassword } from '../services/api.js'
import PasswordInput from '../components/PasswordInput.jsx'

export default function ResetPassword() {
  const [email, setEmail] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await resetPassword({ email, newPassword })
      setMessage('Password reset! Redirecting to login...')
      setTimeout(() => navigate('/login'), 1500)
    } catch (err) {
      setError(err.response?.data?.message || 'Reset failed')
    }
  }

  return (
    <div className="max-w-md mx-auto mt-16 p-8 bg-white dark:bg-gray-800 rounded-xl shadow">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-gray-100">Reset Password</h1>
      {error && <p className="text-red-500 text-sm mb-4">{error}</p>}
      {message && <p className="text-green-500 text-sm mb-4">{message}</p>}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">Email</label>
          <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
            className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100" />
        </div>
        <div>
          <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">New Password</label>
          <PasswordInput
              required
              minLength={6}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
          />
        </div>
        <button className="w-full bg-brand-600 hover:bg-brand-700 text-white py-2 rounded-md font-medium">
          Reset Password
        </button>
      </form>
      <p className="text-sm text-gray-500 mt-4">
        <Link to="/login" className="text-brand-600 hover:underline">Back to login</Link>
      </p>
    </div>
  )
}
