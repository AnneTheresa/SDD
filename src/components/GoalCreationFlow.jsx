/**
 * Goal Creation Flow Component
 *
 * 3-question form for creating a new savings goal:
 * Q1: What are you saving for? (dropdown with presets + custom option)
 * Q2: How much do you plan to save? (numeric input)
 * Q3: By when? (date picker)
 * Review screen before submission
 *
 * Supports both Classic and Neon themes
 */

import React, { useState } from "react";
import PropTypes from "prop-types";

const GOAL_PRESETS = [
  "Emergency Fund",
  "Travel",
  "Tuition",
  "Home",
  "Car",
  "Retirement",
  "Other",
];

const GoalCreationFlow = ({
  accountNumber = "",
  onSubmit = () => {},
  onCancel = () => {},
  isLoading = false,
  theme = "classic",
}) => {
  const [step, setStep] = useState(1);
  const [formData, setFormData] = useState({
    goalName: "",
    targetAmount: "",
    targetDate: "",
  });
  const [errors, setErrors] = useState({});

  const handleGoalNameChange = (e) => {
    const value = e.target.value;
    setFormData((prev) => ({
      ...prev,
      goalName: value,
    }));
    if (errors.goalName) {
      setErrors((prev) => ({ ...prev, goalName: "" }));
    }
  };

  const handleAmountChange = (e) => {
    const value = e.target.value;
    setFormData((prev) => ({
      ...prev,
      targetAmount: value,
    }));
    if (errors.targetAmount) {
      setErrors((prev) => ({ ...prev, targetAmount: "" }));
    }
  };

  const handleDateChange = (e) => {
    const value = e.target.value;
    setFormData((prev) => ({
      ...prev,
      targetDate: value,
    }));
    if (errors.targetDate) {
      setErrors((prev) => ({ ...prev, targetDate: "" }));
    }
  };

  const validateStep = () => {
    const newErrors = {};

    if (step === 1) {
      if (!formData.goalName.trim()) {
        newErrors.goalName = "Please enter a goal name";
      }
    } else if (step === 2) {
      if (!formData.targetAmount) {
        newErrors.targetAmount = "Please enter a target amount";
      } else if (parseFloat(formData.targetAmount) <= 0) {
        newErrors.targetAmount = "Target amount must be greater than $0";
      } else if (formData.targetAmount.split(".")[1]?.length > 2) {
        newErrors.targetAmount =
          "Target amount can have up to 2 decimal places";
      }
    } else if (step === 3) {
      if (!formData.targetDate) {
        newErrors.targetDate = "Please select a target date";
      } else {
        const selectedDate = new Date(formData.targetDate);
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        if (selectedDate < today) {
          newErrors.targetDate = "Target date must be today or in the future";
        }
      }
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleNext = () => {
    if (validateStep()) {
      if (step < 3) {
        setStep(step + 1);
      } else {
        setStep(4); // Review step
      }
    }
  };

  const handlePrevious = () => {
    if (step > 1) {
      setStep(step - 1);
    }
  };

  const handleSubmit = () => {
    if (onSubmit) {
      onSubmit({
        goalName: formData.goalName,
        targetAmount: parseFloat(formData.targetAmount),
        targetDate: formData.targetDate,
      });
    }
  };

  const getThemeClass = () => {
    return theme === "neon" ? "creation-flow-neon" : "creation-flow-classic";
  };

  return (
    <div className={`goal-creation-flow ${getThemeClass()}`}>
      <div className="flow-header">
        <h2>Create a Savings Goal</h2>
        <p className="account-info">Account: {accountNumber}</p>
        <div className="step-indicator">
          <span className={`step ${step >= 1 ? "active" : ""}`}>1</span>
          <span className="step-line"></span>
          <span className={`step ${step >= 2 ? "active" : ""}`}>2</span>
          <span className="step-line"></span>
          <span className={`step ${step >= 3 ? "active" : ""}`}>3</span>
          <span className="step-line"></span>
          <span className={`step ${step >= 4 ? "active" : ""}`}>Review</span>
        </div>
      </div>

      <div className="flow-content">
        {step === 1 && (
          <div className="step-content">
            <label className="step-label">What are you saving for?</label>
            <select
              value={formData.goalName}
              onChange={(e) => {
                if (e.target.value === "custom") {
                  // Will handle custom input
                  handleGoalNameChange({ target: { value: "" } });
                } else {
                  handleGoalNameChange(e);
                }
              }}
              className={`step-select ${errors.goalName ? "error" : ""}`}
            >
              <option value="">Select a preset or enter custom...</option>
              {GOAL_PRESETS.map((preset) => (
                <option key={preset} value={preset}>
                  {preset}
                </option>
              ))}
            </select>
            {!GOAL_PRESETS.includes(formData.goalName) && (
              <input
                type="text"
                placeholder="Or enter your own goal..."
                value={formData.goalName}
                onChange={handleGoalNameChange}
                className="custom-input"
              />
            )}
            {errors.goalName && (
              <p className="error-message">{errors.goalName}</p>
            )}
          </div>
        )}

        {step === 2 && (
          <div className="step-content">
            <label className="step-label">How much do you plan to save?</label>
            <div className="amount-input-group">
              <span className="currency">$</span>
              <input
                type="number"
                placeholder="0.00"
                value={formData.targetAmount}
                onChange={handleAmountChange}
                min="0.01"
                step="0.01"
                className={`amount-input ${errors.targetAmount ? "error" : ""}`}
              />
            </div>
            {errors.targetAmount && (
              <p className="error-message">{errors.targetAmount}</p>
            )}
          </div>
        )}

        {step === 3 && (
          <div className="step-content">
            <label className="step-label">By when?</label>
            <input
              type="date"
              value={formData.targetDate}
              onChange={handleDateChange}
              min={new Date().toISOString().split("T")[0]}
              className={`date-input ${errors.targetDate ? "error" : ""}`}
            />
            {errors.targetDate && (
              <p className="error-message">{errors.targetDate}</p>
            )}
          </div>
        )}

        {step === 4 && (
          <div className="step-content review-content">
            <h3>Review Your Goal</h3>
            <div className="review-items">
              <div className="review-item">
                <span className="review-label">Goal:</span>
                <span className="review-value">{formData.goalName}</span>
              </div>
              <div className="review-item">
                <span className="review-label">Target Amount:</span>
                <span className="review-value">
                  ${parseFloat(formData.targetAmount).toFixed(2)}
                </span>
              </div>
              <div className="review-item">
                <span className="review-label">Target Date:</span>
                <span className="review-value">
                  {new Date(formData.targetDate).toLocaleDateString()}
                </span>
              </div>
            </div>
            <p className="review-note">
              Click "Create Goal" to save this goal.
            </p>
          </div>
        )}
      </div>

      <div className="flow-footer">
        <button
          className="btn btn-secondary"
          onClick={step === 4 ? onCancel : handlePrevious}
          disabled={isLoading || step === 1}
        >
          {step === 4 ? "Cancel" : "Back"}
        </button>
        <button
          className="btn btn-primary"
          onClick={step === 4 ? handleSubmit : handleNext}
          disabled={isLoading}
        >
          {step === 4 ? (isLoading ? "Creating..." : "Create Goal") : "Next"}
        </button>
      </div>
    </div>
  );
};

GoalCreationFlow.propTypes = {
  accountNumber: PropTypes.string,
  onSubmit: PropTypes.func,
  onCancel: PropTypes.func,
  isLoading: PropTypes.bool,
  theme: PropTypes.oneOf(["classic", "neon"]),
};

export default GoalCreationFlow;
