import { Fragment, useState, useEffect } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { createAccount, getInterestRates } from "../api/accounts";
import { getCustomer, listCustomers } from "../api/customers";
import { mapAxiosError } from "../api/axiosClient";
import { useAuth } from "../auth/AuthContext";
import { useListCustomerAccounts } from "../hooks/useListCustomerAccounts";
import { ACCOUNT_TYPES, emptyCreateAccountForm } from "../types";
import useGoals from "../hooks/useGoals";
import SavingsGoalCard from "../components/SavingsGoalCard";
import GoalCreationFlow from "../components/GoalCreationFlow";
import GoalEditForm from "../components/GoalEditForm";

// Set to false to test KYC error boundaries
const MOCK_KYC_VERIFIED = true;

export function AccountListPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const { customerId } = useParams();
  const { isAdmin, rememberCustomerId } = useAuth();
  const [formState, setFormState] = useState(emptyCreateAccountForm);
  const [error, setError] = useState(null);
  const [actionMessage, setActionMessage] = useState(null);
  // Show registration success message if present, then clear it from history
  useEffect(() => {
    if (location.state?.successMessage) {
      setActionMessage(location.state.successMessage);
      // Remove the successMessage from history so it doesn't persist on refresh or navigation
      navigate(location.pathname, { replace: true });
    }
  }, [location.state, location.pathname, navigate]);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [expandedAccountId, setExpandedAccountId] = useState(null);

  // Savings Goals state
  const {
    goals,
    loading: goalsLoading,
    error: goalsError,
    createGoal,
    updateGoal,
    deleteGoal,
    getAllGoals,
  } = useGoals();
  const [isGoalCreationOpen, setIsGoalCreationOpen] = useState(false);
  const [goalCreationAccountId, setGoalCreationAccountId] = useState(null);
  const [editingGoal, setEditingGoal] = useState(null);
  const [goalActionMessage, setGoalActionMessage] = useState(null);

  useEffect(() => {
    if (customerId && !isAdmin) {
      getAllGoals(customerId).catch(() => {});
    }
  }, [customerId, isAdmin, getAllGoals]);

  const goalsByAccountId = (goals || []).reduce((map, goal) => {
    map[goal.account_id] = goal;
    return map;
  }, {});

  const query = useListCustomerAccounts(customerId);
  const totalBalance = (query.data || []).reduce(
    (sum, acc) => sum + (parseFloat(acc.balance) || 0),
    0,
  );
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
  const createAccountMutation = useMutation({ mutationFn: createAccount });
  const interestRatesQuery = useQuery({
    queryKey: ["interest-rates"],
    queryFn: getInterestRates,
  });

  const deletedAccountMessage = location.state?.deletedAccountMessage || null;
  const flashMessage = location.state?.flash || null;
  const accountsError = query.error
    ? mapDeletedCustomerAccountsError(query.error)
    : null;
  const customerError = customerQuery.error
    ? mapDeletedCustomerAccountsError(customerQuery.error)
    : null;

  async function handleCreateAccount(event) {
    event.preventDefault();
    setError(null);
    setActionMessage(null);

    if (existingTypes.has(formState.accountType)) {
      setError({
        message: `You already have an active ${formState.accountType} account.`,
      });
      return;
    }

    try {
      const createdAccount = await createAccountMutation.mutateAsync({
        ...formState,
        customerId,
        balance: formState.balance,
        interestRate: formState.interestRate,
        ...(formState.accountType === "TFSA"
          ? { dateOfBirth: customerQuery.data?.dateOfBirth }
          : {}),
      });
      setActionMessage(
        `Account ${createdAccount.accountId} created successfully.`,
      );
      setFormState(emptyCreateAccountForm);
      setIsCreateModalOpen(false);
      await Promise.all([query.refetch(), customerQuery.refetch()]);
    } catch (mutationError) {
      const mapped = mapAxiosError(mutationError);
      if (mapped.code === "TFSA_ALREADY_EXISTS") {
        setError({
          ...mapped,
          message: "You already have an active TFSA account.",
        });
      } else {
        setError(mapped);
      }
    }
  }

  async function handleCreateGoal(goalData) {
    try {
      await createGoal(goalCreationAccountId, goalData);
      setIsGoalCreationOpen(false);
      setGoalCreationAccountId(null);
      setGoalActionMessage("Savings goal created successfully.");
      getAllGoals(customerId).catch(() => {});
    } catch (err) {
      // error handled by useGoals hook
    }
  }

  async function handleUpdateGoal(goalData) {
    try {
      await updateGoal(editingGoal.account_id, editingGoal.goal_id, goalData);
      setEditingGoal(null);
      setGoalActionMessage("Savings goal updated successfully.");
      getAllGoals(customerId).catch(() => {});
    } catch (err) {
      // error handled by useGoals hook
    }
  }

  async function handleDeleteGoal(goal) {
    try {
      await deleteGoal(goal.account_id, goal.goal_id);
      setGoalActionMessage("Savings goal deleted.");
      getAllGoals(customerId).catch(() => {});
    } catch (err) {
      // error handled by useGoals hook
    }
  }

  function handleCustomerSwitch(event) {
    const nextCustomerId = event.target.value;
    if (!nextCustomerId) {
      return;
    }

    setError(null);
    setActionMessage(null);
    rememberCustomerId(nextCustomerId);
    navigate(`/customer/${nextCustomerId}/accounts`);
  }

  const showInterestRate =
    formState.accountType === "SAVINGS" ||
    (formState.accountType === "TFSA" && isAdmin);
  const showBalance =
    formState.accountType !== "RRSP" &&
    (formState.accountType !== "TFSA" || isAdmin);
  const existingTypes = new Set((query.data || []).map((a) => a.accountType));
  const isDuplicateType = existingTypes.has(formState.accountType);
  const isTfsa = formState.accountType === "TFSA";
  const customerDateOfBirth = customerQuery.data?.dateOfBirth || null;
  const customerKycVerified = MOCK_KYC_VERIFIED;
  const age = calculateAge(customerDateOfBirth);
  const isTooYoung = isTfsa && age !== null && age < 18;
  const isKycBlocked = isTfsa && !customerKycVerified;
  const tfsaRate =
    interestRatesQuery.data?.TFSA ?? interestRatesQuery.data?.tfsa ?? null;
  const rrspRate =
    interestRatesQuery.data?.RRSP ?? interestRatesQuery.data?.rrsp ?? null;

  function openCreateModal() {
    setError(null);
    setActionMessage(null);
    setIsCreateModalOpen(true);
  }

  function closeCreateModal() {
    if (createAccountMutation.isPending) {
      return;
    }

    setIsCreateModalOpen(false);
    setError(null);
  }

  return (
    <>
      {/* Banner messages at the very top, outside main content */}
      <div className="banner-stack">
        {deletedAccountMessage ? (
          <div className="banner success">{deletedAccountMessage}</div>
        ) : null}
        {flashMessage ? (
          <div className="banner info">{flashMessage}</div>
        ) : null}
        {actionMessage ? (
          <div className="banner success">{actionMessage}</div>
        ) : null}
        {error && !isCreateModalOpen ? (
          <div className="banner error">{error.message}</div>
        ) : null}
        {query.isLoading ? (
          <div className="banner success">Loading accounts...</div>
        ) : null}
        {accountsError ? (
          <div className="banner error">{accountsError.message}</div>
        ) : null}
        {customerError ? (
          <div className="banner error">{customerError.message}</div>
        ) : null}
      </div>
      <div className="stack">
        <section className="panel stack">
          <div className="page-header-row">
            <div>
              <h2>{isAdmin && customerQuery.data?.name ? `${customerQuery.data.name}'s Accounts` : 'My Accounts'}</h2>
              <p className="muted text-top-muted">
                {isAdmin && customerQuery.data?.name ? `View and manage ${customerQuery.data.name}'s accounts.` : 'View and manage your accounts.'}
              </p>
            </div>
            {!accountsError && !customerError ? (
              <button type="button" onClick={openCreateModal}>
                Create Account
              </button>
            ) : null}
          </div>
          {isAdmin ? (
            <div className="field">
              <label htmlFor="accounts-customer-switcher">
                Admin Customer Switcher
              </label>
              <select
                id="accounts-customer-switcher"
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
          <section className="accounts-table-shell">
            {accountsError || customerError ? (
              <div className="panel">
                <h3>Accounts unavailable</h3>
                <p className="muted">
                  The selected customer's accounts cannot be displayed right
                  now.
                </p>
              </div>
            ) : query.data && query.data.length > 0 ? (
              <div className="account-card-list">
                {query.data.map((account) => (
                  <Link
                    key={account.accountId}
                    className="account-card"
                    to={`/accounts/${account.accountId}`}
                  >
                    <div className="account-card-header">
                      <span className="account-card-type">
                        {account.accountType}
                        {account.accountType === "TFSA" ? (
                          <span className="badge badge-inline-offset">
                            Tax-Free
                          </span>
                        ) : null}
                        {account.status === "FROZEN" ? (
                          <span className="badge badge-warning badge-inline-offset">
                            Frozen
                          </span>
                        ) : null}
                      </span>
                      <span className="account-card-id">
                        {account.accountId}
                      </span>
                    </div>
                    <span className="account-card-balance">
                      ${account.balance}
                    </span>
                    {account.accountType === "TFSA" && tfsaRate !== null ? (
                      <span className="field-hint">{tfsaRate}% APY</span>
                    ) : null}
                  </Link>
                ))}
                <div className="account-card account-card-total">
                  <div className="account-card-header">
                    <span className="account-card-type">
                      Total Account Balance
                    </span>
                  </div>
                  <span className="account-card-balance">
                    ${totalBalance.toFixed(2)}
                  </span>
                </div>
              </div>
            ) : (
              <div className="panel">
                <h3>No active accounts</h3>
              </div>
            )}
          </section>
        </section>

        {/* Savings Goals Section */}
        {!isAdmin ? <section className="panel stack">
          <div className="page-header-row">
            <div>
              <h2>Savings Goals</h2>
              <p className="muted text-top-muted">
                Track your savings progress for each account.
              </p>
            </div>
          </div>
          {goalActionMessage ? (
            <div className="banner success">{goalActionMessage}</div>
          ) : null}
          {goalsError ? (
            <div className="banner error">
              {goalsError.message || "Could not load savings goals."}
            </div>
          ) : null}
          {goalsLoading ? (
            <div className="banner info">Loading goals...</div>
          ) : null}

          {query.data && query.data.length > 0 ? (
            <div className="goal-list">
              {query.data.map((account) => {
                const goal = goalsByAccountId[account.accountId];
                return (
                  <div key={account.accountId} className="goal-list-item">
                    {goal ? (
                      <SavingsGoalCard
                        goal={goal}
                        onEdit={(g) => setEditingGoal(g)}
                        onDelete={handleDeleteGoal}
                        isLoading={goalsLoading}
                      />
                    ) : (
                      <div className="goal-empty-card">
                        <div className="goal-empty-info">
                          <span className="goal-empty-account">
                            {account.accountType} ··
                            {account.accountNumber?.slice(-4)}
                          </span>
                          <span className="goal-empty-label">
                            No savings goal yet
                          </span>
                        </div>
                        <button
                          type="button"
                          className="secondary"
                          onClick={() => {
                            setGoalCreationAccountId(account.accountId);
                            setIsGoalCreationOpen(true);
                          }}
                        >
                          + Add a goal
                        </button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          ) : null}
        </section> : null}
        {isCreateModalOpen ? (
          <div className="modal-backdrop" onClick={closeCreateModal}>
            <div
              className="modal-panel stack"
              role="dialog"
              aria-modal="true"
              aria-labelledby="create-account-modal-title"
              onClick={(event) => event.stopPropagation()}
            >
              <div className="section-header">
                <div>
                  <h3 id="create-account-modal-title">Create Account</h3>
                </div>
                <button
                  type="button"
                  className="secondary"
                  onClick={closeCreateModal}
                  disabled={createAccountMutation.isPending}
                >
                  Close
                </button>
              </div>
              <form className="stack" onSubmit={handleCreateAccount}>
                {error ? (
                  <div className="banner error">{error.message}</div>
                ) : null}
                {isKycBlocked ? (
                  <div className="banner error">
                    Identity verification required for this account type.
                  </div>
                ) : null}
                {isTooYoung ? (
                  <div className="banner error">
                    TFSA requires a minimum age of 18.
                  </div>
                ) : null}
                <div className="stack">
                  <div className="field">
                    <label htmlFor="accountType">Account Type</label>
                    <select
                      id="accountType"
                      value={formState.accountType}
                      onChange={(event) =>
                        setFormState((current) => ({
                          ...current,
                          accountType: event.target.value,
                        }))
                      }
                    >
                      {ACCOUNT_TYPES.map((type) => (
                        <option
                          key={type}
                          value={type}
                          disabled={type === "TFSA" && !customerKycVerified}
                        >
                          {type}
                        </option>
                      ))}
                    </select>
                    {isTfsa && tfsaRate !== null ? (
                      <p className="field-hint">
                        Current TFSA rate: {tfsaRate}% APY
                      </p>
                    ) : null}
                    {formState.accountType === "RRSP" && rrspRate !== null ? (
                      <p className="field-hint">
                        Current RRSP rate: {rrspRate}% APY
                      </p>
                    ) : null}
                  </div>
                  {showBalance ? (
                    <div className="field">
                      <label htmlFor="balance">
                        {isTfsa ? "Initial Contribution" : "Opening Balance"}
                      </label>
                      <input
                        id="balance"
                        value={formState.balance}
                        onChange={(event) =>
                          setFormState((current) => ({
                            ...current,
                            balance: event.target.value,
                          }))
                        }
                      />
                    </div>
                  ) : null}
                  {showInterestRate ? (
                    <div className="field">
                      <label htmlFor="interestRate">Interest Rate</label>
                      <input
                        id="interestRate"
                        value={formState.interestRate}
                        onChange={(event) =>
                          setFormState((current) => ({
                            ...current,
                            interestRate: event.target.value,
                          }))
                        }
                      />
                    </div>
                  ) : null}
                </div>
                <div className="actions centered-actions">
                  <button
                    type="submit"
                    disabled={
                      createAccountMutation.isPending ||
                      isTooYoung ||
                      isKycBlocked
                    }
                  >
                    Create Account
                  </button>
                  <button
                    type="button"
                    className="secondary"
                    onClick={() => {
                      setFormState(emptyCreateAccountForm);
                      setError(null);
                    }}
                    disabled={createAccountMutation.isPending}
                  >
                    Reset
                  </button>
                </div>
              </form>
            </div>
          </div>
        ) : null}

        {/* Goal Creation Modal */}
        {isGoalCreationOpen ? (
          <div
            className="modal-backdrop"
            onClick={() => setIsGoalCreationOpen(false)}
          >
            <div
              className="modal-panel"
              role="dialog"
              aria-modal="true"
              onClick={(e) => e.stopPropagation()}
            >
              <GoalCreationFlow
                accountNumber={
                  query.data?.find((a) => a.accountId === goalCreationAccountId)
                    ?.accountNumber || ""
                }
                onSubmit={handleCreateGoal}
                onCancel={() => {
                  setIsGoalCreationOpen(false);
                  setGoalCreationAccountId(null);
                }}
                isLoading={goalsLoading}
              />
            </div>
          </div>
        ) : null}

        {/* Goal Edit Modal */}
        {editingGoal ? (
          <div className="modal-backdrop" onClick={() => setEditingGoal(null)}>
            <div
              className="modal-panel"
              role="dialog"
              aria-modal="true"
              onClick={(e) => e.stopPropagation()}
            >
              <GoalEditForm
                goal={editingGoal}
                onSubmit={handleUpdateGoal}
                onCancel={() => setEditingGoal(null)}
                isLoading={goalsLoading}
              />
            </div>
          </div>
        ) : null}
      </div>
    </>
  );
}

function calculateAge(dateOfBirth) {
  if (!dateOfBirth) return null;
  const dob = new Date(dateOfBirth);
  if (isNaN(dob.getTime())) return null;
  const today = new Date();
  let age = today.getFullYear() - dob.getFullYear();
  const monthDiff = today.getMonth() - dob.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < dob.getDate())) {
    age--;
  }
  return age;
}

function mapDeletedCustomerAccountsError(error) {
  const mapped = mapAxiosError(error);

  if (
    mapped.code === "CUSTOMER_NOT_FOUND" ||
    mapped.message === "Customer not found"
  ) {
    return {
      ...mapped,
      message:
        "This customer may have been deleted or is no longer accessible, so their accounts cannot be shown.",
    };
  }

  return mapped;
}
