import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AthleteCreatePage } from "./AthleteCreatePage";
import { apiClient } from "@/lib/api-client";

vi.mock("@/lib/api-client", () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

function renderWithClient() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={["/coach/athletes/new"]}>
        <Routes>
          <Route path="/coach/athletes/new" element={<AthleteCreatePage />} />
          <Route path="/coach/athletes/:id" element={<h1>Ficha do atleta</h1>} />
          <Route path="/coach/athletes" element={<h1>Listagem de atletas</h1>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe("AthleteCreatePage", () => {
  it("renders form with all fields including optional email", () => {
    renderWithClient();

    expect(screen.getByRole("heading", { name: "Novo atleta" })).toBeInTheDocument();
    expect(screen.getByLabelText(/^Nome/)).toBeInTheDocument();
    expect(screen.getByLabelText(/E-mail \(opcional\)/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/^Sexo/)).toBeInTheDocument();
    expect(screen.getByLabelText(/Data de nascimento/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Altura/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Peso/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Objetivo/i)).toBeInTheDocument();
  });

  it("successfully creates athlete and navigates to detail page", async () => {
    const user = userEvent.setup();
    vi.mocked(apiClient.post).mockResolvedValueOnce({
      data: {
        id: "ath-new-1",
        name: "Carlos Silva",
        email: "carlos@exemplo.com",
        sex: "MALE",
        birthDate: "1990-01-15",
        height: 180,
        weight: 80,
        goal: "HYPERTROPHY",
        status: "ACTIVE",
        createdAt: "2026-11-12T00:00:00Z",
        hasCredential: false,
        pendingInvite: null,
      },
    });

    renderWithClient();

    await user.type(screen.getByLabelText(/^Nome/), "Carlos Silva");
    await user.type(screen.getByLabelText(/E-mail \(opcional\)/i), "carlos@exemplo.com");
    await user.selectOptions(screen.getByLabelText(/^Sexo/), "MALE");
    await user.type(screen.getByLabelText(/Data de nascimento/i), "1990-01-15");
    await user.type(screen.getByLabelText(/Altura/i), "180");
    await user.type(screen.getByLabelText(/Peso/i), "80");
    await user.selectOptions(screen.getByLabelText(/Objetivo/i), "HYPERTROPHY");

    await user.click(screen.getByRole("button", { name: "Cadastrar atleta" }));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith("/athletes", {
        name: "Carlos Silva",
        email: "carlos@exemplo.com",
        sex: "MALE",
        birthDate: "1990-01-15",
        height: 180,
        weight: 80,
        goal: "HYPERTROPHY",
      });
      expect(screen.getByRole("heading", { name: "Ficha do atleta" })).toBeInTheDocument();
    });
  });

  it("handles 409 EMAIL_ALREADY_REGISTERED with actionable guidance and link", async () => {
    const user = userEvent.setup();
    // Envelope de erro real do módulo athlete (AthleteExceptionHandler):
    // {timestamp, status, code, error} — sem athleteId.
    vi.mocked(apiClient.post).mockRejectedValueOnce({
      response: {
        status: 409,
        data: {
          timestamp: "2026-11-12T00:00:00Z",
          status: 409,
          code: "EMAIL_ALREADY_REGISTERED",
          error: "O e-mail carlos@exemplo.com já está cadastrado na plataforma.",
        },
      },
    });

    renderWithClient();

    await user.type(screen.getByLabelText(/^Nome/), "Carlos Silva");
    await user.type(screen.getByLabelText(/E-mail \(opcional\)/i), "carlos@exemplo.com");
    await user.selectOptions(screen.getByLabelText(/^Sexo/), "MALE");
    await user.type(screen.getByLabelText(/Data de nascimento/i), "1990-01-15");
    await user.type(screen.getByLabelText(/Altura/i), "180");
    await user.type(screen.getByLabelText(/Peso/i), "80");
    await user.selectOptions(screen.getByLabelText(/Objetivo/i), "HYPERTROPHY");

    await user.click(screen.getByRole("button", { name: "Cadastrar atleta" }));

    await waitFor(() => {
      expect(
        screen.getByText(/Esta pessoa já tem conta no sistema/i)
      ).toBeInTheDocument();
      expect(
        screen.getByRole("link", { name: "Ver atletas cadastrados" })
      ).toHaveAttribute("href", "/coach/athletes");
    });
  });
});
