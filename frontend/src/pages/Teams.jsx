import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { createTeam, listMyTeams, listTeamMembers, addTeamMember, removeTeamMember } from '../services/api.js'
import { useAuth } from '../context/AuthContext.jsx'

export default function Teams() {
  const { user } = useAuth()
  const [teams, setTeams] = useState([])
  const [newTeamName, setNewTeamName] = useState('')
  const [selectedTeam, setSelectedTeam] = useState(null)
  const [members, setMembers] = useState([])
  const [inviteEmail, setInviteEmail] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const load = () => listMyTeams().then(({ data }) => setTeams(data))

  useEffect(() => { load() }, [])

  const handleCreate = async (e) => {
    e.preventDefault();

    console.log("Create button clicked");

    setError('');

    if (!newTeamName.trim()) {
      console.log("Team name is empty");
      return;
    }

    console.log("Creating:", newTeamName);

    try {
      const res = await createTeam(newTeamName);
      console.log(res);

      setNewTeamName('');
      load();
    } catch (err) {
      console.log(err);
      console.log(err.response);

      setError(err.response?.data?.message || 'Could not create team');
    }
  }
  const openTeam = async (team) => {
    setSelectedTeam(team)
    setError('')
    setMessage('')
    const { data } = await listTeamMembers(team.teamId)
    setMembers(data)
  }

  const handleInvite = async (e) => {
    e.preventDefault()
    setError('')
    setMessage('')
    try {
      await addTeamMember(selectedTeam.teamId, inviteEmail)
      setMessage(`Invited ${inviteEmail}`)
      setInviteEmail('')
      const { data } = await listTeamMembers(selectedTeam.teamId)
      setMembers(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Could not add member')
    }
  }

  const handleRemove = async (userId) => {
    await removeTeamMember(selectedTeam.teamId, userId)
    const { data } = await listTeamMembers(selectedTeam.teamId)
    setMembers(data)
  }

  return (
    <div className="max-w-3xl mx-auto mt-8 px-4 pb-16">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-gray-100">Team Workspaces</h1>

      <form onSubmit={handleCreate} className="flex gap-2 mb-6">
        <input
          value={newTeamName} onChange={(e) => setNewTeamName(e.target.value)}
          placeholder="New team name (e.g. Backend Squad)"
          className="flex-1 px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
        />
        <button className="bg-brand-600 hover:bg-brand-700 text-white px-4 py-2 rounded-md text-sm">Create Team</button>
      </form>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div>
          <h2 className="font-semibold text-gray-800 dark:text-gray-100 mb-2">Your Teams</h2>
          {teams.length === 0 ? (
            <p className="text-sm text-gray-400">You're not part of any team yet.</p>
          ) : (
            <div className="space-y-2">
              {teams.map((t) => (
                <button
                  key={t.teamId}
                  onClick={() => openTeam(t)}
                  className={`w-full text-left border rounded-md p-3 dark:border-gray-700 ${
                    selectedTeam?.teamId === t.teamId ? 'border-brand-500 bg-brand-50 dark:bg-gray-700' : 'bg-white dark:bg-gray-800'
                  }`}
                >
                  <p className="font-medium text-gray-900 dark:text-gray-100">{t.name}</p>
                  <p className="text-xs text-gray-400">{t.role} · {t.memberCount} member(s)</p>
                </button>
              ))}
            </div>
          )}
        </div>

        {selectedTeam && (
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4">
            <h2 className="font-semibold text-gray-800 dark:text-gray-100 mb-1">{selectedTeam.name}</h2>
            <Link to={`/dashboard?team=${selectedTeam.teamId}`} className="text-xs text-brand-600 hover:underline">
              View team's submitted projects →
            </Link>

            {error && <p className="text-sm text-red-500 mt-3">{error}</p>}
            {message && <p className="text-sm text-green-500 mt-3">{message}</p>}

            {selectedTeam.role === 'OWNER' && (
              <form onSubmit={handleInvite} className="flex gap-2 mt-4">
                <input
                  type="email" required value={inviteEmail} onChange={(e) => setInviteEmail(e.target.value)}
                  placeholder="teammate@email.com"
                  className="flex-1 px-3 py-2 border rounded-md text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
                />
                <button className="bg-brand-600 hover:bg-brand-700 text-white px-3 py-2 rounded-md text-sm">Invite</button>
              </form>
            )}

            <ul className="mt-4 space-y-2">
              {members.map((m) => (
                <li key={m.userId} className="flex items-center justify-between text-sm">
                  <span className="text-gray-700 dark:text-gray-200">{m.name} <span className="text-gray-400">({m.email})</span></span>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-gray-400">{m.role}</span>
                    {selectedTeam.role === 'OWNER' && m.role !== 'OWNER' && (
                      <button onClick={() => handleRemove(m.userId)} className="text-xs text-red-500 hover:underline">Remove</button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  )
}
