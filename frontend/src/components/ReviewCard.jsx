import React from 'react'
import { Link } from 'react-router-dom'
import { Trash2 } from 'lucide-react'
import ScoreGauge from './ScoreGauge.jsx'

export default function ReviewCard({ project, onDelete }) {
    return (
        <div className="border border-gray-200 dark:border-gray-700 rounded-xl p-4 bg-white dark:bg-gray-800 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 flex items-center justify-between">
            <div className="flex items-center gap-4">
                {project.latestScore != null ? (
                    <ScoreGauge score={project.latestScore} size={56} />
                ) : (
                    <div className="w-14 h-14 rounded-full bg-gray-100 dark:bg-gray-700 flex items-center justify-center text-gray-400 text-xs">—</div>
                )}
                <div>
                    <p className="font-semibold text-gray-900 dark:text-gray-100">{project.projectName}</p>
                    <p className="text-xs text-gray-400">
                        {project.uploadType} · {new Date(project.createdAt).toLocaleString()}
                    </p>
                </div>
            </div>
            <div className="flex items-center gap-3">
                {project.latestReviewId && (
                    <Link
                        to={`/reviews/${project.latestReviewId}`}
                        className="text-sm bg-brand-600 hover:bg-brand-700 text-white px-3 py-1.5 rounded-md transition-colors"
                    >
                        View
                    </Link>
                )}
                <button
                    onClick={() => onDelete(project.projectId)}
                    className="text-gray-400 hover:text-red-500 transition-colors p-1.5 rounded-md hover:bg-red-50 dark:hover:bg-red-900/20"
                    title="Delete project"
                >
                    <Trash2 size={16} />
                </button>
            </div>
        </div>
    )
}