import React from 'react'

const LEVEL_CLASSES = {
  LOW: 'risk-low',
  MODERATE: 'risk-moderate',
  ELEVATED: 'risk-elevated',
  HIGH: 'risk-high',
}

export default function RiskRecordItem({ record, isExpanded, onToggle }) {
  const levelClass = LEVEL_CLASSES[record.level] || 'risk-unknown'
  const hasScore = record.score !== null && record.score !== undefined

  function handleKeyDown(event) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      onToggle()
    }
  }

  return (
    <div
      className="account-card risk-card-clickable"
      role="button"
      tabIndex={0}
      aria-expanded={isExpanded}
      onClick={onToggle}
      onKeyDown={handleKeyDown}
    >
      <div className="account-card-header">
        <span className="account-card-balance risk-card-score">
          {hasScore ? Math.round(record.score) : '--'}
          <span className="risk-card-score-unit">/ 100</span>
        </span>
        {record.overAllExplain ? (
          <span className="account-card-id">{record.overAllExplain}</span>
        ) : null}
        {record.calculateStatus === 'INSUFFICIENT_DATA' ? (
          <span className="account-card-id">
            {record.message || 'Not enough data to calculate a score.'}
          </span>
        ) : null}
      </div>
      <div className="risk-card-meta">
        <span className={`status-badge ${levelClass}`}>
          {record.level || 'Insufficient data'}
        </span>
        {record.calculatedAt ? (
          <span className="account-card-id">
            {new Date(record.calculatedAt).toLocaleString()}
          </span>
        ) : null}
      </div>
    </div>
  )
}
