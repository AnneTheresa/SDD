/**
 * GoalEditForm component tests
 * Covers T050: form pre-populates, validation errors display, PUT called with changed fields
 */

import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import GoalEditForm from "../../components/GoalEditForm";

const sampleGoal = {
  goal_id: 1,
  goal_name: "Travel",
  target_amount: 5000,
  target_date: "2027-06-01",
};

describe("GoalEditForm", () => {
  beforeEach(() => vi.clearAllMocks());

  it("pre-populates form with existing goal values", () => {
    render(<GoalEditForm goal={sampleGoal} />);
    expect(screen.getByDisplayValue("Travel")).toBeTruthy();
    expect(screen.getByDisplayValue("5000")).toBeTruthy();
    expect(screen.getByDisplayValue("2027-06-01")).toBeTruthy();
  });

  it("shows validation error when goal name is cleared", async () => {
    render(<GoalEditForm goal={sampleGoal} />);
    const nameInput = screen.getByDisplayValue("Travel");
    fireEvent.change(nameInput, { target: { value: "" } });
    fireEvent.click(screen.getByText("Save Changes"));
    await waitFor(() => {
      expect(screen.getByText(/Please enter a goal name/i)).toBeTruthy();
    });
  });

  it("shows validation error when target amount is set to 0", async () => {
    render(<GoalEditForm goal={sampleGoal} />);
    const amountInput = screen.getByDisplayValue("5000");
    fireEvent.change(amountInput, { target: { value: "0" } });
    fireEvent.click(screen.getByText("Save Changes"));
    await waitFor(() => {
      expect(screen.getByText(/greater than \$0/i)).toBeTruthy();
    });
  });

  it("calls onSubmit with changed fields when form is valid", async () => {
    const onSubmit = vi.fn();
    render(<GoalEditForm goal={sampleGoal} onSubmit={onSubmit} />);

    const nameInput = screen.getByDisplayValue("Travel");
    fireEvent.change(nameInput, { target: { value: "Updated Travel" } });

    const amountInput = screen.getByDisplayValue("5000");
    fireEvent.change(amountInput, { target: { value: "6000" } });

    fireEvent.click(screen.getByText("Save Changes"));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({
        goalName: "Updated Travel",
        targetAmount: 6000,
        targetDate: "2027-06-01",
      });
    });
  });

  it("calls onCancel when Cancel is clicked", () => {
    const onCancel = vi.fn();
    render(<GoalEditForm goal={sampleGoal} onCancel={onCancel} />);
    fireEvent.click(screen.getByText("Cancel"));
    expect(onCancel).toHaveBeenCalled();
  });

  it("disables inputs and Save button when isLoading", () => {
    render(<GoalEditForm goal={sampleGoal} isLoading={true} />);
    const saveBtn = screen.getByText("Saving...");
    expect(saveBtn).toHaveProperty("disabled", true);
  });
});
