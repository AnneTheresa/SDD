import { Fragment, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate, useParams } from "react-router-dom";
import { getCustomer, listCustomers } from "../api/customers";
import { mapAxiosError } from "../api/axiosClient";
import { useAuth } from "../auth/AuthContext";
import { useListCustomerAccounts } from "../hooks/useListCustomerAccounts";
import { useListRiskAssessmentHistory } from "../hooks/useListRiskAssessmentHistory";
import RiskRecordItem from "../components/RiskRecordItem";
import RiskReportPanel from "../components/RiskReportPanel";
import { calculateRiskScore } from "../api/riskAssessment";

export function AdminRiskScorePage() {
  const navigate = useNavigate();
  const { customerId } = useParams();
  const { isAdmin, rememberCustomerId } = useAuth();
  const [error, setError] = useState(null);
  const [infoMessage, setInfoMessage] = useState(null);
  const [isCalculating, setIsCalculating] = useState(false);
  const [expandedScoreId, setExpandedScoreId] = useState(null);

  const riskQuery = useListRiskAssessmentHistory(customerId);
  const customerQuery = useQuery({
    queryKey: ["customer", customerId],
    queryFn: () => getCustomer(customerId),
    enabled: Boolean(customerId),
  });
  const customerListQuery = useQuery({
    queryKey: ["customers"],
    queryFn: listCustomers,
    enabled: isAdmin,
  });

  const risksError = riskQuery.error
  ? mapAxiosError(riskQuery.error)
  : null;
  const customerError = customerQuery.error
    ? mapAxiosError(customerQuery.error)
    : null;

  function handleCustomerSwitch(event) {
    const nextCustomerId = event.target.value;
    if (!nextCustomerId) {
      return;
    }

    setError(null);
    setInfoMessage(null);
    rememberCustomerId(nextCustomerId);
    navigate(`/admin/${nextCustomerId}/risk-assessment`);
  }

  const customerName = customerQuery.data?.name;

  async function handleRiskCalculation() {
    setError(null);
    setInfoMessage(null);
    setIsCalculating(true);

    try {
      const result = await calculateRiskScore(customerId);

      // INSUFFICIENT_DATA comes back as a 200 with no score, not an error
      if (result.calculateStatus === "INSUFFICIENT_DATA") {
        setInfoMessage(
          result.message || "Not enough transaction history to calculate a risk score."
        );
        return;
      }
      await riskQuery.refetch();
    } catch (caughtError) {
      setError(mapAxiosError(caughtError));
    } finally {
      setIsCalculating(false);
    }
  }

  return (
    <>
      {/* Banner messages at the very top, outside main content */}
      <div className="banner-stack">

        {infoMessage ? <div className="banner info">{infoMessage}</div> : null}
        {error ? <div className="banner error">{error.message}</div> : null}
        {customerError ? (
          <div className="banner error">{customerError.message}</div>
        ) : null}
      </div>
      <div className="stack">
        <section className="panel stack">
          <div className="page-header-row">
            <div>
              <h2>
                {customerName
                  ? `${customerName}'s Risk Assessment`
                  : "Risk Assessment"}
              </h2>
              <p className="muted text-top-muted">
                Review the customer's risk score history and recalculate on
                demand.
              </p>
            </div>
            {customerId && !customerError ? (
              <button
                type="button"
                onClick={handleRiskCalculation}
                disabled={isCalculating}
              >
                {isCalculating ? "Calculating..." : "Calculate Risk Score"}
              </button>
            ) : null}
          </div>
          {isAdmin ? (
            <div className="field">
              <label htmlFor="risk-customer-switcher">
                Admin Customer Switcher
              </label>
              <select
                id="risk-customer-switcher"
                value={customerId || ""}
                onChange={handleCustomerSwitch}
              >
                <option value="">Select customer</option>
                {(customerListQuery.data || []).map((customerOption) => (
                  <option
                    key={customerOption.customerId}
                    value={customerOption.customerId}
                  >
                    {customerOption.customerId} - {customerOption.name}
                  </option>
                ))}
              </select>
              {customerListQuery.error ? (
                <p className="field-hint">
                  {mapAxiosError(customerListQuery.error).message}
                </p>
              ) : null}
            </div>
          ) : null}

        </section>

        {/*--------Risk Score History List---------------- */}
        <section className="panel stack">
          <div className="page-header-row">
            <div>
              <h2>Risk Assessment History</h2>
              <p className="muted text-top-muted">
                Customer's risk assessment history in the past 6 months.
              </p>
            </div>
          </div>
          {riskQuery.isLoading ? (
            <div className="banner info">Loading risk-assessment records...</div>
          ) : null}
          {risksError ? (
            <div className="banner error">{risksError.message}</div>
          ) : null}
          {riskQuery.data && riskQuery.data.length > 0 ? (
            <div className="account-card-list">
              {riskQuery.data.map((r) => (
                <Fragment key={r.riskScoreId}>
                  <RiskRecordItem
                    record={r}
                    isExpanded={expandedScoreId === r.riskScoreId}
                    onToggle={() =>
                      setExpandedScoreId((current) =>
                        current === r.riskScoreId ? null : r.riskScoreId
                      )
                    }
                  />
                  {expandedScoreId === r.riskScoreId ? (
                    <RiskReportPanel record={r} />
                  ) : null}
                </Fragment>
              ))}
            </div>
          ) : !riskQuery.isLoading && !risksError ? (
            <div className="panel">
              <h3>No Risk Assessment Record</h3>
            </div>
          ) : null}
        </section>
      </div>
    </>
  );
}
