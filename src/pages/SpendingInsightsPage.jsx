import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { mapAxiosError } from '../api/axiosClient';
import { AccountSwitcher } from '../components/AccountSwitcher';
import { SpendingBarChart, SpendingPieChart } from '../components/InsightCharts';
import { useAuth } from '../auth/AuthContext';
import { useListCustomerAccounts } from '../hooks/useListCustomerAccounts';
import { useSpendingInsights } from '../hooks/useGroup3';
import { emptySpendingInsightsLookup } from '../types';

const MONTH_OPTIONS = [
  { value: '01', label: 'January' },
  { value: '02', label: 'February' },
  { value: '03', label: 'March' },
  { value: '04', label: 'April' },
  { value: '05', label: 'May' },
  { value: '06', label: 'June' },
  { value: '07', label: 'July' },
  { value: '08', label: 'August' },
  { value: '09', label: 'September' },
  { value: '10', label: 'October' },
  { value: '11', label: 'November' },
  { value: '12', label: 'December' }
];

function splitPeriod(period) {
  const [year = '', month = ''] = String(period || '').split('-');
  return { year, month };
}

function joinPeriod(year, month) {
  if (!year || !month) {
    return '';
  }

  return `${year}-${month}`;
}

const INSIGHT_CATEGORIES = [
  'Housing',
  'Transport',
  'Food & Drink',
  'Entertainment',
  'Shopping',
  'Utilities',
  'Health',
  'Income',
  'Other',
  'No category'
];

function toAmount(value) {
  const amount = Number.parseFloat(value ?? 0);
  return Number.isFinite(amount) ? amount : 0;
}

function toFixedAmount(value) {
  return toAmount(value).toFixed(2);
}

function toPercentageStrings(items) {
  const total = items.reduce((sum, item) => sum + toAmount(item.totalAmount), 0);
  if (total <= 0) {
    return items.map((item) => ({ ...item, percentage: '0.00' }));
  }

  let runningPercentage = 0;
  return items.map((item, index) => {
    if (index === items.length - 1) {
      return { ...item, percentage: Math.max(0, 100 - runningPercentage).toFixed(2) };
    }

    const percentage = Number(((toAmount(item.totalAmount) / total) * 100).toFixed(2));
    runningPercentage += percentage;
    return { ...item, percentage: percentage.toFixed(2) };
  });
}

function applyNoCategoryFallback(items, totalDebitSpend, hasUncategorised) {
  const totalSpend = toAmount(totalDebitSpend);
  if (totalSpend <= 0) {
    return items;
  }

  const namedSpend = items
    .filter((item) => item.category !== 'No category')
    .reduce((sum, item) => sum + toAmount(item.totalAmount), 0);
  const noCategoryAmount = items
    .filter((item) => item.category === 'No category')
    .reduce((sum, item) => sum + toAmount(item.totalAmount), 0);
  const remainder = Number((totalSpend - namedSpend).toFixed(2));

  if ((!hasUncategorised && remainder <= 0) || remainder <= 0 || remainder === noCategoryAmount) {
    return items;
  }

  return items.map((item) => item.category === 'No category'
    ? {
        ...item,
        totalAmount: remainder.toFixed(2)
      }
    : item);
}

function parsePeriod(period) {
  const [yearText, monthText] = String(period || '').split('-');
  const year = Number.parseInt(yearText, 10);
  const month = Number.parseInt(monthText, 10);

  if (!Number.isFinite(year) || !Number.isFinite(month) || month < 1 || month > 12) {
    const now = new Date();
    return { year: now.getFullYear(), month: now.getMonth() + 1 };
  }

  return { year, month };
}

function buildEmptySixMonthTrend(period) {
  const { year, month } = parsePeriod(period);
  const current = new Date();
  const currentYear = current.getFullYear();
  const currentMonth = current.getMonth() + 1;
  const entries = [];

  for (let offset = 5; offset >= 0; offset -= 1) {
    const date = new Date(Date.UTC(year, month - 1, 1));
    date.setUTCMonth(date.getUTCMonth() - offset);

    const entryYear = date.getUTCFullYear();
    const entryMonth = date.getUTCMonth() + 1;
    const isComplete = entryYear < currentYear || (entryYear === currentYear && entryMonth < currentMonth);

    entries.push({
      year: entryYear,
      month: entryMonth,
      totalSpend: '0.00',
      isComplete,
      accountExisted: true
    });
  }

  return entries;
}

function buildEmptyBreakdown() {
  return INSIGHT_CATEGORIES.map((category) => ({
    category,
    totalAmount: '0.00',
    percentage: '0.00',
    transactionCount: 0
  }));
}

function normalizeCategoryLabel(value) {
  const text = String(value ?? '').trim();
  if (!text || text === 'Uncategorised') {
    return 'No category';
  }

  return text;
}

function normalizeCategoryBreakdown(items, totalDebitSpend, hasUncategorised) {
  const source = Array.isArray(items) ? items : [];
  const totals = new Map();

  INSIGHT_CATEGORIES.forEach((category) => {
    totals.set(category, {
      category,
      totalAmount: 0,
      transactionCount: 0
    });
  });

  source.forEach((item) => {
    const category = normalizeCategoryLabel(item.category);
    const existing = totals.get(category) || {
      category,
      totalAmount: 0,
      transactionCount: 0
    };

    existing.totalAmount += toAmount(item.totalAmount);
    existing.transactionCount += Number.parseInt(item.transactionCount ?? 0, 10) || 0;
    totals.set(category, existing);
  });

  const normalized = INSIGHT_CATEGORIES.map((category) => {
    const item = totals.get(category);
    return {
      category,
      totalAmount: toFixedAmount(item?.totalAmount ?? 0),
      percentage: '0.00',
      transactionCount: item?.transactionCount ?? 0
    };
  });

  return toPercentageStrings(applyNoCategoryFallback(normalized, totalDebitSpend, hasUncategorised));
}

function normalizeInsights(data, period) {
  const empty = {
    totalDebitSpend: '0.00',
    transactionCount: 0,
    dataFresh: true,
    categoryBreakdown: buildEmptyBreakdown(),
    sixMonthTrend: buildEmptySixMonthTrend(period)
  };

  if (!data) {
    return empty;
  }

  return {
    ...empty,
    ...data,
    categoryBreakdown: Array.isArray(data.categoryBreakdown) && data.categoryBreakdown.length > 0
      ? normalizeCategoryBreakdown(data.categoryBreakdown, data.totalDebitSpend, data.hasUncategorised)
      : empty.categoryBreakdown,
    sixMonthTrend: Array.isArray(data.sixMonthTrend) && data.sixMonthTrend.length > 0
      ? data.sixMonthTrend
      : empty.sixMonthTrend
  };
}

export function SpendingInsightsPage() {
  const { accountId } = useParams();
  const { authState } = useAuth();
  const accountsQuery = useListCustomerAccounts(authState.customerId);
  const initial = splitPeriod(emptySpendingInsightsLookup.period);
  const [form, setForm] = useState({ year: initial.year, month: initial.month });
  const [submittedPeriod, setSubmittedPeriod] = useState('');
  const [expandedChart, setExpandedChart] = useState({ trend: false, breakdown: false });
  const query = useSpendingInsights({ accountId, period: submittedPeriod });

  function handleSubmit(event) {
    event.preventDefault();
    const period = joinPeriod(form.year, form.month);
    if (!period) {
      return;
    }

    setSubmittedPeriod(period);
  }

  const insights = query.data;
  const chartData = normalizeInsights(insights, submittedPeriod);
  const queryError = query.error ? mapAxiosError(query.error) : null;
  const noTransactions = Boolean(submittedPeriod)
    && !query.isFetching
    && !queryError
    && (chartData.transactionCount ?? 0) === 0;

  function toggleChart(panel) {
    setExpandedChart((current) => ({
      ...current,
      [panel]: !current[panel]
    }));
  }

  return (
    <div className="stack">
      <section className="panel stack">
        <div className="section-header">
          <div>
            <h2>Spending Insights</h2>
            <p className="muted">Review debit-spend totals for a selected month as a pie chart plus a six-month bar chart trend.</p>
          </div>
          <AccountSwitcher accountId={accountId} accounts={accountsQuery.data} feature="insights" />
        </div>
        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="insights-year">Insight Year</label>
            <input
              id="insights-year"
              type="number"
              min="1900"
              max="9999"
              step="1"
              value={form.year}
              onChange={(event) => setForm((current) => ({ ...current, year: event.target.value }))}
              placeholder="e.g. 2026"
              required
            />
          </div>
          <div className="field">
            <label htmlFor="insights-month">Insight Month</label>
            <select
              id="insights-month"
              value={form.month}
              onChange={(event) => setForm((current) => ({ ...current, month: event.target.value }))}
              required
            >
              <option value="">Select month</option>
              {MONTH_OPTIONS.map((month) => (
                <option key={month.value} value={month.value}>{month.label}</option>
              ))}
            </select>
          </div>
          <div className="actions">
            <button type="submit" disabled={query.isFetching}>Load Insights</button>
            <Link className="button-link subtle" to={`/accounts/${accountId}`}>Back to Account</Link>
          </div>
        </form>
        {query.isLoading || query.isFetching ? <div className="banner success">Loading spending insights...</div> : null}
        {queryError ? <div className="banner error">{queryError.message}</div> : null}
        {!submittedPeriod ? <div className="banner info">Pick a month and click Load Insights to request backend data.</div> : null}
        {noTransactions ? <div className="banner info">No eligible spending transactions were found for this month. Showing a zero-value category breakdown and trend.</div> : null}
      </section>

      <section className="panel stack">
        <div className="card-grid">
          <article className="metric">
            <p className="muted">Selected Period</p>
            <strong>{submittedPeriod || 'None selected'}</strong>
          </article>
          <article className="metric">
            <p className="muted">Total Expenses</p>
            <strong>{chartData.totalDebitSpend ?? '0.00'}</strong>
          </article>
          <article className="metric">
            <p className="muted">Transactions Counted</p>
            <strong>{chartData.transactionCount ?? 0}</strong>
          </article>
        </div>
        <div className="insights-accordion" role="region" aria-label="Spending insights charts">
          <article className="insights-accordion-item">
            <button
              type="button"
              className="insights-accordion-trigger"
              onClick={() => toggleChart('trend')}
              aria-expanded={expandedChart.trend}
              aria-controls="insights-panel-trend"
            >
              <span>Six-Month Trend</span>
              <span
                className={`chevron-icon${expandedChart.trend ? ' open' : ''}`}
                aria-hidden="true"
              >
                ▽
              </span>
            </button>
            <div
              id="insights-panel-trend"
              className={`insights-accordion-content${expandedChart.trend ? ' open' : ''}`}
            >
              <div className="insights-accordion-inner">
                <SpendingBarChart
                  entries={chartData.sixMonthTrend}
                  showTitle={false}
                  className="accordion-chart-card"
                />
              </div>
            </div>
          </article>

          <article className="insights-accordion-item">
            <button
              type="button"
              className="insights-accordion-trigger"
              onClick={() => toggleChart('breakdown')}
              aria-expanded={expandedChart.breakdown}
              aria-controls="insights-panel-breakdown"
            >
              <span>Category Breakdown</span>
              <span
                className={`chevron-icon${expandedChart.breakdown ? ' open' : ''}`}
                aria-hidden="true"
              >
                ▽
              </span>
            </button>
            <div
              id="insights-panel-breakdown"
              className={`insights-accordion-content${expandedChart.breakdown ? ' open' : ''}`}
            >
              <div className="insights-accordion-inner">
                <SpendingPieChart
                  categories={chartData.categoryBreakdown}
                  showTitle={false}
                  className="accordion-chart-card"
                />
              </div>
            </div>
          </article>
        </div>
      </section>
    </div>
  );
}