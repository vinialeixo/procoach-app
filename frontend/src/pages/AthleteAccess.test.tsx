import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AthleteListPage } from "./AthleteListPage";
import { AthleteDetailPage } from "./AthleteDetailPage";
import { apiClient } from "@/lib/api-client";

vi.mock("@/lib/api-client", () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

function createClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
}

beforeEach(() => {
  vi.clearAllMocks();
  sessionStorage.clear();
});

describe("Athlete Access & Invites (Issue #98 / T-082)", () => {
  it("displays access badges in AthleteListPage for each athlete access state", async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: [
        {
          id: "ath-1",
          name: "Atleta com Acesso",
          sex: "MALE",
          birthDate: "1995-05-10",
          height: 175,
          weight: 75,
          goal: "HYPERTROPHY",
          status: "ACTIVE",
          createdAt: "2026-11-10T10:00:00Z",
          hasCredential: true,
          pendingInvite: null,
        },
        {
          id: "ath-2",
          name: "Atleta com Convite",
          sex: "FEMALE",
          birthDate: "1998-08-20",
          height: 165,
          weight: 60,
          goal: "CUTTING",
          status: "ACTIVE",
          createdAt: "2026-11-10T11:00:00Z",
          hasCredential: false,
          pendingInvite: {
            id: "inv-2",
            email: "convite@atleta.com",
            expiresAt: "2026-11-19T10:00:00Z",
          },
        },
        {
          id: "ath-3",
          name: "Atleta sem Acesso",
          sex: "MALE",
          birthDate: "2000-01-01",
          height: 180,
          weight: 85,
          goal: "BULKING",
          status: "ACTIVE",
          createdAt: "2026-11-10T12:00:00Z",
          hasCredential: false,
          pendingInvite: null,
        },
      ],
    });

    render(
      <QueryClientProvider client={createClient()}>
        <MemoryRouter initialEntries={["/coach/athletes"]}>
          <Routes>
            <Route path="/coach/athletes" element={<AthleteListPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText("Atleta com Acesso")).toBeInTheDocument();
      expect(screen.getByText("Atleta com Convite")).toBeInTheDocument();
      expect(screen.getByText("Atleta sem Acesso")).toBeInTheDocument();
    });

    // Coluna Acesso e seus badges
    expect(screen.getByText("Tem acesso")).toBeInTheDocument();
    expect(screen.getByText("Convite pendente")).toBeInTheDocument();
    expect(screen.getByText("Sem acesso")).toBeInTheDocument();
  });

  it("displays access status and pending invite section on AthleteDetailPage", async () => {
    const user = userEvent.setup();
    window.confirm = vi.fn(() => true);

    // Mock athlete query
    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === "/athletes/ath-pending") {
        return Promise.resolve({
          data: {
            id: "ath-pending",
            name: "Lucas Pereira",
            email: "lucas@exemplo.com",
            sex: "MALE",
            birthDate: "1992-04-12",
            height: 178,
            weight: 76,
            goal: "MAINTENANCE",
            status: "ACTIVE",
            createdAt: "2026-11-10T00:00:00Z",
            hasCredential: false,
            pendingInvite: {
              id: "inv-pending-1",
              email: "lucas@exemplo.com",
              expiresAt: "2026-11-18T10:00:00Z",
            },
          },
        });
      }
      if (url === "/athletes/ath-pending/invites") {
        return Promise.resolve({
          data: [
            {
              id: "inv-pending-1",
              athleteId: "ath-pending",
              email: "lucas@exemplo.com",
              expiresAt: "2026-11-18T10:00:00Z",
              status: "PENDING",
              invitePath: "/convite/tok-123",
            },
          ],
        });
      }
      if (url.includes("/assessments")) {
        return Promise.resolve({ data: [] });
      }
      if (url.includes("/diets")) {
        return Promise.resolve({ data: [] });
      }
      return Promise.reject(new Error(`Not found: ${url}`));
    });

    vi.mocked(apiClient.post).mockResolvedValue({ data: {} });

    render(
      <QueryClientProvider client={createClient()}>
        <MemoryRouter initialEntries={["/coach/athletes/ath-pending"]}>
          <Routes>
            <Route path="/coach/athletes/:id" element={<AthleteDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Header badge
    await waitFor(() => {
      expect(screen.getByText("Lucas Pereira")).toBeInTheDocument();
      expect(screen.getAllByText("Convite pendente").length).toBeGreaterThan(0);
    });

    // Seção de convites
    expect(screen.getByRole("heading", { name: "Acesso ao aplicativo" })).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.getByText(/Convite pendente: lucas@exemplo.com/)).toBeInTheDocument();
    });

    // Copiar link
    const copyButton = screen.getByRole("button", { name: "Copiar link" });
    await user.click(copyButton);
    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Link copiado!" })).toBeInTheDocument();
    });

    // Revogar convite
    const revokeButton = screen.getByRole("button", { name: "Revogar convite" });
    await user.click(revokeButton);

    await waitFor(() => {
      expect(window.confirm).toHaveBeenCalled();
      expect(apiClient.post).toHaveBeenCalledWith(
        "/athletes/ath-pending/invites/inv-pending-1/revoke"
      );
    });
  });

  it("disables 'Convidar para o app' button when athlete hasCredential is true", async () => {
    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === "/athletes/ath-has-access") {
        return Promise.resolve({
          data: {
            id: "ath-has-access",
            name: "Marina Costa",
            sex: "FEMALE",
            birthDate: "1994-06-15",
            height: 168,
            weight: 62,
            goal: "HYPERTROPHY",
            status: "ACTIVE",
            createdAt: "2026-11-10T00:00:00Z",
            hasCredential: true,
            pendingInvite: null,
          },
        });
      }
      if (url === "/athletes/ath-has-access/invites") {
        return Promise.resolve({ data: [] });
      }
      return Promise.resolve({ data: [] });
    });

    render(
      <QueryClientProvider client={createClient()}>
        <MemoryRouter initialEntries={["/coach/athletes/ath-has-access"]}>
          <Routes>
            <Route path="/coach/athletes/:id" element={<AthleteDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText("Marina Costa")).toBeInTheDocument();
      expect(screen.getAllByText("Tem acesso").length).toBeGreaterThan(0);
    });

    const inviteButton = screen.getByRole("button", { name: "Convidar para o app" });
    expect(inviteButton).toBeDisabled();
    expect(
      screen.getByText(/Atleta possui credencial ativa e acesso liberado ao aplicativo./)
    ).toBeInTheDocument();
  });
});
