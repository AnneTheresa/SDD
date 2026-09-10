/**
 * Goal Edit Form Component
 *
 * Form for editing an existing savings goal
 * - Pre-populates with current goal values
 * - Validates input before submission
 * - Supports both Classic and Neon themes
 */

import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";

const GoalEditForm = ({
  goal = {},
  onSubmit = () => {},
  onCancel = () => {},
  isLoading = false,
  theme = "classic",
}) => {
  const [formData, setFormData] = useState({
    goalName: "",
    targetAmount: "",
    targetDate: "",
  });
  const [errors, setErrors] = useState({});

  useEffect(() => {
    if (goal && goal.goal_id) {
      setFormData({
        goalName: goal.goal_name || "",
        targetAmount: goal.target_amount ? goal.target_amount.toString() : "",
        targetDate: goal.target_date || "",
      });
    }
  }, [goal]);

  const handleInputChange = (field, value) => {
    setFormData((prev) => ({
      ...prev,
      [field]: value,
    }));
    if (errors[field]) {
      setErrors((prev) => ({ ...prev, [field]: "" }));
    }
  };

  const validateForm = () => {
    const newErrors = {};

    if (!formData.goalName.trim()) {
      newErrors.goalName = "Please enter a goal name";
    }

    if (!formData.targetAmount) {
      newErrors.targetAmount = "Please enter a target amount";
    } else if (parseFloat(formData.targetAmount) <= 0) {
      newErrors.targetAmount = "Target amount must be greater than $0";
    }

    if (!formData.targetDate) {
      newErrors.targetDate = "Please select a target date";
    } else {
      const selectedDate = new Date(formData.targetDate);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      // For edit, allow same date or future date (no past restriction)
      if (selectedDate < today) {
        newErrors.targetDate = "Target date must be today or in the future";
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = () => {
    if (validateForm()) {
      onSubmit({
        goalName: formData.goalName,
        targetAmount: parseFloat(formData.targetAmount),
        targetDate: formData.targetDate,
      });
    }
  };

  const getThemeClass = () => {
    return theme === "neon" ? "edit-form-neon" : "edit-form-classic";
  };

  return (
    <div className={`goal-edit-form ${getThemeClass()}`}>
      <div className="form-header">
        <h2>Edit Savings Goal</h2>
        <p className="goal-info">Goal ID: {goal.goal_id}</p>
      </div>

      <div className="form-body">
        <div className="form-group">
          <label htmlFor="goalName" className="form-label">
            Goal Name
          </label>
          <input
            id="goalName"
            type="text"
            placeholder="What are you saving for?"
            value={formData.goalName}
            onChange={(e) => handleInputChange("goalName", e.target.value)}
            className={`form-input ${errors.goalName ? "error" : ""}`}
            disabled={isLoading}
          />
          {errors.goalName && (
            <p className="error-message">{errors.goalName}</p>
          )}
        </div>

        <div className="form-group">
          <label htmlFor="targetAmount" className="form-label">
            Target Amount
          </label>
          <div className="amount-input-group">
            <span className="currency">$</span>
            <input
              id="targetAmount"
              type="number"
              placeholder="0.00"
              value={formData.targetAmount}
              onChange={(e) =>
                handleInputChange("targetAmount", e.target.value)
              }
              min="0.01"
              step="0.01"
              className={`form-input ${errors.targetAmount ? "error" : ""}`}
              disabled={isLoading}
            />
          </div>
          {errors.targetAmount && (
            <p className="error-message">{errors.targetAmount}</p>
          )}
        </div>

        <div className="form-group">
          <label htmlFor="targetDate" className="form-label">
            Target Date
          </label>
          <input
            id="targetDate"
            type="date"
            value={formData.targetDate}
            onChange={(e) => handleInputChange("targetDate", e.target.value)}
            min={new Date().toISOString().split("T")[0]}
            className={`form-input ${errors.targetDate ? "error" : ""}`}
            disabled={isLoading}
          />
          {errors.targetDate && (
            <p className="error-message">{errors.targetDate}</p>
          )}
        </div>
      </div>

      <div className="form-footer">
        <button
          className="btn btn-secondary"
          onClick={onCancel}
          disabled={isLoading}
        >
          Cancel
        </button>
        <button
          className="btn btn-primary"
          onClick={handleSubmit}
          disabled={isLoading}
        >
          {isLoading ? "Saving..." : "Save Changes"}
        </button>
      </div>
    </div>
  );
};

GoalEditForm.propTypes = {
  goal: PropTypes.shape({
    goal_id: PropTypes.number,
    goal_name: PropTypes.string,
    target_amount: PropTypes.number,
    target_date: PropTypes.string,
  }),
  onSubmit: PropTypes.func,
  onCancel: PropTypes.func,
  isLoading: PropTypes.bool,
  theme: PropTypes.oneOf(["classic", "neon"]),
};

export default GoalEditForm;
