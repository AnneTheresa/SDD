import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { mapAxiosError } from '../api/axiosClient';
import { exportTransactionHistoryPdf } from '../api/group3';
import { AccountSwitcher } from '../components/AccountSwitcher';
import { TransactionsTable } from '../components/TransactionsTable';
import { useListCustomerAccounts } from '../hooks/useListCustomerAccounts';
import { useRecategoriseTransaction, useTransactionHistory } from '../hooks/useGroup3';
import { useAuth } from '../auth/AuthContext';
import { TRANSACTION_CATEGORIES, emptyTransactionHistoryFilters } from '../types';

function todayDateInputValue() {
  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function validateDateRange(startDate, endDate) {
  if (!startDate || !endDate) {
    return 'Select both a start date and an end date.';
  }

  const today = todayDateInputValue();
  if (startDate > today || endDate > today) {
    return 'Date range cannot include future dates.';
  }

  const start = new Date(`${startDate}T00:00:00Z`);
  const end = new Date(`${endDate}T23:59:59Z`);

  if (start > end) {
    return 'Start date must be on or before the end date.';
  }

  const millisecondsInDay = 1000 * 60 * 60 * 24;
  const spanInDays = Math.floor((end - start) / millisecondsInDay);

  if (spanInDays > 366) {
    return 'Transaction history can only cover up to 366 days.';
  }

  return null;
}

export function TransactionHistoryPage() {
  const { accountId } = useParams();
  const { authState } = useAuth();
  const accountsQuery = useListCustomerAccounts(authState.customerId);
  const [filters, setFilters] = useState(emptyTransactionHistoryFilters);
  const [submittedFilters, setSubmittedFilters] = useState(null);
  const [localError, setLocalError] = useState(null);
  const [exportError, setExportError] = useState(null);
  const [categoryError, setCategoryError] = useState(null);
  const [categoryMessage, setCategoryMessage] = useState(null);
  const [savingTransactionId, setSavingTransactionId] = useState(null);
  const [isExporting, setIsExporting] = useState(false);
  const recategoriseMutation = useRecategoriseTransaction();
  const query = useTransactionHistory({
    accountId,
    startDate: submittedFilters?.startDate,
    endDate: submittedFilters?.endDate
  });

  function handleSubmit(event) {
    event.preventDefault();
    const validationMessage = validateDateRange(filters.startDate, filters.endDate);
    if (validationMessage) {
      setLocalError(validationMessage);
      setCategoryError(null);
      setCategoryMessage(null);
      return;
    }

    setLocalError(null);
    setCategoryError(null);
    setCategoryMessage(null);
    setSubmittedFilters(filters);
  }

  async function handleExport() {
    const validationMessage = validateDateRange(filters.startDate, filters.endDate);
    if (validationMessage) {
      setLocalError(validationMessage);
      return;
    }

    setLocalError(null);
    setExportError(null);
    setIsExporting(true);

    try {
      const blob = await exportTransactionHistoryPdf({ accountId, ...filters });
      const downloadUrl = window.URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = downloadUrl;
      anchor.download = `account-${accountId}-transactions-${filters.startDate}-to-${filters.endDate}.pdf`;
      document.body.appendChild(anchor);
      anchor.click();
      anchor.remove();
      window.URL.revokeObjectURL(downloadUrl);
    } catch (error) {
      setExportError(mapAxiosError(error));
    } finally {
      setIsExporting(false);
    }
  }

  async function handleSaveCategory(transactionId, category) {
    if (!transactionId || category == null) {
      return;
    }

    setCategoryError(null);
    setCategoryMessage(null);
    setSavingTransactionId(transactionId);

    try {
      await recategoriseMutation.mutateAsync({ accountId, transactionId, category });
      setCategoryMessage('Transaction category updated successfully.');
      await query.refetch();
    } catch (error) {
      setCategoryError(mapAxiosError(error));
    } finally {
      setSavingTransactionId(null);
    }
  }

  const queryError = query.error ? mapAxiosError(query.error) : null;
  const history = query.data;

  return (
    <div className="stack">
      <section className="panel stack">
        <div className="section-header">
          <div>
            <h2>Transaction History</h2>
            <p className="muted">Filter account activity by date range and export the same slice as PDF.</p>
          </div>
          <AccountSwitcher accountId={accountId} accounts={accountsQuery.data} feature="transactions" />
        </div>
        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="field">
            <label htmlFor="history-start-date">Start Date</label>
            <input
              id="history-start-date"
              type="date"
              max={todayDateInputValue()}
              value={filters.startDate}
              onChange={(event) => setFilters((current) => ({ ...current, startDate: event.target.value }))}
            />
          </div>
          <div className="field">
            <label htmlFor="history-end-date">End Date</label>
            <input
              id="history-end-date"
              type="date"
              max={todayDateInputValue()}
              value={filters.endDate}
              onChange={(event) => setFilters((current) => ({ ...current, endDate: event.target.value }))}
            />
          </div>
          <div className="actions">
            <button type="submit" disabled={query.isFetching}>Apply Range</button>
            <button type="button" className="secondary" onClick={handleExport} disabled={isExporting}>Export PDF</button>
            <Link className="button-link subtle" to={`/accounts/${accountId}`}>Back to Account</Link>
          </div>
        </form>
        {query.isLoading || query.isFetching ? <div className="banner success">Loading transaction history...</div> : null}
        {localError ? <div className="banner error">{localError}</div> : null}
        {queryError ? <div className="banner error">{queryError.message}</div> : null}
        {exportError ? <div className="banner error">{exportError.message}</div> : null}
        {categoryError ? <div className="banner error">{categoryError.message}</div> : null}
        {categoryMessage ? <div className="banner success">{categoryMessage}</div> : null}
      </section>

      <section className="panel stack">
        <div className="section-header">
          <div>
            <h3>Selected Range</h3>
            <p className="muted">
              {submittedFilters ? `${submittedFilters.startDate} to ${submittedFilters.endDate}` : 'No range applied yet'}
            </p>
          </div>
          <div className="detail-item">
            <p className="muted">Transactions Returned</p>
            <strong>{history?.transactionCount ?? 0}</strong>
          </div>
        </div>
        {!submittedFilters ? <div className="banner info">Choose a range and click Apply Range to load transaction history.</div> : null}
        {submittedFilters && !query.isFetching && !queryError && (history?.transactionCount ?? 0) === 0 ? (
          <div className="banner info">No transactions were found for the selected date range. Try widening the range.</div>
        ) : null}
        <TransactionsTable
          transactions={history?.transactions}
          emptyTitle="No transactions in this range"
          emptyMessage="The selected range returned no account activity."
          categoryOptions={TRANSACTION_CATEGORIES}
          onSaveCategory={handleSaveCategory}
          savingTransactionId={savingTransactionId}
        />
      </section>
    </div>
  );
}