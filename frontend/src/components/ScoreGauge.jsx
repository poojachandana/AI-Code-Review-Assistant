import React from 'react'

export default function ScoreGauge({ score, size = 120 }) {
    const radius = (size - 16) / 2
    const circumference = 2 * Math.PI * radius
    const offset = circumference - (score / 100) * circumference

    const color =
        score >= 80 ? '#16a34a' : score >= 50 ? '#ca8a04' : '#dc2626'

    return (
        <div className="relative inline-flex items-center justify-center" style={{ width: size, height: size }}>
            <svg width={size} height={size} className="-rotate-90">
                <circle
                    cx={size / 2} cy={size / 2} r={radius}
                    fill="none" stroke="currentColor" strokeWidth="10"
                    className="text-gray-200 dark:text-gray-700"
                />
                <circle
                    cx={size / 2} cy={size / 2} r={radius}
                    fill="none" stroke={color} strokeWidth="10"
                    strokeDasharray={circumference}
                    strokeDashoffset={offset}
                    strokeLinecap="round"
                    style={{ transition: 'stroke-dashoffset 1s ease-out' }}
                />
            </svg>
            <div className="absolute flex flex-col items-center">
                <span className="text-2xl font-extrabold" style={{ color }}>{score}</span>
                <span className="text-xs text-gray-400">/ 100</span>
            </div>
        </div>
    )
}