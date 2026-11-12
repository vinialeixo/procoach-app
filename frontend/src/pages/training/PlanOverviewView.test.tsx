import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { PlanOverviewView } from "./PlanOverviewView";
import { Periodization, TrainingPlan, TrainingSession } from "@/lib/training";

const mockPeriodization: Periodization = {
  id: "per-1",
  athleteId: "ath-1",
  name: "Periodização 2026",
  description: "Foco hipertrofia",
  startDate: "2026-01-01",
  endDate: "2026-06-30",
  status: "ACTIVE",
  deleted: false,
};

const mockPlan: TrainingPlan = {
  id: "plan-1",
  athleteId: "ath-1",
  periodizationId: "per-1",
  name: "Plano v1",
  description: "Treinos A/B",
  version: 1,
  previousVersionId: null,
  status: "ACTIVE",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
  deleted: false,
};

const mockSessions: TrainingSession[] = [
  {
    id: "ses-1",
    athleteId: "ath-1",
    periodizationId: "per-1",
    planId: "plan-1",
    name: "Treino A - Superiores",
    description: null,
    weekNumber: 1,
    dayOfWeek: 1,
    status: "ACTIVE",
    exercises: [],
    deleted: false,
  },
  {
    id: "ses-2",
    athleteId: "ath-1",
    periodizationId: "per-1",
    planId: "plan-1",
    name: "Treino B - Inferiores (Finalizado)",
    description: null,
    weekNumber: 1,
    dayOfWeek: 3,
    status: "FINISHED",
    exercises: [],
    deleted: false,
  },
];

describe("PlanOverviewView (Issue #42)", () => {
  it("renderiza atalho '+ Adicionar exercício' em cada sessão do cronograma", () => {
    render(
      <PlanOverviewView
        athleteId="ath-1"
        periodizationId="per-1"
        planId="plan-1"
        sessionId="ses-1"
        selectedPeriodization={mockPeriodization}
        plans={[mockPlan]}
        selectedPlan={mockPlan}
        previousPlan={undefined}
        sessions={mockSessions}
        planComparison={null}
        plansLoading={false}
        plansError={false}
        createPlanPending={false}
        createPlanError={null}
        transitionPlanPending={false}
        transitionPlanError={null}
        createSessionPending={false}
        createSessionError={null}
        onCreatePlan={vi.fn()}
        onCreateSession={vi.fn()}
        onSelectSession={vi.fn()}
        onOpenVersioning={vi.fn()}
        onOpenComparing={vi.fn()}
        onTransitionPlan={vi.fn()}
      />
    );

    const shortcutButtons = screen.getAllByRole("button", { name: "+ Adicionar exercício" });
    expect(shortcutButtons).toHaveLength(2);
  });

  it("chama onAddExerciseShortcut ao clicar no atalho da sessão ativa", async () => {
    const user = userEvent.setup();
    const handleShortcut = vi.fn();
    const handleSelect = vi.fn();

    render(
      <PlanOverviewView
        athleteId="ath-1"
        periodizationId="per-1"
        planId="plan-1"
        sessionId=""
        selectedPeriodization={mockPeriodization}
        plans={[mockPlan]}
        selectedPlan={mockPlan}
        previousPlan={undefined}
        sessions={mockSessions}
        planComparison={null}
        plansLoading={false}
        plansError={false}
        createPlanPending={false}
        createPlanError={null}
        transitionPlanPending={false}
        transitionPlanError={null}
        createSessionPending={false}
        createSessionError={null}
        onCreatePlan={vi.fn()}
        onCreateSession={vi.fn()}
        onSelectSession={handleSelect}
        onAddExerciseShortcut={handleShortcut}
        onOpenVersioning={vi.fn()}
        onOpenComparing={vi.fn()}
        onTransitionPlan={vi.fn()}
      />
    );

    const [firstShortcut] = screen.getAllByRole("button", { name: "+ Adicionar exercício" });
    await user.click(firstShortcut);

    expect(handleShortcut).toHaveBeenCalledWith("ses-1");
  });

  it("desabilita botão de atalho quando a sessão está finalizada (FINISHED)", () => {
    render(
      <PlanOverviewView
        athleteId="ath-1"
        periodizationId="per-1"
        planId="plan-1"
        sessionId="ses-1"
        selectedPeriodization={mockPeriodization}
        plans={[mockPlan]}
        selectedPlan={mockPlan}
        previousPlan={undefined}
        sessions={mockSessions}
        planComparison={null}
        plansLoading={false}
        plansError={false}
        createPlanPending={false}
        createPlanError={null}
        transitionPlanPending={false}
        transitionPlanError={null}
        createSessionPending={false}
        createSessionError={null}
        onCreatePlan={vi.fn()}
        onCreateSession={vi.fn()}
        onSelectSession={vi.fn()}
        onOpenVersioning={vi.fn()}
        onOpenComparing={vi.fn()}
        onTransitionPlan={vi.fn()}
      />
    );

    const shortcutButtons = screen.getAllByRole("button", { name: "+ Adicionar exercício" });
    expect(shortcutButtons[0]).toBeEnabled();
    expect(shortcutButtons[1]).toBeDisabled();
  });
});
