/**
 * Goal Delete Confirmation Modal Component
 *
 * Displays confirmation dialog before deleting a savings goal
 * - Shows goal name and amount
 * - Provides Cancel and Delete buttons
 * - Supports both Classic and Neon themes
 */

import React, { useState } from "react";
import PropTypes from "prop-types";

const GoalDeleteConfirmation = ({
  isOpen = false,
  goalName = "",
  targetAmount = 0,
  onConfirm = () => {},
  onCancel = () => {},
  isLoading = false,
  theme = "classic",
}) => {
  const [hasConfirmed, setHasConfirmed] = useState(false);

  const handleConfirm = () => {
    if (onConfirm) {
      onConfirm();
    }
  };

  const handleCancel = () => {
    setHasConfirmed(false);
    if (onCancel) {
      onCancel();
    }
  };

  if (!isOpen) {
    return null;
  }

  const getThemeClass = () => {
    return theme === "neon" ? "modal-neon" : "modal-classic";
  };

  return (
    <div className={`modal-overlay ${getThemeClass()}`} role="presentation">
      <div className="modal-content" role="dialog" aria-modal="true">
        <div className="modal-header">
          <h2 className="modal-title">Delete Savings Goal</h2>
        </div>

        <div className="modal-body">
          <p className="confirmation-text">
            Are you sure you want to delete the following savings goal?
          </p>

          <div className="goal-summary">
            <div className="summary-row">
              <span className="summary-label">Goal:</span>
              <span className="summary-value">{goalName}</span>
            </div>
            <div className="summary-row">
              <span className="summary-label">Target Amount:</span>
              <span className="summary-value">${targetAmount.toFixed(2)}</span>
            </div>
          </div>

          <p className="warning-text">
            This action cannot be undone. Your goal will be permanently deleted.
          </p>
        </div>

        <div className="modal-footer">
          <button
            className="btn btn-secondary"
            onClick={handleCancel}
            disabled={isLoading}
          >
            Cancel
          </button>
          <button
            className="btn btn-danger"
            onClick={handleConfirm}
            disabled={isLoading}
          >
            {isLoading ? "Deleting..." : "Delete Goal"}
          </button>
        </div>
      </div>
    </div>
  );
};

GoalDeleteConfirmation.propTypes = {
  isOpen: PropTypes.bool,
  goalName: PropTypes.string,
  targetAmount: PropTypes.number,
  onConfirm: PropTypes.func,
  onCancel: PropTypes.func,
  isLoading: PropTypes.bool,
  theme: PropTypes.oneOf(["classic", "neon"]),
};

export default GoalDeleteConfirmation;
