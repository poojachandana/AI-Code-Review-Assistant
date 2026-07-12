import React, { useEffect, useState } from 'react'
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, BarChart, Bar, Cell } from 'recharts'
import { getAnalyticsOverview } from '../services/api.js'

const SEVERITY_COLORS = {
    CRITICAL: '#dc2626', HIGH: '#ea580c', MEDIUM: '#ca8a04', LOW: '#16a34a', INFO: '#6b7280',
}

const TOOLTIP_STYLE = {
    borderRadius: 10,
    border: 'none',
    boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
    fontSize: 13,
}

const CARD_CLASS = "bg-white dark:bg-gray-800 rounded-xl shadow-sm hover:shadow-md transition-all duration-200 p-5"

export default function Analytics() {
    const [data, setData] = useState(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        getAnalyticsOverview().then(({ data }) => setData(data)).finally(() => setLoading(false))
    }, [])

    if (loading) {
        return (
            <div className="max-w-4xl mx-auto mt-8 px-4">
                <div className="h-8 w-56 bg-gray-200 dark:bg-gray-700 rounded-md animate-pulse mb-6" />
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                    {[1, 2, 3, 4].map((i) => (
                        <div key={i} className="h-20 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse" />
                    ))}
                </div>
                <div className="h-64 bg-gray-200 dark:bg-gray-700 rounded-xl animate-pulse" />
            </div>
        )
    }
    if (!data) return <p className="text-center mt-16 text-red-500">Could not load analytics.</p>

    const severityData = Object.entries(data.severityDistribution || {}).map(([name, value]) => ({ name, value }))

    return (
        <div className="max-w-4xl mx-auto mt-8 px-4 pb-16">
            <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-gray-100">Repository Analytics</h1>

            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
                <StatCard label="Projects" value={data.totalProjects} />
                <StatCard label="Reviews" value={data.totalReviews} />
                <StatCard label="Avg Quality Score" value={`${data.averageQualityScore}/100`} />
                <StatCard label="Total Findings" value={severityData.reduce((s, x) => s + x.value, 0)} />
            </div>

            <div className={`${CARD_CLASS} mb-6`}>
                <h2 className="font-semibold text-gray-800 dark:text-gray-100 mb-2">Quality Score Trend</h2>
                {data.scoreTrend.length === 0 ? (
                    <p className="text-sm text-gray-400">No reviews yet — submit some code to see trends.</p>
                ) : (
                    <ResponsiveContainer width="100%" height={260}>
                        <LineChart data={data.scoreTrend}>
                            <defs>
                                <linearGradient id="scoreLineGradient" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="0%" stopColor="#2563eb" stopOpacity={0.25} />
                                    <stop offset="100%" stopColor="#2563eb" stopOpacity={0} />
                                </linearGradient>
                            </defs>
                            <XAxis dataKey="date" tick={{ fontSize: 10 }} axisLine={false} tickLine={false} />
                            <YAxis domain={[0, 100]} axisLine={false} tickLine={false} />
                            <Tooltip contentStyle={TOOLTIP_STYLE} />
                            <Line
                                type="monotone" dataKey="score" stroke="#2563eb" strokeWidth={2.5}
                                dot={{ r: 4, fill: '#2563eb', strokeWidth: 0 }}
                                activeDot={{ r: 6 }}
                                animationDuration={800}
                            />
                        </LineChart>
                    </ResponsiveContainer>
                )}
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className={CARD_CLASS}>
                    <h2 className="font-semibold text-gray-800 dark:text-gray-100 mb-2">Findings by Severity</h2>
                    {severityData.length === 0 ? (
                        <p className="text-sm text-gray-400">No findings yet.</p>
                    ) : (
                        <ResponsiveContainer width="100%" height={220}>
                            <BarChart data={severityData}>
                                <XAxis dataKey="name" tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
                                <YAxis axisLine={false} tickLine={false} />
                                <Tooltip contentStyle={TOOLTIP_STYLE} cursor={{ fill: 'rgba(0,0,0,0.03)' }} />
                                <Bar dataKey="value" radius={[6, 6, 0, 0]} animationDuration={800}>
                                    {severityData.map((entry) => (
                                        <Cell key={entry.name} fill={SEVERITY_COLORS[entry.name] || '#999'} />
                                    ))}
                                </Bar>
                            </BarChart>
                        </ResponsiveContainer>
                    )}
                </div>

                <div className={CARD_CLASS}>
                    <h2 className="font-semibold text-gray-800 dark:text-gray-100 mb-2">Top Issue Categories</h2>
                    {data.topCategories.length === 0 ? (
                        <p className="text-sm text-gray-400">No findings yet.</p>
                    ) : (
                        <ul className="space-y-2">
                            {data.topCategories.map((c) => (
                                <li key={c.category} className="flex justify-between items-center text-sm text-gray-700 dark:text-gray-200 py-1.5 border-b border-gray-100 dark:border-gray-700 last:border-0">
                                    <span>{c.category}</span>
                                    <span className="font-semibold bg-brand-50 dark:bg-brand-900/30 text-brand-600 dark:text-brand-400 px-2 py-0.5 rounded-full text-xs">{c.count}</span>
                                </li>
                            ))}
                        </ul>
                    )}
                </div>
            </div>
        </div>
    )
}

function StatCard({ label, value }) {
    return (
        <div className="bg-white dark:bg-gray-800 rounded-xl shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all duration-200 p-4 text-center">
            <p className="text-2xl font-bold text-brand-600 dark:text-brand-500">{value}</p>
            <p className="text-xs text-gray-400 mt-1">{label}</p>
        </div>
    )
}