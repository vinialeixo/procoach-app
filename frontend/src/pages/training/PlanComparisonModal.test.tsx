import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { PlanComparisonModal } from "./PlanComparisonModal";
import { PlanComparison } from "@/lib/training";

const mockComparison: PlanComparison = {
  previous: {
    id: "plan-v1",
    athleteId: "ath-1",
    periodizationId: "per-1",
    version: 1,
    name: "Hipertrofia v1",
    description: null,
    status: "FINISHED",
    previousVersionId: null,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    deleted: false,
  },
  current: {
    id: "plan-v2",
    athleteId: "ath-1",
    periodizationId: "per-1",
    version: 2,
    name: "Hipertrofia v2",
    description: null,
    status: "ACTIVE",
    previousVersionId: "plan-v1",
    createdAt: "2026-02-01T00:00:00Z",
    updatedAt: "2026-02-01T00:00:00Z",
    deleted: false,
  },
  summary: {
    added: 1,
    removed: 1,
    changed: 1,
    unchanged: 2,
    previousExerciseCount: 4,
    currentExerciseCount: 4,
  },
  sessions: [
    {
      key: "ses-1",
      name: "Treino A",
      weekNumber: 1,
      dayOfWeek: 1,
      change: "CHANGED",
      previous: null,
      current: null,
      exerciseChanges: [
        {
          exerciseName: "Supino Reto",
          change: "CHANGED",
          previousSummary: "3x10 60kg",
          currentSummary: "4x8 70kg",
        },
      ],
    },
  ],
};

describe("PlanComparisonModal (Issue #39 / T-074)", () => {
  it("não renderiza quando open=false", () => {
    const { container } = render(
      <PlanComparisonModal
        open={false}
        comparison={mockComparison}
        onClose={vi.fn()}
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it("renderiza resumo de comparação entre versão anterior e corrente", async () => {
    const user = userEvent.setup();
    const handleClose = vi.fn();

    render(
      <PlanComparisonModal
        open={true}
        comparison={mockComparison}
        onClose={handleClose}
      />
    );

    expect(screen.getByRole("heading", { name: "Comparar versões do plano" })).toBeInTheDocument();
    expect(screen.getByText("Hipertrofia v1")).toBeInTheDocument();
    expect(screen.getByText("Hipertrofia v2")).toBeInTheDocument();

    // Contadores de resumo
    expect(screen.getByText(/1 nova\(s\) · 1 removida\(s\) · 1 alterada\(s\) · 2 sem mudanças/)).toBeInTheDocument();

    // Sessão alterada
    expect(screen.getByText("Treino A")).toBeInTheDocument();
    expect(screen.getByText("Supino Reto")).toBeInTheDocument();
    expect(screen.getByText("Antes: 3x10 60kg")).toBeInTheDocument();
    expect(screen.getByText("Depois: 4x8 70kg")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Fechar" }));
    expect(handleClose).toHaveBeenCalled();
  });
});
