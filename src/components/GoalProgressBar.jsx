/**
 * Goal Progress Bar Component
 *
 * Displays a visual progress bar for savings goal progress
 * - Progress percentage capped at 100%
 * - Shows percentage text
 * - Supports both Classic and Neon themes
 */

import React from "react";
import PropTypes from "prop-types";
import "../styles.css";

const GoalProgressBar = ({
  progressPercentage = 0,
  currentBalance = 0,
  targetAmount = 0,
  theme = "classic",
}) => {
  // Coerce incoming values to numbers when possible (API may send strings)
  const balanceNum =
    typeof currentBalance === "number"
      ? currentBalance
      : parseFloat(currentBalance) || 0;
  const targetNum =
    typeof targetAmount === "number"
      ? targetAmount
      : parseFloat(targetAmount) || 0;
  const progressNum =
    typeof progressPercentage === "number"
      ? progressPercentage
      : parseFloat(progressPercentage) || 0;

  // Cap progress at 100%
  // Prefer calculating progress client-side from balances when available so
  // we don't display 100% due to unrelated rounding of a server-provided value.
  let computedProgress = null; // number
  if (targetNum > 0) {
    const balanceCents = Math.round(balanceNum * 100);
    const targetCents = Math.round(targetNum * 100);
    if (targetCents > 0) {
      // percent with two decimals, floored (no rounding up)
      const percentHundredths = Math.floor(
        (balanceCents * 10000) / targetCents,
      );
      computedProgress = percentHundredths / 100; // e.g. 99.99
    }
  }

  const actualProgress =
    computedProgress !== null ? computedProgress : progressNum;
  const displayProgress = Math.min(
    targetNum > 0 && balanceNum < targetNum ? Math.max(0, actualProgress) : 100,
    100,
  );

  const percentageText =
    targetNum > 0 && balanceNum < targetNum
      ? Number.isInteger(actualProgress)
        ? String(actualProgress)
        : actualProgress.toFixed(2)
      : "100";

  // Determine progress color based on percentage
  const getProgressColor = () => {
    // Use actual balances if available to decide completion state.
    const isComplete =
      targetNum > 0 ? balanceNum >= targetNum : progressNum >= 100;

    if (isComplete) return "var(--color-success, #28a745)";
    if (displayProgress >= 75) return "var(--color-warning, #ffc107)";
    if (displayProgress >= 50) return "var(--color-info, #17a2b8)";
    return "var(--color-primary, #007bff)";
  };

  const getThemeClass = () => {
    return theme === "neon" ? "progress-bar-neon" : "progress-bar-classic";
  };

  return (
    <div className={`goal-progress-container ${getThemeClass()}`}>
      <div className="progress-info">
        <span className="progress-label">Progress</span>
        <span className="progress-percentage">{percentageText}%</span>
      </div>

      <div className="progress-bar-wrapper">
        <div
          className="progress-bar-track"
          role="progressbar"
          aria-valuenow={displayProgress}
          aria-valuemin="0"
          aria-valuemax="100"
        >
          <div
            className="progress-bar-fill"
            style={{
              width: `${displayProgress}%`,
              backgroundColor: getProgressColor(),
            }}
          />
        </div>
      </div>

      <div className="progress-details">
        <span className="balance-info">
          Saved:{" "}
          <span className="balance-amount">${currentBalance.toFixed(2)}</span>
        </span>
        <span className="target-info">of ${targetAmount.toFixed(2)}</span>
      </div>
    </div>
  );
};

GoalProgressBar.propTypes = {
  progressPercentage: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  currentBalance: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  targetAmount: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  theme: PropTypes.oneOf(["classic", "neon"]),
};

export default GoalProgressBar;
