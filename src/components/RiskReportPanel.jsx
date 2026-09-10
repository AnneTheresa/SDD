import {
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
} from 'recharts'

const FACTOR_LABELS = {
  SPENDING_INCOME_RATIO: 'Spending / Income Ratio',
  SAVING_BALANCE: 'Savings Balance',
  GOAL_PROGRESS: 'Goal Progress',
  FROZEN_ACCOUNT_OVERRIDE: 'Frozen Account Override',
}

const FACTOR_COLORS = {
  SPENDING_INCOME_RATIO: '#00e5ff',
  SAVING_BALANCE: '#4dfccf',
  GOAL_PROGRESS: '#a855f7',
  FROZEN_ACCOUNT_OVERRIDE: '#ff4d6d',
}

const LEVEL_CLASSES = {
  LOW: 'risk-low',
  MODERATE: 'risk-moderate',
  ELEVATED: 'risk-elevated',
  HIGH: 'risk-high',
}

function factorLabel(dataElement) {
  if (!dataElement) {
    return 'Unknown factor'
  }
  return FACTOR_LABELS[dataElement] || dataElement.replace(/_/g, ' ')
}

function isNumber(value) {
  return typeof value === 'number' && Number.isFinite(value)
}

function formatPoints(value) {
  return isNumber(value) ? value.toFixed(1) : '--'
}

function formatWeight(value) {
  return isNumber(value) ? `${Math.round(value * 100)}%` : '--'
}

function RiskChartTooltip({ active, payload, total }) {
  if (!active || !payload || payload.length === 0) {
    return null
  }

  const slice = payload[0]
  const share = total > 0 ? (slice.value / total) * 100 : 0

  return (
    <div className="risk-chart-tooltip">
      <strong>{slice.name}</strong>
      <p>
        {formatPoints(slice.value)} pts · {share.toFixed(1)}% of score
      </p>
    </div>
  )
}

export default function RiskReportPanel({ record }) {
  const factors = record.factors || []
  const hasScore = isNumber(record.score)
  const levelClass = LEVEL_CLASSES[record.level] || 'risk-unknown'
  const isInsufficient = record.calculateStatus === 'INSUFFICIENT_DATA'

  // Contributions sum to the score, so each slice is literally that factor's
  // share of the total. Zero-contribution factors would render as invisible slices.
  const scoringFactors = factors.filter(
    (factor) =>
      factor.valid && isNumber(factor.contribution) && factor.contribution > 0
  )
  const totalContribution = scoringFactors.reduce(
    (sum, factor) => sum + factor.contribution,
    0
  )
  const pieData = scoringFactors.map((factor) => ({
    key: factor.dataElement,
    name: factorLabel(factor.dataElement),
    value: factor.contribution,
  }))

  return (
    <section className="panel risk-report">
      <div className="risk-report-grid">
        {/* Left: Report */}
        <div className="risk-report-info">
          <header className="risk-report-header">
            <div className="risk-report-score">
              <span className="risk-report-score-value">
                {hasScore ? Math.round(record.score) : '--'}
              </span>
              <span className="risk-report-score-unit">/ 100</span>
            </div>
            <div className="risk-report-headline">
              <span className={`status-badge ${levelClass}`}>
                {record.level || 'Insufficient data'}
              </span>
              {record.calculatedAt ? (
                <span className="account-card-id">
                  {new Date(record.calculatedAt).toLocaleString()}
                </span>
              ) : null}
            </div>
          </header>

          {record.overAllExplain ? (
            <p className="risk-report-summary">{record.overAllExplain}</p>
          ) : null}

          {isInsufficient ? (
            <div className="banner info">
              {record.message || 'Not enough data to calculate a score.'}
              {record.code ? (
                <span className="risk-report-code"> ({record.code})</span>
              ) : null}
            </div>
          ) : null}

          <dl className="risk-report-meta">
            <div>
              <dt>Record ID</dt>
              <dd>{record.riskScoreId ?? '--'}</dd>
            </div>
            <div>
              <dt>Customer ID</dt>
              <dd>{record.customerId ?? '--'}</dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>{record.calculateStatus || '--'}</dd>
            </div>
          </dl>

          <h4 className="risk-report-subtitle">Factor Breakdown</h4>
          {factors.length > 0 ? (
            <ul className="risk-factor-list">
              {factors.map((factor) => {
                const color = FACTOR_COLORS[factor.dataElement] || '#94a3b8'
                const counted =
                  factor.valid &&
                  isNumber(factor.contribution) &&
                  factor.contribution > 0

                return (
                  <li className="risk-factor" key={factor.dataElement}>
                    <div className="risk-factor-title">
                      <span
                        className="risk-factor-swatch"
                        style={{
                          backgroundColor: counted ? color : 'transparent',
                          borderColor: color,
                        }}
                      />
                      <span className="risk-factor-name">
                        {factorLabel(factor.dataElement)}
                      </span>
                      <span className="risk-factor-points">
                        {counted
                          ? `${formatPoints(factor.contribution)} pts`
                          : 'Not scored'}
                      </span>
                    </div>
                    <p className="risk-factor-explanation">
                      {factor.explanation || 'No data available for this factor.'}
                    </p>
                    <div className="risk-factor-stats">
                      <span>
                        Subscore{' '}
                        <strong>
                          {isNumber(factor.subscore) ? factor.subscore : '--'}
                        </strong>
                      </span>
                      <span>
                        Weight <strong>{formatWeight(factor.weight)}</strong>
                      </span>
                      <span>
                        Contribution{' '}
                        <strong>{formatPoints(factor.contribution)}</strong>
                      </span>
                    </div>
                  </li>
                )
              })}
            </ul>
          ) : (
            <p className="muted">No factors were recorded for this assessment.</p>
          )}
        </div>

        {/* RRight: Pie Chart */}
        <div className="risk-report-chart">
          <h4 className="risk-report-subtitle">Score Contribution</h4>
          {pieData.length > 0 ? (
            <>
              <div className="risk-pie-shell">
                <ResponsiveContainer width="100%" height={280}>
                  <PieChart>
                    <Pie
                      data={pieData}
                      dataKey="value"
                      nameKey="name"
                      cx="50%"
                      cy="50%"
                      innerRadius={62}
                      outerRadius={104}
                      paddingAngle={2}
                      stroke="none"
                      isAnimationActive={false}
                    >
                      {pieData.map((slice) => (
                        <Cell
                          key={slice.key}
                          fill={FACTOR_COLORS[slice.key] || '#94a3b8'}
                        />
                      ))}
                    </Pie>
                    <Tooltip
                      content={<RiskChartTooltip total={totalContribution} />}
                    />
                    <Legend
                      verticalAlign="bottom"
                      height={36}
                      iconType="circle"
                    />
                  </PieChart>
                </ResponsiveContainer>
                <div className="risk-pie-center" aria-hidden="true">
                  <span className="risk-pie-center-value">
                    {formatPoints(totalContribution)}
                  </span>
                  <span className="risk-pie-center-label">total pts</span>
                </div>
              </div>
              <p className="muted risk-report-note">
                Each slice is that factor&apos;s weighted contribution to the
                final score. Factors that contributed nothing are listed on the
                left but excluded here.
              </p>
            </>
          ) : (
            <p className="muted">
              No weighted factors contributed to this score, so there is nothing
              to chart.
            </p>
          )}
        </div>
      </div>
    </section>
  )
}
