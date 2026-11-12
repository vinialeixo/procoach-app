import { FormEvent } from "react";
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

  it("apresenta opções de estratégia de criação com duplicação pré-selecionada (Issue #43)", () => {
    render(
      <PlanVersionModal
        open={true}
        selectedPlan={mockPlan}
        sessionsCount={2}
        isPending={false}
        error={null}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />
    );

    const duplicateRadio = screen.getByLabelText(
      /Duplicar sessões e prescrições da versão atual \(Recomendado\)/
    ) as HTMLInputElement;
    const blankRadio = screen.getByLabelText(
      /Começar em branco \(nova versão sem sessões copiadas\)/
    ) as HTMLInputElement;

    expect(duplicateRadio).toBeInTheDocument();
    expect(duplicateRadio.checked).toBe(true);
    expect(blankRadio).toBeInTheDocument();
    expect(blankRadio.checked).toBe(false);
  });

  it("permite selecionar opção de começar em branco e submeter (Issue #43)", async () => {
    const user = userEvent.setup();
    let submittedStrategy = "";
    const handleSubmit = vi.fn((e: FormEvent<HTMLFormElement>) => {
      e.preventDefault();
      const form = new FormData(e.currentTarget);
      submittedStrategy = String(form.get("strategy"));
    });

    render(
      <PlanVersionModal
        open={true}
        selectedPlan={mockPlan}
        sessionsCount={2}
        isPending={false}
        error={null}
        onClose={vi.fn()}
        onSubmit={handleSubmit}
      />
    );

    const blankRadio = screen.getByLabelText(
      /Começar em branco \(nova versão sem sessões copiadas\)/
    );
    await user.click(blankRadio);

    await user.click(screen.getByRole("button", { name: "Criar versão" }));

    expect(handleSubmit).toHaveBeenCalled();
    expect(submittedStrategy).toBe("blank");
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
