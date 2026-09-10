/**
 * GoalDeleteConfirmation component tests
 * Covers T062: modal shows confirmation text, Delete button calls deleteGoal()
 */

import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import GoalDeleteConfirmation from "../../components/GoalDeleteConfirmation";

describe("GoalDeleteConfirmation", () => {
  beforeEach(() => vi.clearAllMocks());

  it("renders nothing when isOpen is false", () => {
    const { container } = render(
      <GoalDeleteConfirmation
        isOpen={false}
        goalName="Travel"
        targetAmount={5000}
      />,
    );
    expect(container.firstChild).toBeNull();
  });

  it("renders confirmation text when isOpen is true", () => {
    render(
      <GoalDeleteConfirmation
        isOpen={true}
        goalName="Travel"
        targetAmount={5000}
      />,
    );
    expect(screen.getByText(/Delete Savings Goal/i)).toBeTruthy();
    expect(screen.getByText(/Are you sure/i)).toBeTruthy();
    expect(screen.getByText("Travel")).toBeTruthy();
    expect(screen.getByText(/\$5000\.00/)).toBeTruthy();
  });

  it("calls onConfirm when Delete Goal button is clicked", () => {
    const onConfirm = vi.fn();
    render(
      <GoalDeleteConfirmation
        isOpen={true}
        goalName="Travel"
        targetAmount={5000}
        onConfirm={onConfirm}
      />,
    );
    fireEvent.click(screen.getByText("Delete Goal"));
    expect(onConfirm).toHaveBeenCalled();
  });

  it("calls onCancel when Cancel button is clicked", () => {
    const onCancel = vi.fn();
    render(
      <GoalDeleteConfirmation
        isOpen={true}
        goalName="Travel"
        targetAmount={5000}
        onCancel={onCancel}
      />,
    );
    fireEvent.click(screen.getByText("Cancel"));
    expect(onCancel).toHaveBeenCalled();
  });

  it("shows Deleting... and disables button when isLoading is true", () => {
    render(
      <GoalDeleteConfirmation
        isOpen={true}
        goalName="Travel"
        targetAmount={5000}
        isLoading={true}
      />,
    );
    const deleteBtn = screen.getByText("Deleting...");
    expect(deleteBtn).toHaveProperty("disabled", true);
  });

  it("shows warning text about permanent deletion", () => {
    render(
      <GoalDeleteConfirmation
        isOpen={true}
        goalName="Travel"
        targetAmount={5000}
      />,
    );
    expect(screen.getByText(/cannot be undone/i)).toBeTruthy();
  });
});
