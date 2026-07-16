import React, { useState } from 'react'
import { useAuth } from '../context/AuthContext.jsx'
import { updateProfile } from '../services/api.js'

export default function Profile() {
  const { user } = useAuth()
  const [name, setName] = useState(user?.name || '')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setMessage('')
    try {
      await updateProfile({ name, currentPassword, newPassword })
      setMessage('Profile updated successfully')
      const stored = JSON.parse(sessionStorage.getItem('user'))
      sessionStorage.setItem('user', JSON.stringify({ ...stored, name }))
    } catch (err) {
      setError(err.response?.data?.message || 'Update failed')
    }
  }

  return (
    <div className="max-w-md mx-auto mt-12 p-8 bg-white dark:bg-gray-800 rounded-xl shadow">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-gray-100">Your Profile</h1>
      <p className="text-sm text-gray-500 mb-6">{user?.email}</p>
      {error && <p className="text-red-500 text-sm mb-4">{error}</p>}
      {message && <p className="text-green-500 text-sm mb-4">{message}</p>}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">Name</label>
          <input value={name} onChange={(e) => setName(e.target.value)}
            className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100" />
        </div>

        <hr className="border-gray-200 dark:border-gray-700" />
        <p className="text-sm text-gray-500">Change password (optional)</p>
        <div>
          <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">Current Password</label>
          <input type="password" value={currentPassword} onChange={(e) => setCurrentPassword(e.target.value)}
            className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100" />
        </div>
        <div>
          <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">New Password</label>
          <input type="password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)}
            className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100" />
        </div>
        <button className="w-full bg-brand-600 hover:bg-brand-700 text-white py-2 rounded-md font-medium">
          Save Changes
        </button>
      </form>
    </div>
  )
}
