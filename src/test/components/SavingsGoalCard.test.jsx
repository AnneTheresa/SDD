/**
 * SavingsGoalCard component tests
 * Covers T029: progress bar renders at correct %, time remaining displays, status badge shows
 * Covers T070: edge cases (100%, overdue, balance changes)
 */

import { render, screen, fireEvent } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import SavingsGoalCard from "../../components/SavingsGoalCard";

const makeGoal = (overrides = {}) => ({
  goal_id: 1,
  account_id: 100,
  account_number: "ACC-001",
  account_type: "SAVINGS",
  goal_name: "Travel",
  target_amount: 5000,
  target_date: "2027-06-01",
  current_balance: 1000,
  progress_percentage: 20,
  time_remaining_days: 365,
  status: "IN_PROGRESS",
  ...overrides,
});

describe("SavingsGoalCard", () => {
  beforeEach(() => vi.clearAllMocks());

  it("renders goal name and account info", () => {
    render(<SavingsGoalCard goal={makeGoal()} />);
    expect(screen.getByText("Travel")).toBeTruthy();
    expect(screen.getByText(/ACC-001/)).toBeTruthy();
  });

  it("renders progress bar with correct percentage label", () => {
    render(<SavingsGoalCard goal={makeGoal({ progress_percentage: 20 })} />);
    expect(screen.getByText("20%")).toBeTruthy();
  });

  it("T070 edge case: renders 100% when balance equals target", () => {
    render(
      <SavingsGoalCard
        goal={makeGoal({
          progress_percentage: 100,
          status: "ACHIEVED",
          current_balance: 5000,
        })}
      />,
    );
    expect(screen.getByText("100%")).toBeTruthy();
    expect(screen.getByText("Achieved")).toBeTruthy();
  });

  it("T070 edge case: progress capped at 100% even when percentage > 100", () => {
    render(
      <SavingsGoalCard
        goal={makeGoal({ progress_percentage: 100, current_balance: 6000 })}
      />,
    );
    // GoalProgressBar caps at 100 — rendered percentage should be 100, not 120
    expect(screen.getByText("100%")).toBeTruthy();
  });

  it("keeps near-complete goals below 100% and renders one time remaining row", () => {
    render(
      <SavingsGoalCard
        goal={makeGoal({
          target_amount: 1000,
          current_balance: 999,
          progress_percentage: 99.9,
          time_remaining_days: 30,
        })}
      />,
    );

    expect(screen.getByText("99.90%")).toBeTruthy();
    expect(screen.queryByText("100%")).toBeNull();
    expect(screen.getAllByText(/Time Remaining:/i)).toHaveLength(1);
  });

  it("displays time remaining in days", () => {
    render(<SavingsGoalCard goal={makeGoal({ time_remaining_days: 180 })} />);
    expect(screen.getByText(/180 days/)).toBeTruthy();
  });

  it("caps short-term recommendation to one month", () => {
    render(
      <SavingsGoalCard
        goal={makeGoal({
          target_amount: 5000,
          current_balance: 1000,
          time_remaining_days: 3,
        })}
      />,
    );

    expect(screen.getByText("$4000.00 / mo")).toBeTruthy();
  });

  it("keeps the recommended monthly contribution above zero when a small amount remains", () => {
    render(
      <SavingsGoalCard
        goal={makeGoal({
          target_amount: 1001,
          current_balance: 1000,
          time_remaining_days: 355653,
        })}
      />,
    );

    expect(screen.getByText("$1.00 / mo")).toBeTruthy();
  });

  it('T070 edge case: shows "Past deadline" when time_remaining_days is 0', () => {
    render(
      <SavingsGoalCard
        goal={makeGoal({ time_remaining_days: 0, status: "OVERDUE" })}
      />,
    );
    expect(screen.getByText(/Past deadline/i)).toBeTruthy();
    expect(screen.getByText("Overdue")).toBeTruthy();
  });

  it("shows ACHIEVED status badge", () => {
    render(
      <SavingsGoalCard
        goal={makeGoal({ status: "ACHIEVED", progress_percentage: 100 })}
      />,
    );
    expect(screen.getByText("Achieved")).toBeTruthy();
  });

  it("shows OVERDUE status badge", () => {
    render(
      <SavingsGoalCard
        goal={makeGoal({ status: "OVERDUE", time_remaining_days: 0 })}
      />,
    );
    expect(screen.getByText("Overdue")).toBeTruthy();
  });

  it("shows NOT_STARTED status badge", () => {
    render(
      <SavingsGoalCard
        goal={makeGoal({
          status: "NOT_STARTED",
          progress_percentage: 0,
          current_balance: 0,
        })}
      />,
    );
    expect(screen.getByText("Not Started")).toBeTruthy();
  });

  it("calls onEdit when Edit button is clicked", () => {
    const onEdit = vi.fn();
    const goal = makeGoal();
    render(<SavingsGoalCard goal={goal} onEdit={onEdit} />);
    fireEvent.click(screen.getByText("Edit"));
    expect(onEdit).toHaveBeenCalledWith(goal);
  });

  it("shows confirmation UI when Delete button is clicked", () => {
    render(<SavingsGoalCard goal={makeGoal()} />);
    fireEvent.click(screen.getByText("Delete"));
    expect(screen.getByText(/Are you sure/i)).toBeTruthy();
  });

  it("calls onDelete when confirmed", () => {
    const onDelete = vi.fn();
    const goal = makeGoal();
    render(<SavingsGoalCard goal={goal} onDelete={onDelete} />);
    fireEvent.click(screen.getByText("Delete"));
    fireEvent.click(screen.getAllByText("Delete")[1]); // second Delete in confirmation
    expect(onDelete).toHaveBeenCalledWith(goal);
  });

  it("hides confirmation when Cancel is clicked", () => {
    render(<SavingsGoalCard goal={makeGoal()} />);
    fireEvent.click(screen.getByText("Delete"));
    fireEvent.click(screen.getByText("Cancel"));
    expect(screen.queryByText(/Are you sure/i)).toBeNull();
  });

  it("returns null when no valid goal is provided", () => {
    const { container } = render(<SavingsGoalCard goal={{}} />);
    expect(container.firstChild).toBeNull();
  });

  it("disables buttons when isLoading is true", () => {
    render(<SavingsGoalCard goal={makeGoal()} isLoading={true} />);
    const editBtn = screen.getByText("Edit");
    const deleteBtn = screen.getByText("Delete");
    expect(editBtn).toHaveProperty("disabled", true);
    expect(deleteBtn).toHaveProperty("disabled", true);
  });
});
