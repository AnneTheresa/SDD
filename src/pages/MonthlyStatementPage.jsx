import { useMemo, useState } from "react";
import { Link, useParams, useSearchParams } from "react-router-dom";
import { mapAxiosError } from "../api/axiosClient";
import { AccountSwitcher } from "../components/AccountSwitcher";
import { useAuth } from "../auth/AuthContext";
import { useListCustomerAccounts } from "../hooks/useListCustomerAccounts";
import { useMonthlyStatement } from "../hooks/useGroup3";
import { emptyMonthlyStatementLookup } from "../types";

const MONTH_OPTIONS = [
  { value: "01", label: "January" },
  { value: "02", label: "February" },
  { value: "03", label: "March" },
  { value: "04", label: "April" },
  { value: "05", label: "May" },
  { value: "06", label: "June" },
  { value: "07", label: "July" },
  { value: "08", label: "August" },
  { value: "09", label: "September" },
  { value: "10", label: "October" },
  { value: "11", label: "November" },
  { value: "12", label: "December" },
];

function splitPeriod(period) {
  const [year = "", month = ""] = String(period || "").split("-");
  return { year, month };
}

function joinPeriod(year, month) {
  if (!year || !month) {
    return "";
  }

  return `${year}-${month}`;
}

function parseYearMonth(period) {
  const match = String(period || "").match(/^(\d{4})-(\d{2})$/);
  if (!match) {
    return null;
  }

  return {
    year: Number(match[1]),
    month: Number(match[2]),
  };
}

function isFuturePeriod(period) {
  const parsed = parseYearMonth(period);
  if (!parsed) {
    return false;
  }

  const now = new Date();
  const currentYear = now.getFullYear();
  const currentMonth = now.getMonth() + 1;
  return (
    parsed.year > currentYear ||
    (parsed.year === currentYear && parsed.month > currentMonth)
  );
}

function resolveStatementErrorMessage(queryError, submittedPeriod) {
  if (!queryError) {
    return null;
  }

  if (isFuturePeriod(submittedPeriod)) {
    return "Statement month cannot be in the future. Please choose the current month or an earlier month.";
  }

  if (
    queryError.code === "HTTP_500" ||
    queryError.code === "INTERNAL_SERVER_ERROR"
  ) {
    return "Statement is not available for the selected month. Please choose a month when this account was already open.";
  }

  return queryError.message;
}

export function MonthlyStatementPage() {
  const { accountId } = useParams();
  const { authState } = useAuth();
  const accountsQuery = useListCustomerAccounts(authState.customerId);
  const [searchParams] = useSearchParams();
  const requestedPeriod = searchParams.get("period");

  // Compute default period with correct January rollover logic
  function getDefaultStatementPeriod() {
    const now = new Date();
    const currentYear = now.getFullYear();
    const currentMonth = now.getMonth() + 1;
    let statementMonth, statementYear;
    if (currentMonth === 1) {
      statementMonth = 12;
      statementYear = currentYear - 1;
    } else {
      statementMonth = currentMonth - 1;
      statementYear = currentYear;
    }
    // Pad month to two digits
    const monthStr = String(statementMonth).padStart(2, "0");
    return { year: String(statementYear), month: monthStr };
  }

  let initial;
  if (requestedPeriod) {
    initial = splitPeriod(requestedPeriod);
  } else {
    initial = getDefaultStatementPeriod();
  }

  const [form, setForm] = useState({
    year: initial.year,
    month: initial.month,
  });
  const selectedPeriod = useMemo(
    () => joinPeriod(form.year, form.month),
    [form.month, form.year],
  );
  const query = useMonthlyStatement({ accountId, period: selectedPeriod });

  const statementPdf = query.data;
  const queryError = query.error ? mapAxiosError(query.error) : null;
  const errorMessage = resolveStatementErrorMessage(queryError, selectedPeriod);

  function handleDownloadStatement() {
    if (!statementPdf || !selectedPeriod) {
      return;
    }

    const downloadUrl = window.URL.createObjectURL(statementPdf);
    const anchor = document.createElement("a");
    anchor.href = downloadUrl;
    anchor.download = `statement-${accountId}-${selectedPeriod}.pdf`;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    window.URL.revokeObjectURL(downloadUrl);
  }

  return (
    <div className="stack">
      <section className="panel stack">
        <div className="section-header">
          <div>
            <h2>Monthly Statement</h2>
            <p className="muted">
              Request a monthly statement PDF for a specific year and month.
            </p>
          </div>
          <AccountSwitcher
            accountId={accountId}
            accounts={accountsQuery.data}
            feature="statements"
          />
        </div>
        <form
          className="form-grid"
          onSubmit={(event) => event.preventDefault()}
        >
          <div className="field">
            <label htmlFor="statement-year">Statement Year</label>
            <input
              id="statement-year"
              type="number"
              min="1900"
              max="9999"
              step="1"
              value={form.year}
              onChange={(event) =>
                setForm((current) => ({ ...current, year: event.target.value }))
              }
              placeholder="e.g. 2026"
              required
            />
          </div>
          <div className="field">
            <label htmlFor="statement-month">Statement Month</label>
            <select
              id="statement-month"
              value={form.month}
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  month: event.target.value,
                }))
              }
              required
            >
              <option value="">Select month</option>
              {MONTH_OPTIONS.map((month) => (
                <option key={month.value} value={month.value}>
                  {month.label}
                </option>
              ))}
            </select>
          </div>
          <div className="actions">
            <Link className="button-link subtle" to={`/accounts/${accountId}`}>
              Back to Account
            </Link>
          </div>
        </form>
        {query.isLoading || query.isFetching ? (
          <div className="banner success">Loading monthly statement...</div>
        ) : null}
        {errorMessage ? (
          <div className="banner error">{errorMessage}</div>
        ) : null}
        {!selectedPeriod ? (
          <div className="banner info">
            Pick a statement month to request data.
          </div>
        ) : null}
      </section>

      <section className="panel stack">
        <div className="section-header">
          <div>
            <h3>Statement File</h3>
            <p className="muted">
              Requested period: {selectedPeriod || "None selected"}
            </p>
          </div>
        </div>
        {statementPdf ? (
          <div className="actions">
            <button type="button" onClick={handleDownloadStatement}>
              Download Statement PDF
            </button>
          </div>
        ) : (
          <div className="banner info">
            Load a statement period to generate a downloadable PDF.
          </div>
        )}
      </section>
    </div>
  );
}
