import React from 'react'
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts'

const SEVERITY_COLORS = {
  CRITICAL: '#dc2626',
  HIGH: '#ea580c',
  MEDIUM: '#ca8a04',
  LOW: '#16a34a',
  INFO: '#6b7280',
}

const TOOLTIP_STYLE = {
  borderRadius: 10,
  border: 'none',
  boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
  fontSize: 13,
}

export function MetricsBarChart({ review }) {
  const data = [
    { name: 'Classes', value: review.numClasses },
    { name: 'Methods', value: review.numMethods },
    { name: 'Avg Method Len', value: review.avgMethodLength },
    { name: 'Cyclomatic Cx', value: review.cyclomaticComplexity },
    { name: 'Maintainability', value: review.maintainabilityIndex },
  ]

  return (
      <ResponsiveContainer width="100%" height={260}>
        <BarChart data={data}>
          <defs>
            <linearGradient id="metricsBarGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#3b82f6" stopOpacity={1} />
              <stop offset="100%" stopColor="#3b82f6" stopOpacity={0.35} />
            </linearGradient>
          </defs>
          <XAxis dataKey="name" tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
          <YAxis axisLine={false} tickLine={false} />
          <Tooltip contentStyle={TOOLTIP_STYLE} cursor={{ fill: 'rgba(59,130,246,0.06)' }} />
          <Bar dataKey="value" fill="url(#metricsBarGradient)" radius={[6, 6, 0, 0]} animationDuration={800} />
        </BarChart>
      </ResponsiveContainer>
  )
}

export function SeverityDistributionChart({ findings }) {
  const counts = {}
  findings.forEach((f) => {
    counts[f.severity] = (counts[f.severity] || 0) + 1
  })
  const data = Object.entries(counts).map(([name, value]) => ({ name, value }))

  if (data.length === 0) return <p className="text-sm text-gray-400">No findings to chart.</p>

  return (
      <ResponsiveContainer width="100%" height={260}>
        <PieChart>
          <Pie
              data={data} dataKey="value" nameKey="name"
              innerRadius={50} outerRadius={90}
              paddingAngle={3}
              label
              animationDuration={800}
          >
            {data.map((entry) => (
                <Cell key={entry.name} fill={SEVERITY_COLORS[entry.name] || '#999'} stroke="none" />
            ))}
          </Pie>
          <Legend iconType="circle" />
          <Tooltip contentStyle={TOOLTIP_STYLE} />
        </PieChart>
      </ResponsiveContainer>
  )
}