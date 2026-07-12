import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getReview, getDocumentation, getApiDocs, getReadmeSummary, exportReportUrl } from '../services/api.js'
import FindingItem from '../components/FindingItem.jsx'
import RefactorPanel from '../components/RefactorPanel.jsx'
import ScoreGauge from '../components/ScoreGauge.jsx'
import { MetricsBarChart, SeverityDistributionChart } from '../components/ComplexityChart.jsx'


const FILTERS = ['ALL', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW', 'INFO']

const DOC_TABS = [
  { key: 'class', label: '📄 Class/Method Docs', fetcher: getDocumentation },
  { key: 'api', label: '🔌 API Documentation', fetcher: getApiDocs },
  { key: 'readme', label: '📋 README Summary', fetcher: getReadmeSummary },
]



export default function ReviewDetail() {
  const { reviewId } = useParams()
  const [review, setReview] = useState(null)
  const [filter, setFilter] = useState('ALL')
  const [docsCache, setDocsCache] = useState({})
  const [activeDocTab, setActiveDocTab] = useState(null)
  const [docsLoading, setDocsLoading] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getReview(reviewId).then(({ data }) => setReview(data)).finally(() => setLoading(false))
  }, [reviewId])

  const loadDocTab = async (tabKey) => {
    setActiveDocTab(tabKey)
    if (docsCache[tabKey] || !review) return
    setDocsLoading(true)
    try {
      const tab = DOC_TABS.find((t) => t.key === tabKey)
      const { data } = await tab.fetcher(review.projectId)
      setDocsCache((prev) => ({ ...prev, [tabKey]: data }))
    } catch (err) {
      setDocsCache((prev) => ({ ...prev, [tabKey]: err.response?.data?.message || 'Could not generate this document.' }))
    } finally {
      setDocsLoading(false)
    }
  }

  if (loading) return <p className="text-center mt-16 text-gray-400">Loading review...</p>
  if (!review) return <p className="text-center mt-16 text-red-500">Review not found.</p>

  const findings = filter === 'ALL' ? review.findings : review.findings.filter((f) => f.severity === filter)

  return (
    <div className="max-w-4xl mx-auto mt-8 px-4 pb-16">
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 dark:text-gray-100">{review.projectName}</h1>
          <p className="text-sm text-gray-400">{new Date(review.createdAt).toLocaleString()}</p>
        </div>
        <ScoreGauge score={review.reviewScore} size={110} />
      </div>
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-5 mb-6">
        <h2 className="font-semibold text-gray-800 dark:text-gray-100 mb-2">Summary</h2>
        <p className="text-gray-600 dark:text-gray-300 text-sm">{review.summary}</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-5">
          <h2 className="font-semibold text-gray-800 dark:text-gray-100 mb-2">Complexity Metrics</h2>
          <MetricsBarChart review={review} />
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-5">
          <h2 className="font-semibold text-gray-800 dark:text-gray-100 mb-2">Findings by Severity</h2>
          <SeverityDistributionChart findings={review.findings} />
        </div>
      </div>

      <div className="flex flex-wrap gap-2 mb-6">
        <a href={exportReportUrl(review.reviewId, 'pdf')} className="text-sm bg-gray-200 dark:bg-gray-700 dark:text-gray-100 px-3 py-1.5 rounded-md">
          ⬇ Export PDF
        </a>
        <a href={exportReportUrl(review.reviewId, 'html')} target="_blank" rel="noreferrer" className="text-sm bg-gray-200 dark:bg-gray-700 dark:text-gray-100 px-3 py-1.5 rounded-md">
          ⬇ Export HTML
        </a>
        <a href={exportReportUrl(review.reviewId, 'markdown')} className="text-sm bg-gray-200 dark:bg-gray-700 dark:text-gray-100 px-3 py-1.5 rounded-md">
          ⬇ Export Markdown
        </a>
        {DOC_TABS.map((tab) => (
          <button
            key={tab.key}
            onClick={() => loadDocTab(tab.key)}
            className={`text-sm px-3 py-1.5 rounded-md ${
              activeDocTab === tab.key
                ? 'bg-brand-600 text-white'
                : 'bg-gray-200 dark:bg-gray-700 dark:text-gray-100'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeDocTab && (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-5 mb-6">
          <h2 className="font-semibold text-gray-800 dark:text-gray-100 mb-2">
            {DOC_TABS.find((t) => t.key === activeDocTab)?.label}
          </h2>
          {docsLoading && !docsCache[activeDocTab] ? (
            <p className="text-gray-400 text-sm">Generating...</p>
          ) : (
            <pre className="whitespace-pre-wrap text-xs text-gray-600 dark:text-gray-300 max-h-96 overflow-auto">
              {docsCache[activeDocTab]}
            </pre>
          )}
        </div>
      )}

      <RefactorPanel projectId={review.projectId} />

      <div className="flex items-center gap-2 mb-4 flex-wrap">
        {FILTERS.map((f) => (
          <button
            key={f}
            onClick={() => setFilter(f)}
            className={`text-xs px-3 py-1 rounded-full border ${
              filter === f
                ? 'bg-brand-600 text-white border-brand-600'
                : 'border-gray-300 dark:border-gray-600 text-gray-600 dark:text-gray-300'
            }`}
          >
            {f} {f !== 'ALL' ? `(${review.findings.filter((x) => x.severity === f).length})` : `(${review.findings.length})`}
          </button>
        ))}
      </div>

      <div className="space-y-3">
        {findings.length === 0 ? (
          <p className="text-gray-400 text-sm">No findings in this category. 🎉</p>
        ) : (
          findings.map((f, idx) => <FindingItem key={idx} finding={f} />)
        )}
      </div>
    </div>
  )
}
