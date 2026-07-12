import React, { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { adminListUsers, adminDeleteUser, adminListProjects, adminDeleteProject, adminStats } from '../services/api.js'

export default function AdminDashboard() {
  const { user } = useAuth()
  const [stats, setStats] = useState(null)
  const [users, setUsers] = useState([])
  const [projects, setProjects] = useState([])
  const [tab, setTab] = useState('stats')

  useEffect(() => {
    adminStats().then(({ data }) => setStats(data))
    adminListUsers().then(({ data }) => setUsers(data))
    adminListProjects().then(({ data }) => setProjects(data))
  }, [])

  if (user?.role !== 'ROLE_ADMIN') {
    return <Navigate to="/dashboard" replace />
  }

  const handleDeleteUser = async (id) => {
    if (!confirm('Delete this user and all their projects?')) return
    await adminDeleteUser(id)
    setUsers((prev) => prev.filter((u) => u.id !== id))
  }

  const handleDeleteProject = async (id) => {
    if (!confirm('Delete this project?')) return
    await adminDeleteProject(id)
    setProjects((prev) => prev.filter((p) => p.projectId !== id))
  }

  return (
    <div className="max-w-5xl mx-auto mt-8 px-4 pb-16">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-gray-100">Admin Dashboard</h1>

      <div className="flex gap-2 mb-6">
        {['stats', 'users', 'projects'].map((t) => (
          <button key={t} onClick={() => setTab(t)}
            className={`px-4 py-2 rounded-md text-sm capitalize ${tab === t ? 'bg-brand-600 text-white' : 'bg-gray-200 dark:bg-gray-700 dark:text-gray-100'}`}>
            {t}
          </button>
        ))}
      </div>

      {tab === 'stats' && stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard label="Total Users" value={stats.totalUsers} />
          <StatCard label="Total Projects" value={stats.totalProjects} />
          <StatCard label="Total Reviews" value={stats.totalReviews} />
          <StatCard label="Avg Quality Score" value={`${stats.averageQualityScore}/100`} />
          <div className="col-span-2 md:col-span-4 bg-white dark:bg-gray-800 rounded-lg shadow p-4 mt-2">
            <h3 className="font-semibold text-gray-800 dark:text-gray-100 mb-2">Findings by Severity (platform-wide)</h3>
            <ul className="space-y-1 text-sm">
              {Object.entries(stats.severityBreakdown || {}).map(([sev, count]) => (
                <li key={sev} className="flex justify-between text-gray-600 dark:text-gray-300">
                  <span>{sev}</span><span className="font-semibold">{count}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}

      {tab === 'users' && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-100 dark:bg-gray-700 text-left">
              <tr>
                <th className="p-3">Name</th><th className="p-3">Email</th><th className="p-3">Role</th><th className="p-3"></th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id} className="border-t border-gray-100 dark:border-gray-700">
                  <td className="p-3 text-gray-800 dark:text-gray-100">{u.name}</td>
                  <td className="p-3 text-gray-500">{u.email}</td>
                  <td className="p-3 text-gray-500">{u.role.replace('ROLE_', '')}</td>
                  <td className="p-3 text-right">
                    <button onClick={() => handleDeleteUser(u.id)} className="text-red-500 hover:underline text-xs">Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tab === 'projects' && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
          <table className="w-full text-sm">
            <thead className="bg-gray-100 dark:bg-gray-700 text-left">
              <tr>
                <th className="p-3">Project</th><th className="p-3">Owner</th><th className="p-3">Score</th><th className="p-3"></th>
              </tr>
            </thead>
            <tbody>
              {projects.map((p) => (
                <tr key={p.projectId} className="border-t border-gray-100 dark:border-gray-700">
                  <td className="p-3 text-gray-800 dark:text-gray-100">{p.projectName}</td>
                  <td className="p-3 text-gray-500">{p.ownerEmail}</td>
                  <td className="p-3 text-gray-500">{p.latestScore ?? '—'}</td>
                  <td className="p-3 text-right">
                    <button onClick={() => handleDeleteProject(p.projectId)} className="text-red-500 hover:underline text-xs">Delete</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}

function StatCard({ label, value }) {
  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 text-center">
      <p className="text-2xl font-bold text-brand-600 dark:text-brand-500">{value}</p>
      <p className="text-xs text-gray-400 mt-1">{label}</p>
    </div>
  )
}
