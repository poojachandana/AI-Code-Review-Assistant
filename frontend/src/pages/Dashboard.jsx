import React, { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { listProjects, listTeamProjects, deleteProject } from '../services/api.js'
import ReviewCard from '../components/ReviewCard.jsx'

export default function Dashboard() {
  const [searchParams] = useSearchParams()
  const teamId = searchParams.get('team')
  const [projects, setProjects] = useState([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const [filterScore, setFilterScore] = useState('ALL')

  const load = async (q) => {
    setLoading(true)
    try {
      const { data } = teamId ? await listTeamProjects(teamId, q) : await listProjects(q)
      setProjects(data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load('') }, [teamId])

  const handleSearch = (e) => {
    e.preventDefault()
    load(search)
  }

  const handleDelete = async (projectId) => {
    if (!confirm('Delete this project and its reviews?')) return
    await deleteProject(projectId)
    setProjects((prev) => prev.filter((p) => p.projectId !== projectId))
  }

  const filtered = projects.filter((p) => {
    if (filterScore === 'ALL') return true
    if (filterScore === 'HIGH' ) return (p.latestScore ?? 0) >= 80
    if (filterScore === 'MEDIUM') return (p.latestScore ?? 0) >= 50 && (p.latestScore ?? 0) < 80
    if (filterScore === 'LOW') return (p.latestScore ?? 0) < 50
    return true
  })

  return (
    <div className="max-w-3xl mx-auto mt-8 px-4">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
          {teamId ? 'Team Review Dashboard' : 'Review Dashboard'}
        </h1>
        <Link to="/upload" className="bg-brand-600 hover:bg-brand-700 text-white px-4 py-2 rounded-md text-sm">
          + New Review
        </Link>
      </div>

      <form onSubmit={handleSearch} className="flex gap-3 mb-4">
        <input
          value={search} onChange={(e) => setSearch(e.target.value)}
          placeholder="Search projects..."
          className="flex-1 px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
        />
        <select
          value={filterScore} onChange={(e) => setFilterScore(e.target.value)}
          className="px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
        >
          <option value="ALL">All scores</option>
          <option value="HIGH">High (80+)</option>
          <option value="MEDIUM">Medium (50-79)</option>
          <option value="LOW">Low (&lt;50)</option>
        </select>
        <button className="bg-gray-200 dark:bg-gray-700 dark:text-gray-100 px-4 py-2 rounded-md text-sm">Search</button>
      </form>

      {loading ? (
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
                <div key={i} className="h-20 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse" />
            ))}
          </div>
      ) : filtered.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <p>No reviews yet.</p>
          <Link to="/upload" className="text-brand-600 hover:underline">Submit your first code review</Link>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((p) => (
            <ReviewCard key={p.projectId} project={p} onDelete={handleDelete} />
          ))}
        </div>
      )}
    </div>
  )
}
