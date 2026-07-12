import React from 'react'

const SEVERITY_STYLES = {
  CRITICAL: 'bg-red-100 text-red-800 border-red-300 dark:bg-red-900/40 dark:text-red-300 dark:border-red-700',
  HIGH: 'bg-orange-100 text-orange-800 border-orange-300 dark:bg-orange-900/40 dark:text-orange-300 dark:border-orange-700',
  MEDIUM: 'bg-yellow-100 text-yellow-800 border-yellow-300 dark:bg-yellow-900/40 dark:text-yellow-300 dark:border-yellow-700',
  LOW: 'bg-green-100 text-green-800 border-green-300 dark:bg-green-900/40 dark:text-green-300 dark:border-green-700',
  INFO: 'bg-gray-100 text-gray-800 border-gray-300 dark:bg-gray-700 dark:text-gray-300 dark:border-gray-600',
}

export function SeverityBadge({ severity }) {
  return (
    <span className={`text-xs font-semibold px-2 py-0.5 rounded-full border ${SEVERITY_STYLES[severity] || SEVERITY_STYLES.INFO}`}>
      {severity}
    </span>
  )
}

export default function FindingItem({ finding }) {
  return (
    <div className="border border-gray-200 dark:border-gray-700 rounded-lg p-4 bg-white dark:bg-gray-800">
      <div className="flex items-center gap-2 flex-wrap mb-1">
        <SeverityBadge severity={finding.severity} />
        <span className="text-xs uppercase tracking-wide text-gray-400">{finding.category}</span>
        <span className="text-xs text-gray-400">via {finding.source}</span>
        {finding.fileName && (
          <span className="text-xs text-gray-400">
            · {finding.fileName}{finding.lineNumber ? `:${finding.lineNumber}` : ''}
          </span>
        )}
      </div>
      <p className="font-medium text-gray-900 dark:text-gray-100">{finding.issue}</p>
      {finding.explanation && (
        <p className="text-sm text-gray-600 dark:text-gray-400 mt-1"><b>Why:</b> {finding.explanation}</p>
      )}
      {finding.suggestion && (
        <p className="text-sm text-gray-600 dark:text-gray-400 mt-1"><b>Fix:</b> {finding.suggestion}</p>
      )}
    </div>
  )
}
