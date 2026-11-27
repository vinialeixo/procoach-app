import { ReactNode } from "react";
import { render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/lib/api-client";
import { getAthleteId } from "@/lib/auth";
import { TrainingPlan, TrainingSession } from "@/lib/training";
import { AthleteTrainingPage } from "./AthleteTrainingPage";

vi.mock("@/lib/api-client", () => ({
  apiClient: {
    get: vi.fn(),
  },
}));

vi.mock("@/lib/auth", () => ({
  getAthleteId: vi.fn(),
}));

vi.mock("@/layouts/AthleteShell", () => ({
  AthleteShell: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}));

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <AthleteTrainingPage />
    </QueryClientProvider>
  );
}

const ACTIVE_PLAN: TrainingPlan = {
  id: "plan-2",
  athleteId: "ath-1",
  periodizationId: "per-1",
  name: "Bloco Força",
  description: "Ciclo principal",
  version: 2,
  previousVersionId: "plan-1",
  status: "ACTIVE",
  createdAt: "2026-11-10T10:00:00Z",
  updatedAt: "2026-11-10T10:00:00Z",
  deleted: false,
};

const PREVIOUS_PLAN: TrainingPlan = {
  id: "plan-1",
  athleteId: "ath-1",
  periodizationId: "per-1",
  name: "Bloco Base",
  description: null,
  version: 1,
  previousVersionId: null,
  status: "FINISHED",
  createdAt: "2026-10-10T10:00:00Z",
  updatedAt: "2026-10-20T10:00:00Z",
  deleted: false,
};

const ACTIVE_SESSIONS: TrainingSession[] = [
  {
    id: "sess-1",
    periodizationId: "per-1",
    planId: "plan-2",
    athleteId: "ath-1",
    name: "Treino A — Inferior",
    description: null,
    weekNumber: 1,
    dayOfWeek: 1,
    status: "ACTIVE",
    deleted: false,
    exercises: [
      {
        id: "ex-1",
        exerciseId: "lib-1",
        exerciseName: "Agachamento livre",
        customExercise: false,
        orderIndex: 1,
        sets: 4,
        repetitions: 6,
        load: 100,
        intensityType: "RPE",
        intensityValue: 8,
        restSeconds: 180,
        notes: null,
        exerciseVersion: 1,
        newerVersionAvailable: false,
        currentExerciseId: "lib-1",
      },
      {
        id: "ex-2",
        exerciseId: "lib-2",
        exerciseName: "Leg press 45",
        customExercise: false,
        orderIndex: 2,
        sets: 3,
        repetitions: 10,
        load: 180,
        intensityType: "RIR",
        intensityValue: 2,
        restSeconds: 120,
        notes: null,
        exerciseVersion: 1,
        newerVersionAvailable: false,
        currentExerciseId: "lib-2",
      },
      {
        id: "ex-3",
        exerciseId: "lib-3",
        exerciseName: "Levantamento terra",
        customExercise: false,
        orderIndex: 3,
        sets: 3,
        repetitions: 5,
        load: 120,
        intensityType: "PERCENT_1RM",
        intensityValue: 80,
        restSeconds: 180,
        notes: null,
        exerciseVersion: 1,
        newerVersionAvailable: false,
        currentExerciseId: "lib-3",
      },
      {
        id: "ex-4",
        exerciseId: "lib-4",
        exerciseName: "Panturrilha em pé",
        customExercise: false,
        orderIndex: 4,
        sets: 4,
        repetitions: 15,
        load: 60,
        intensityType: "RPE",
        intensityValue: null,
        restSeconds: 60,
        notes: null,
        exerciseVersion: 1,
        newerVersionAvailable: false,
        currentExerciseId: "lib-4",
      },
    ],
  },
];

const PREVIOUS_SESSIONS: TrainingSession[] = [
  {
    id: "sess-old-1",
    periodizationId: "per-1",
    planId: "plan-1",
    athleteId: "ath-1",
    name: "Treino Base A",
    description: null,
    weekNumber: 1,
    dayOfWeek: 1,
    status: "FINISHED",
    deleted: false,
    exercises: [
      {
        id: "ex-old-1",
        exerciseId: "lib-1",
        exerciseName: "Agachamento guiado",
        customExercise: false,
        orderIndex: 1,
        sets: 3,
        repetitions: 10,
        load: 70,
        intensityType: "RPE",
        intensityValue: 7,
        restSeconds: 120,
        notes: null,
        exerciseVersion: 1,
        newerVersionAvailable: false,
        currentExerciseId: "lib-1",
      },
    ],
  },
];

describe("AthleteTrainingPage (Issue #210)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getAthleteId).mockReturnValue("ath-1");
  });

  it("renders prescribed intensity (RPE, RIR, %1RM, and type-only fallback) in active session and version history", async () => {
    vi.mocked(apiClient.get).mockImplementation(
      async (url: string, config?: { params?: unknown }) => {
        const planId = (config?.params as { planId?: string } | undefined)?.planId;
        if (url.includes("/plans")) {
          return { data: [ACTIVE_PLAN, PREVIOUS_PLAN] };
        }
        if (planId === "plan-2") {
          return { data: ACTIVE_SESSIONS };
        }
        if (planId === "plan-1") {
          return { data: PREVIOUS_SESSIONS };
        }
        return { data: [] };
      }
    );

    renderPage();

    await waitFor(() => {
      expect(screen.getByText("Treino A — Inferior")).toBeInTheDocument();
    });

    expect(screen.getByText("Intensidade: RPE 8")).toBeInTheDocument();
    expect(screen.getByText("Intensidade: RIR 2")).toBeInTheDocument();
    expect(screen.getByText("Intensidade: %1RM 80%")).toBeInTheDocument();
    expect(screen.getByText("Intensidade: RPE")).toBeInTheDocument();
    expect(screen.queryByText(/null|undefined/)).toBeNull();

    // Histórico de versões também exibe intensidade resumida
    await waitFor(() => {
      expect(screen.getByText("RPE 7")).toBeInTheDocument();
    });
  });
});
