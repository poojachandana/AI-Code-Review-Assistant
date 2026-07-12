import React, { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Editor from '@monaco-editor/react'
import { uploadFile, submitSnippet, listMyTeams } from '../services/api.js'
import { useAuth } from '../context/AuthContext.jsx'

const LANGUAGE_TEMPLATES = {
  java: {
    fileName: 'Main.java',
    code: 'public class Main {\n\n    public static void main(String[] args) {\n        System.out.println("Hello, world!");\n    }\n}\n',
  },
  python: {
    fileName: 'main.py',
    code: 'def main():\n    print("Hello, world!")\n\n\nif __name__ == "__main__":\n    main()\n',
  },
  javascript: {
    fileName: 'main.js',
    code: 'function main() {\n  console.log("Hello, world!");\n}\n\nmain();\n',
  },
}

export default function UploadCode() {
  const { theme } = useAuth()
  const [mode, setMode] = useState('file') // 'file' | 'snippet'
  const [file, setFile] = useState(null)
  const [language, setLanguage] = useState('java')
  const [code, setCode] = useState(LANGUAGE_TEMPLATES.java.code)
  const [fileName, setFileName] = useState(LANGUAGE_TEMPLATES.java.fileName)
  const [projectName, setProjectName] = useState('')
  const [teams, setTeams] = useState([])
  const [teamId, setTeamId] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    listMyTeams().then(({ data }) => setTeams(data)).catch(() => {})
  }, [])

  const handleLanguageChange = (lang) => {
    setLanguage(lang)
    setFileName(LANGUAGE_TEMPLATES[lang].fileName)
    setCode(LANGUAGE_TEMPLATES[lang].code)
  }

  const handleUpload = async (e) => {
    e.preventDefault()
    setError('')
    if (!file) { setError('Please choose a file or .zip to upload'); return }
    setLoading(true)
    try {
      const { data } = await uploadFile(file, teamId || null)
      navigate(`/reviews/${data.reviewId}`)
    } catch (err) {
      setError(err.response?.data?.message || 'Upload/analysis failed')
    } finally {
      setLoading(false)
    }
  }

  const handleSnippet = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const { data } = await submitSnippet({ code, fileName, projectName, teamId: teamId || null })
      navigate(`/reviews/${data.reviewId}`)
    } catch (err) {
      setError(err.response?.data?.message || 'Analysis failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-3xl mx-auto mt-8 px-4 pb-16">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-gray-100">Submit Code for Review</h1>

      <div className="flex gap-2 mb-6">
        <button onClick={() => setMode('file')}
          className={`px-4 py-2 rounded-md text-sm ${mode === 'file' ? 'bg-brand-600 text-white' : 'bg-gray-200 dark:bg-gray-700 dark:text-gray-100'}`}>
          Upload File / ZIP
        </button>
        <button onClick={() => setMode('snippet')}
          className={`px-4 py-2 rounded-md text-sm ${mode === 'snippet' ? 'bg-brand-600 text-white' : 'bg-gray-200 dark:bg-gray-700 dark:text-gray-100'}`}>
          Paste Snippet (Monaco Editor)
        </button>
      </div>

      {teams.length > 0 && (
        <div className="mb-4">
          <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">Submit to team workspace (optional)</label>
          <select value={teamId} onChange={(e) => setTeamId(e.target.value)}
            className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100">
            <option value="">Personal (only visible to me)</option>
            {teams.map((t) => <option key={t.teamId} value={t.teamId}>{t.name}</option>)}
          </select>
        </div>
      )}

      {error && <p className="text-red-500 text-sm mb-4">{error}</p>}

      {mode === 'file' ? (
        <form onSubmit={handleUpload} className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow space-y-4">
          <input
            type="file" accept=".java,.zip,.py,.js,.jsx,.ts,.tsx"
            onChange={(e) => setFile(e.target.files[0])}
            className="w-full text-sm text-gray-600 dark:text-gray-300"
          />
          <p className="text-xs text-gray-400">Supports .java, .py, .js/.jsx/.ts/.tsx files, or a .zip of a whole project.</p>
          <button disabled={loading} className="bg-brand-600 hover:bg-brand-700 text-white px-4 py-2 rounded-md">
            {loading ? 'Analyzing...' : 'Analyze Code'}
          </button>
        </form>
      ) : (
        <form onSubmit={handleSnippet} className="bg-white dark:bg-gray-800 p-6 rounded-lg shadow space-y-4">
          <div className="flex gap-4 flex-wrap">
            <div>
              <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">Language</label>
              <select value={language} onChange={(e) => handleLanguageChange(e.target.value)}
                className="px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100">
                <option value="java">Java</option>
                <option value="python">Python</option>
                <option value="javascript">JavaScript</option>
              </select>
            </div>
            <div className="flex-1 min-w-[160px]">
              <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">File name</label>
              <input value={fileName} onChange={(e) => setFileName(e.target.value)}
                className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100" />
            </div>
            <div className="flex-1 min-w-[160px]">
              <label className="block text-sm text-gray-600 dark:text-gray-300 mb-1">Project name (optional)</label>
              <input value={projectName} onChange={(e) => setProjectName(e.target.value)}
                className="w-full px-3 py-2 border rounded-md dark:bg-gray-700 dark:border-gray-600 dark:text-gray-100" />
            </div>
          </div>

          <div className="border border-gray-200 dark:border-gray-700 rounded-md overflow-hidden">
            <Editor
              height="420px"
              language={language}
              value={code}
              onChange={(value) => setCode(value ?? '')}
              theme={theme === 'dark' ? 'vs-dark' : 'light'}
              options={{ minimap: { enabled: false }, fontSize: 13, scrollBeyondLastLine: false }}
            />
          </div>

          <p className="text-xs text-gray-400">
            Note: Checkstyle/PMD/bug-pattern static analysis run on Java code. Python/JavaScript snippets
            still get a full AI-powered review; complexity metrics are Java-specific for now.
          </p>

          <button disabled={loading} className="bg-brand-600 hover:bg-brand-700 text-white px-4 py-2 rounded-md">
            {loading ? 'Analyzing...' : 'Analyze Code'}
          </button>
        </form>
      )}
    </div>
  )
}
