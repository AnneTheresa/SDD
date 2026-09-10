const CATEGORY_COLORS = {
  'Housing': '#00e5ff',
  'Transport': '#ff4d6d',
  'Food & Drink': '#4dfccf',
  'Entertainment': '#a855f7',
  'Shopping': '#52a8ff',
  'Utilities': '#ff7ab6',
  'Health': '#b78cff',
  'Income': '#39ff88',
  'Other': '#ffd166',
  'No category': '#475569'
};

function toAmount(value) {
  const amount = Number.parseFloat(value ?? 0);
  return Number.isFinite(amount) ? amount : 0;
}

function monthLabel(entry) {
  return `${entry.year}-${String(entry.month).padStart(2, '0')}`;
}

function toNiceStep(value) {
  if (!Number.isFinite(value) || value <= 0) {
    return 1;
  }

  const magnitude = 10 ** Math.floor(Math.log10(value));
  const normalized = value / magnitude;
  const candidates = [1, 2, 2.5, 5, 7.5, 10];
  const chosen = candidates.find((candidate) => normalized <= candidate) || 10;
  return chosen * magnitude;
}

function formatAxisTick(value) {
  const rounded = Math.abs(value) < 1e-9 ? 0 : value;
  if (Number.isInteger(rounded)) {
    return String(rounded);
  }

  if (Math.abs(rounded) >= 10) {
    return rounded.toFixed(1).replace(/\.0$/, '');
  }

  return rounded.toFixed(2).replace(/0+$/, '').replace(/\.$/, '');
}

export function SpendingBarChart({ entries, showTitle = true, className = '' }) {
  if (!Array.isArray(entries) || entries.length === 0) {
    return (
      <div className={`chart-card empty-state stack tight-gap ${className}`.trim()}>
        {showTitle ? <h3>Six-Month Trend</h3> : null}
        <p className="muted">No trend data returned for this period yet.</p>
      </div>
    );
  }

  const amounts = entries.map((entry) => toAmount(entry.totalSpend ?? entry.totalDebitSpend));
  const maxAmount = Math.max(...amounts, 0);
  const axisTickCount = 5;
  const axisIntervals = axisTickCount - 1;
  const paddedMax = maxAmount > 0 ? maxAmount * 1.2 : 1;
  const axisStep = toNiceStep(paddedMax / axisIntervals);
  const axisMax = axisStep * axisIntervals;
  const gridStepPercent = `${100 / axisIntervals}%`;
  const axisTicks = Array.from({ length: axisTickCount }, (_, index) => {
    const value = axisMax - (axisStep * index);
    return formatAxisTick(value);
  });

  return (
    <div className={`chart-card stack ${className}`.trim()}>
      {showTitle ? <h3>Six-Month Trend</h3> : null}
      <div className="bar-chart-shell">
        <div className="bar-y-axis" aria-hidden="true">
          {axisTicks.map((tick, index) => (
            <span key={`${tick}-${index}`}>{tick}</span>
          ))}
        </div>
        <div className="bar-chart" style={{ '--grid-step': gridStepPercent }}>
          {entries.map((entry) => {
            const amount = toAmount(entry.totalSpend ?? entry.totalDebitSpend);
            const isZero = amount <= 0;
            const height = isZero
              ? '2px'
              : axisMax > 0
                ? `${(amount / axisMax) * 100}%`
                : '2px';

            return (
              <div className="bar-row" key={`${entry.year}-${entry.month}`}>
                <div className="bar-track">
                  <div
                    className={`bar-fill${isZero ? ' placeholder' : ''}`}
                    style={{ height }}
                    title={`Amount: ${amount.toFixed(2)}`}
                  />
                </div>
                <div className="bar-meta">
                  <strong>{monthLabel(entry)}</strong>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export function SpendingPieChart({ categories, showTitle = true, className = '' }) {
  if (!Array.isArray(categories) || categories.length === 0) {
    return (
      <div className={`chart-card empty-state stack tight-gap ${className}`.trim()}>
        {showTitle ? <h3>Category Breakdown</h3> : null}
        <p className="muted">No category data returned for this month yet.</p>
      </div>
    );
  }

  const totalAmount = categories.reduce((sum, category) => sum + toAmount(category.totalAmount), 0);
  const isEmpty = totalAmount <= 0;
  const visibleCategories = categories.filter((category) => toAmount(category.totalAmount) > 0);

  let runningTotal = 0;
  const segments = [];

  visibleCategories.forEach((category, index) => {
    const percentage = totalAmount > 0 ? (toAmount(category.totalAmount) / totalAmount) * 100 : 0;
    const start = runningTotal;
    runningTotal = Math.min(100, runningTotal + percentage);
    const end = index === visibleCategories.length - 1 ? 100 : runningTotal;
    const color = CATEGORY_COLORS[category.category] || '#94a3b8';

    if (end > start) {
      segments.push(`${color} ${start}% ${end}%`);
    }
  });

  const chartStyle = isEmpty
    ? { background: 'conic-gradient(#334155 0 100%)' }
    : { background: `conic-gradient(${segments.join(', ')})` };

  return (
    <div className={`chart-card stack ${className}`.trim()}>
      {showTitle ? <h3>Category Breakdown</h3> : null}
      <div className="pie-layout">
        <div className={`pie-chart${isEmpty ? ' empty' : ''}`} style={chartStyle} aria-hidden="true">
          {isEmpty ? <span className="pie-empty-label">No spend</span> : null}
        </div>
        {visibleCategories.length > 0 ? (
          <div className="pie-legend">
            {visibleCategories.map((category, index) => (
              <div className="legend-row" key={`${category.category}-${index}`}>
                <span className="legend-swatch" style={{ backgroundColor: CATEGORY_COLORS[category.category] || '#94a3b8' }} />
                <div>
                  <strong>{category.category}</strong>
                  <p className="muted compact-text">{category.totalAmount} ({category.percentage}%)</p>
                </div>
              </div>
            ))}
          </div>
        ) : null}
      </div>
    </div>
  );
}