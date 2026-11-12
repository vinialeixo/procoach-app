import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { PlanVersionModal } from "./PlanVersionModal";
import { TrainingPlan } from "@/lib/training";

const mockPlan: TrainingPlan = {
  id: "plan-1",
  athleteId: "ath-1",
  periodizationId: "per-1",
  version: 2,
  name: "Hipertrofia Fase 2",
  description: "Foco em sobrecarga",
  status: "ACTIVE",
  previousVersionId: "plan-0",
  createdAt: "2026-01-01T00:00:00Z",
  updatedAt: "2026-01-01T00:00:00Z",
  deleted: false,
};

describe("PlanVersionModal (Issue #39 / T-074)", () => {
  it("não renderiza quando open=false", () => {
    const { container } = render(
      <PlanVersionModal
        open={false}
        selectedPlan={mockPlan}
        sessionsCount={3}
        isPending={false}
        error={null}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it("renderiza campos com valores padrão da versão anterior e dispara submit", async () => {
    const user = userEvent.setup();
    const handleSubmit = vi.fn((e) => e.preventDefault());
    const handleClose = vi.fn();

    render(
      <PlanVersionModal
        open={true}
        selectedPlan={mockPlan}
        sessionsCount={2}
        isPending={false}
        error={null}
        onClose={handleClose}
        onSubmit={handleSubmit}
      />
    );

    expect(screen.getByRole("heading", { name: "Criar nova versão do plano" })).toBeInTheDocument();
    expect(
      screen.getByText((content) => content.includes("A versão v2 permanece intacta"))
    ).toBeInTheDocument();

    const nameInput = screen.getByLabelText("Nome da nova versão");
    expect(nameInput).toHaveValue("Hipertrofia Fase 2");

    const descInput = screen.getByLabelText("Descrição da nova versão");
    expect(descInput).toHaveValue("Foco em sobrecarga");

    await user.click(screen.getByRole("button", { name: "Criar versão" }));
    expect(handleSubmit).toHaveBeenCalled();
  });

  it("exibe alerta quando há erro", () => {
    render(
      <PlanVersionModal
        open={true}
        selectedPlan={mockPlan}
        sessionsCount={1}
        isPending={false}
        error={new Error("Erro de servidor")}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />
    );

    expect(
      screen.getByText("Não foi possível criar a nova versão do plano.")
    ).toBeInTheDocument();
  });
});
