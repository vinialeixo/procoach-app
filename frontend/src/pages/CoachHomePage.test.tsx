import { ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { useAthletes } from "@/lib/athletes";
import { CoachHomePage } from "./CoachHomePage";

vi.mock("@/layouts/AppShell", () => ({
  AppShell: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}));

vi.mock("@/lib/athletes", async () => {
  const actual = await vi.importActual<typeof import("@/lib/athletes")>("@/lib/athletes");
  return {
    ...actual,
    useAthletes: vi.fn(),
  };
});

describe("CoachHomePage", () => {
  it("exibe o inbox de atenção para atletas com convite pendente ou sem acesso", () => {
    const soon = new Date(Date.now() + 12 * 60 * 60 * 1000).toISOString();
    vi.mocked(useAthletes).mockReturnValue({
      data: [
        {
          id: "ath-1",
          name: "Ana Costa",
          sex: "FEMALE",
          birthDate: "1995-01-01",
          heightCm: 165,
          weightKg: 60,
          goal: "HYPERTROPHY",
          status: "ACTIVE",
          createdAt: "2026-10-01T00:00:00Z",
          hasCredential: false,
          pendingInvite: { id: "inv-1", email: "ana@test.com", expiresAt: soon },
        },
        {
          id: "ath-2",
          name: "Bruno Lima",
          sex: "MALE",
          birthDate: "1992-05-10",
          heightCm: 180,
          weightKg: 82,
          goal: "CUTTING",
          status: "ACTIVE",
          createdAt: "2026-10-02T00:00:00Z",
          hasCredential: false,
          pendingInvite: null,
        },
        {
          id: "ath-3",
          name: "Carla Dias",
          sex: "FEMALE",
          birthDate: "1998-03-12",
          heightCm: 170,
          weightKg: 62,
          goal: "MAINTENANCE",
          status: "PAUSED",
          createdAt: "2026-10-03T00:00:00Z",
          hasCredential: true,
          pendingInvite: null,
        },
      ],
      isLoading: false,
      isError: false,
      refetch: vi.fn(),
    } as never);

    render(
      <MemoryRouter>
        <CoachHomePage />
      </MemoryRouter>
    );

    expect(screen.getByRole("region", { name: "Inbox de atenção" })).toBeInTheDocument();
    expect(screen.getByText(/Convite expira em/)).toBeInTheDocument();
    expect(screen.getByText("Sem acesso ao app")).toBeInTheDocument();
    expect(screen.getByText("Convidar")).toBeInTheDocument();
  });
});
