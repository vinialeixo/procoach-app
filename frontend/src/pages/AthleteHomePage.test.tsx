import { ReactNode } from "react";
import { MemoryRouter } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { getAthleteId } from "@/lib/auth";
import { useCurrentAthleteDiet } from "@/lib/diets";
import { useAthleteNotifications } from "@/lib/notifications";
import { useAthleteTrainingSessions, useTrainingPlans } from "@/lib/training";
import { AthleteHomePage } from "./AthleteHomePage";

vi.mock("@/layouts/AthleteShell", () => ({
  AthleteShell: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}));

vi.mock("@/lib/auth", () => ({
  getAthleteId: vi.fn(),
}));

vi.mock("@/lib/diets", async () => {
  const actual = await vi.importActual<typeof import("@/lib/diets")>("@/lib/diets");
  return {
    ...actual,
    useCurrentAthleteDiet: vi.fn(),
  };
});

vi.mock("@/lib/training", async () => {
  const actual = await vi.importActual<typeof import("@/lib/training")>("@/lib/training");
  return {
    ...actual,
    useTrainingPlans: vi.fn(),
    useAthleteTrainingSessions: vi.fn(),
  };
});

vi.mock("@/lib/notifications", () => ({
  useAthleteNotifications: vi.fn(),
}));

describe("AthleteHomePage", () => {
  beforeEach(() => {
    vi.mocked(getAthleteId).mockReturnValue("ath-1");
    vi.mocked(useTrainingPlans).mockReturnValue({
      data: [{ id: "plan-1", version: 2, status: "ACTIVE" }],
      isPending: false,
    } as never);
    const jsDay = new Date().getDay();
    const todayDay = jsDay === 0 ? 7 : jsDay;
    vi.mocked(useAthleteTrainingSessions).mockReturnValue({
      data: [
        {
          id: "sess-1",
          name: "Treino A — Superiores",
          dayOfWeek: todayDay,
          weekNumber: 1,
          exercises: [{ id: "ex-1" }, { id: "ex-2" }],
        },
      ],
      isPending: false,
    } as never);
    vi.mocked(useCurrentAthleteDiet).mockReturnValue({
      data: {
        id: "diet-1",
        objective: "HYPERTROPHY",
        version: 1,
        macros: {
          calories: 2400,
          protein: 180,
          carbohydrates: 260,
          fat: 70,
        },
        totals: {
          calories: 2400,
          protein: 180,
          carbohydrates: 260,
          fat: 70,
        },
        meals: [
          {
            id: "meal-1",
            name: "Almoço",
            order: 1,
            time: "23:59",
            items: [],
          },
        ],
      },
      isPending: false,
    } as never);
    vi.mocked(useAthleteNotifications).mockReturnValue({
      data: [{ id: "n-1", readAt: null }],
    } as never);
  });

  it("exibe o treino de hoje, próxima refeição e contagem de avisos não lidos", () => {
    render(
      <MemoryRouter>
        <AthleteHomePage />
      </MemoryRouter>
    );

    expect(screen.getByText("Treino A — Superiores")).toBeInTheDocument();
    expect(screen.getByText(/2 exercício\(s\) · v2/)).toBeInTheDocument();
    expect(screen.getByText("Almoço")).toBeInTheDocument();
    expect(screen.getByText(/2400 kcal · P 180g · C 260g · G 70g/)).toBeInTheDocument();
    expect(screen.getByText("1")).toBeInTheDocument();
  });
});
