/**
 * GoalCreationFlow component tests
 * Covers T028: verify form fields accept input, validation errors display, POST called with correct data
 */

import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import GoalCreationFlow from "../../components/GoalCreationFlow";

describe("GoalCreationFlow", () => {
  const defaultProps = {
    accountNumber: "ACC-001",
    onSubmit: vi.fn(),
    onCancel: vi.fn(),
    isLoading: false,
    theme: "classic",
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders step 1 with goal name question", () => {
    render(<GoalCreationFlow {...defaultProps} />);
    expect(screen.getByText(/What are you saving for/i)).toBeTruthy();
    expect(screen.getByText("Next")).toBeTruthy();
  });

  it("shows validation error when goal name is empty and Next is clicked", async () => {
    render(<GoalCreationFlow {...defaultProps} />);
    fireEvent.click(screen.getByText("Next"));
    await waitFor(() => {
      expect(screen.getByText(/Please enter a goal name/i)).toBeTruthy();
    });
  });

  it("advances to step 2 when valid goal name is selected", async () => {
    render(<GoalCreationFlow {...defaultProps} />);
    const select = screen.getByRole("combobox");
    fireEvent.change(select, { target: { value: "Travel" } });
    fireEvent.click(screen.getByText("Next"));
    await waitFor(() => {
      expect(screen.getByText(/How much do you plan to save/i)).toBeTruthy();
    });
  });

  it("shows validation error when target amount is zero", async () => {
    render(<GoalCreationFlow {...defaultProps} />);

    // Step 1: enter goal name
    const select = screen.getByRole("combobox");
    fireEvent.change(select, { target: { value: "Travel" } });
    fireEvent.click(screen.getByText("Next"));

    // Step 2: enter invalid amount
    await waitFor(() => screen.getByText(/How much do you plan to save/i));
    const amountInput = screen.getByPlaceholderText("0.00");
    fireEvent.change(amountInput, { target: { value: "0" } });
    fireEvent.click(screen.getByText("Next"));

    await waitFor(() => {
      expect(screen.getByText(/greater than \$0/i)).toBeTruthy();
    });
  });

  it("shows validation error when target amount has more than 2 decimal places", async () => {
    render(<GoalCreationFlow {...defaultProps} />);

    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: "Travel" },
    });
    fireEvent.click(screen.getByText("Next"));

    await waitFor(() => screen.getByPlaceholderText("0.00"));
    fireEvent.change(screen.getByPlaceholderText("0.00"), {
      target: { value: "10.001" },
    });
    fireEvent.click(screen.getByText("Next"));

    await waitFor(() => {
      expect(
        screen.getByText(/Target amount can have up to 2 decimal places/i),
      ).toBeTruthy();
    });
  });

  it("shows validation error when target date is in the past", async () => {
    render(<GoalCreationFlow {...defaultProps} />);

    // Step 1
    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: "Travel" },
    });
    fireEvent.click(screen.getByText("Next"));

    // Step 2
    await waitFor(() => screen.getByPlaceholderText("0.00"));
    fireEvent.change(screen.getByPlaceholderText("0.00"), {
      target: { value: "5000" },
    });
    fireEvent.click(screen.getByText("Next"));

    // Step 3: pick a past date
    await waitFor(() => screen.getByText(/By when/i));
    const pastDate = new Date();
    pastDate.setDate(pastDate.getDate() - 1);
    const dateInput =
      screen.getByDisplayValue?.("") ||
      document.querySelector('input[type="date"]');
    fireEvent.change(dateInput, {
      target: { value: pastDate.toISOString().split("T")[0] },
    });
    fireEvent.click(screen.getByText("Next"));

    await waitFor(() => {
      expect(screen.getByText(/must be today or in the future/i)).toBeTruthy();
    });
  });

  it("shows review screen after all 3 valid steps", async () => {
    render(<GoalCreationFlow {...defaultProps} />);

    // Step 1
    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: "Travel" },
    });
    fireEvent.click(screen.getByText("Next"));

    // Step 2
    await waitFor(() => screen.getByPlaceholderText("0.00"));
    fireEvent.change(screen.getByPlaceholderText("0.00"), {
      target: { value: "5000" },
    });
    fireEvent.click(screen.getByText("Next"));

    // Step 3: future date
    await waitFor(() => screen.getByText(/By when/i));
    const futureDate = new Date();
    futureDate.setFullYear(futureDate.getFullYear() + 1);
    const dateInput = document.querySelector('input[type="date"]');
    fireEvent.change(dateInput, {
      target: { value: futureDate.toISOString().split("T")[0] },
    });
    fireEvent.click(screen.getByText("Next"));

    await waitFor(() => {
      expect(screen.getByText(/Review Your Goal/i)).toBeTruthy();
      expect(screen.getByText("Create Goal")).toBeTruthy();
    });
  });

  it("calls onSubmit with correct data when Create Goal is clicked", async () => {
    const onSubmit = vi.fn();
    render(<GoalCreationFlow {...defaultProps} onSubmit={onSubmit} />);

    // Step 1
    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: "Travel" },
    });
    fireEvent.click(screen.getByText("Next"));

    // Step 2
    await waitFor(() => screen.getByPlaceholderText("0.00"));
    fireEvent.change(screen.getByPlaceholderText("0.00"), {
      target: { value: "5000" },
    });
    fireEvent.click(screen.getByText("Next"));

    // Step 3
    await waitFor(() => screen.getByText(/By when/i));
    const futureDate = new Date();
    futureDate.setFullYear(futureDate.getFullYear() + 1);
    const dateValue = futureDate.toISOString().split("T")[0];
    fireEvent.change(document.querySelector('input[type="date"]'), {
      target: { value: dateValue },
    });
    fireEvent.click(screen.getByText("Next"));

    // Review
    await waitFor(() => screen.getByText("Create Goal"));
    fireEvent.click(screen.getByText("Create Goal"));

    expect(onSubmit).toHaveBeenCalledWith({
      goalName: "Travel",
      targetAmount: 5000,
      targetDate: dateValue,
    });
  });

  it("calls onCancel when cancel is clicked on review step", async () => {
    const onCancel = vi.fn();
    render(<GoalCreationFlow {...defaultProps} onCancel={onCancel} />);

    // Navigate to review
    fireEvent.change(screen.getByRole("combobox"), {
      target: { value: "Travel" },
    });
    fireEvent.click(screen.getByText("Next"));
    await waitFor(() => screen.getByPlaceholderText("0.00"));
    fireEvent.change(screen.getByPlaceholderText("0.00"), {
      target: { value: "5000" },
    });
    fireEvent.click(screen.getByText("Next"));
    await waitFor(() => screen.getByText(/By when/i));
    const futureDate = new Date();
    futureDate.setFullYear(futureDate.getFullYear() + 1);
    fireEvent.change(document.querySelector('input[type="date"]'), {
      target: { value: futureDate.toISOString().split("T")[0] },
    });
    fireEvent.click(screen.getByText("Next"));
    await waitFor(() => screen.getByText("Cancel"));
    fireEvent.click(screen.getByText("Cancel"));

    expect(onCancel).toHaveBeenCalled();
  });
});
