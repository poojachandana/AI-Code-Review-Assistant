import React, { useEffect, useState } from 'react'
import Editor from '@monaco-editor/react'
import { listProjectFiles, refactorFile } from '../services/api.js'
import { useAuth } from '../context/AuthContext.jsx'

function guessLanguage(fileName) {
  if (fileName.endsWith('.py')) return 'python'
  if (fileName.endsWith('.ts') || fileName.endsWith('.tsx')) return 'typescript'
  if (fileName.endsWith('.js') || fileName.endsWith('.jsx')) return 'javascript'
  return 'java'
}

export default function RefactorPanel({ projectId }) {
  const { theme } = useAuth()
  const [files, setFiles] = useState([])
  const [selectedFile, setSelectedFile] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    listProjectFiles(projectId).then(({ data }) => {
      setFiles(data)
      if (data.length > 0) setSelectedFile(data[0])
    }).catch(() => {})
  }, [projectId])

  const handleRefactor = async () => {
    if (!selectedFile) return
    setLoading(true)
    setError('')
    setResult(null)
    try {
      const { data } = await refactorFile(projectId, selectedFile)
      if (!data.available) {
        setError(data.error || 'AI refactor unavailable (no API key configured on the server).')
      } else {
        setResult(data.refactoredCode)
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Refactor request failed')
    } finally {
      setLoading(false)
    }
  }

  if (files.length === 0) return null

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-5 mb-6">
      <h2 className="font-semibold text-gray-800 dark:text-gray-100 mb-3">AI-Powered Code Refactoring</h2>
      <div className="flex flex-wrap gap-2 mb-3">
        <select
          value={selectedFile}
          onChange={(e) => { setSelectedFile(e.target.value); setResult(null) }}
          className="px-3 py-2 border rounded-md text-sm dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100"
        >
          {files.map((f) => <option key={f} value={f}>{f}</option>)}
        </select>
        <button
          onClick={handleRefactor}
          disabled={loading}
          className="text-sm bg-brand-600 hover:bg-brand-700 text-white px-3 py-1.5 rounded-md"
        >
          {loading ? 'Refactoring...' : '✨ Get AI Refactor'}
        </button>
      </div>

      {error && <p className="text-sm text-red-500 mb-2">{error}</p>}

      {result && (
        <div className="border border-gray-200 dark:border-gray-700 rounded-md overflow-hidden">
          <Editor
            height="400px"
            language={guessLanguage(selectedFile)}
            value={result}
            theme={theme === 'dark' ? 'vs-dark' : 'light'}
            options={{ readOnly: true, minimap: { enabled: false }, fontSize: 13 }}
          />
        </div>
      )}
    </div>
  )
}
