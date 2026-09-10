import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LoginPage } from "./LoginPage";
import { RegisterPage } from "./RegisterPage";

const mockNavigate = vi.fn();
const mockCompleteLogin = vi.fn();
const mockRememberCustomerId = vi.fn();
const loginUser = vi.fn();
const registerUser = vi.fn();
const createCustomer = vi.fn();
const mutationState = { isPending: false };

vi.mock("@tanstack/react-query", () => ({
  useMutation: ({ mutationFn }) => ({
    isPending: mutationState.isPending,
    mutateAsync: mutationFn,
  }),
}));

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock("../api/auth", () => ({
  loginUser: (...args) => loginUser(...args),
  registerUser: (...args) => registerUser(...args),
}));

vi.mock("../api/customers", () => ({
  createCustomer: (...args) => createCustomer(...args),
}));

vi.mock("../auth/AuthContext", () => ({
  useAuth: () => ({
    completeLogin: mockCompleteLogin,
    rememberCustomerId: mockRememberCustomerId,
  }),
}));

function renderLoginPage(initialEntry = "/login") {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

function renderRegisterPage() {
  return render(
    <MemoryRouter initialEntries={["/register"]}>
      <Routes>
        <Route path="/register" element={<RegisterPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

function submitCurrentForm() {
  fireEvent.submit(document.querySelector("form"));
}

function goToRegisterDetailsStep() {
  fireEvent.click(screen.getByRole("button", { name: "Continue" }));
}

describe("auth pages", () => {
  beforeEach(() => {
    mutationState.isPending = false;
    mockNavigate.mockReset();
    mockCompleteLogin.mockReset();
    mockRememberCustomerId.mockReset();
    loginUser.mockReset();
    registerUser.mockReset();
    createCustomer.mockReset();
  });

  it("validates login form fields before submitting", async () => {
    renderLoginPage();

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "not-an-email" },
    });
    submitCurrentForm();

    expect(
      await screen.findByText("Enter a valid email address."),
    ).toBeInTheDocument();
    expect(loginUser).not.toHaveBeenCalled();

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "user@example.com" },
    });
    submitCurrentForm();

    expect(
      await screen.findByText("Password is required."),
    ).toBeInTheDocument();
    expect(loginUser).not.toHaveBeenCalled();
  });

  it("disables login submit while the mutation is pending", () => {
    mutationState.isPending = true;

    renderLoginPage();

    expect(screen.getByRole("button", { name: "Sign In" })).toBeDisabled();
  });

  it("submits login and navigates to the intended route", async () => {
    loginUser.mockResolvedValue({ accessToken: "token" });
    mockCompleteLogin.mockReturnValue({ roles: [], customerId: "44" });

    renderLoginPage({
      pathname: "/login",
      state: { from: { pathname: "/accounts/12" } },
    });

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "user@example.com" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "secret" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Sign In" }));

    await waitFor(() => {
      expect(loginUser).toHaveBeenCalledWith({
        username: "user@example.com",
        password: "secret",
      });
      expect(mockCompleteLogin).toHaveBeenCalledWith(
        { accessToken: "token" },
        "user@example.com",
      );
      expect(mockNavigate).toHaveBeenCalledWith("/accounts/12", {
        replace: true,
      });
    });
  });

  it("shows register validation errors before submitting", async () => {
    renderRegisterPage();
    goToRegisterDetailsStep();

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "bad-email" },
    });
    submitCurrentForm();
    expect(
      await screen.findByText("Enter a valid email address."),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "user@example.com" },
    });
    submitCurrentForm();
    expect(
      await screen.findByText("Password is required."),
    ).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "secret" },
    });
    submitCurrentForm();
    expect(await screen.findByText("Name is required.")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText("Full Name"), {
      target: { value: "New Customer" },
    });
    submitCurrentForm();
    expect(await screen.findByText("Address is required.")).toBeInTheDocument();

    expect(registerUser).not.toHaveBeenCalled();
  });

  it("disables register submit while the mutation is pending", () => {
    mutationState.isPending = true;

    renderRegisterPage();
    goToRegisterDetailsStep();

    expect(
      screen.getByRole("button", { name: "Create Account" }),
    ).toBeDisabled();
  });

  it("submits registration, creates the customer, and navigates to the customer page", async () => {
    registerUser.mockResolvedValue({});
    loginUser.mockResolvedValue({ accessToken: "token" });
    createCustomer.mockResolvedValue({ customerId: "88" });

    renderRegisterPage();
    goToRegisterDetailsStep();

    fireEvent.change(screen.getByLabelText("Email"), {
      target: { value: "user@example.com" },
    });
    fireEvent.change(screen.getByLabelText("Password"), {
      target: { value: "secret" },
    });
    fireEvent.change(screen.getByLabelText("Full Name"), {
      target: { value: "New Customer" },
    });
    fireEvent.change(screen.getByLabelText("Address"), {
      target: { value: "1 Main St" },
    });
    fireEvent.change(screen.getByLabelText(/Date of Birth/i), {
      target: { value: "1998-10-26" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Create Account" }));

    await waitFor(() => {
      expect(registerUser).toHaveBeenCalledWith({
        username: "user@example.com",
        password: "secret",
      });
      expect(loginUser).toHaveBeenCalledWith({
        username: "user@example.com",
        password: "secret",
      });
      expect(createCustomer).toHaveBeenCalledWith(
        {
          name: "New Customer",
          address: "1 Main St",
          type: "PERSON",
          dateOfBirth: "1998-10-26",
          kycVerified: true,
        },
        "token",
      );
      expect(mockCompleteLogin).toHaveBeenCalledWith(
        { accessToken: "token" },
        "user@example.com",
      );
      expect(mockRememberCustomerId).toHaveBeenCalledWith("88");
      expect(mockNavigate).toHaveBeenCalledWith("/customer/88/accounts", {
        state: {
          successMessage: "Account created successfully! Welcome to Voltio.",
        },
      });
    });
  });
});
