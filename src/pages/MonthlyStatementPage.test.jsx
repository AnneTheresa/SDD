import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MonthlyStatementPage } from "./MonthlyStatementPage";

const useMonthlyStatementMock = vi.fn();

vi.mock("../hooks/useGroup3", () => ({
  useMonthlyStatement: (filters) => useMonthlyStatementMock(filters),
}));

vi.mock("../hooks/useListCustomerAccounts", () => ({
  useListCustomerAccounts: () => ({
    isLoading: false,
    error: null,
    data: [{ accountId: 12, accountType: "SAVINGS", balance: "500.00" }],
  }),
}));

vi.mock("../auth/AuthContext", () => ({
  useAuth: () => ({ authState: { customerId: 1 } }),
}));

vi.mock("../components/AccountSwitcher", () => ({
  AccountSwitcher: () => null,
}));

function renderMonthlyStatementPage(path = "/accounts/12/statements") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route
          path="/accounts/:accountId/statements"
          element={<MonthlyStatementPage />}
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe("MonthlyStatementPage", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    useMonthlyStatementMock.mockReset();
    useMonthlyStatementMock.mockReturnValue({
      data: null,
      error: null,
      isLoading: false,
      isFetching: false,
    });
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("defaults to previous month on first load for a normal month", () => {
    vi.setSystemTime(new Date("2026-05-15T10:00:00.000Z"));

    renderMonthlyStatementPage();

    expect(screen.getByLabelText("Statement Year")).toHaveValue(2026);
    expect(screen.getByLabelText("Statement Month")).toHaveValue("04");
    expect(useMonthlyStatementMock).toHaveBeenCalledWith(
      expect.objectContaining({
        accountId: "12",
        period: "2026-04",
      }),
    );
  });

  it("defaults to December of the previous year in January", () => {
    vi.setSystemTime(new Date("2026-01-15T10:00:00.000Z"));

    renderMonthlyStatementPage();

    expect(screen.getByLabelText("Statement Year")).toHaveValue(2025);
    expect(screen.getByLabelText("Statement Month")).toHaveValue("12");
    expect(useMonthlyStatementMock).toHaveBeenCalledWith(
      expect.objectContaining({
        accountId: "12",
        period: "2025-12",
      }),
    );
  });
});
