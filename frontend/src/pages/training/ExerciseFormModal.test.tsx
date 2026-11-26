import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { Exercise } from "@/lib/training";
import { ExerciseFormModal } from "./ExerciseFormModal";

describe("ExerciseFormModal (#193)", () => {
  it("valida campos obrigatórios e cria novo exercício com listas e referências", async () => {
    const user = userEvent.setup();
    const onSave = vi.fn().mockResolvedValue(undefined);

    render(
      <ExerciseFormModal
        open={true}
        exercise={null}
        pending={false}
        error={null}
        onClose={vi.fn()}
        onSave={onSave}
      />
    );

    await user.click(screen.getByRole("button", { name: "Criar exercício" }));
    expect(await screen.findByText("Informe o nome do exercício")).toBeInTheDocument();
    expect(screen.getByText("Selecione pelo menos um grupo muscular")).toBeInTheDocument();

    await user.type(screen.getByLabelText("Nome do exercício *"), "Rosca direta");
    await user.click(screen.getByLabelText("Bíceps"));
    await user.selectOptions(screen.getByLabelText("Equipamento *"), "BARRA_LIVRE");
    await user.type(
      screen.getByLabelText("Execução correta *"),
      "Cotovelos fixos ao lado do tronco."
    );
    await user.type(
      screen.getByLabelText("Erros comuns (1 por linha)"),
      "Balançar o tronco\nAbrir cotovelos"
    );

    await user.click(screen.getByRole("button", { name: "Criar exercício" }));

    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith(
        {
          name: "Rosca direta",
          muscleGroups: ["BICEPS"],
          equipment: "BARRA_LIVRE",
          execution: "Cotovelos fixos ao lado do tronco.",
          commonErrors: ["Balançar o tronco", "Abrir cotovelos"],
          progressions: [],
          regressions: [],
          contraindications: [],
          references: [],
        },
        undefined
      );
    });
  });

  it("pré-preenche edição e avisa que salvar cria nova versão (v+1)", async () => {
    const user = userEvent.setup();
    const onSave = vi.fn().mockResolvedValue(undefined);
    const existing: Exercise = {
      id: "ex-1",
      name: "Supino reto",
      muscleGroups: ["PEITO", "TRICEPS"],
      equipment: "BARRA_LIVRE",
      execution: "Desça a barra até o peito.",
      commonErrors: ["Quicar a barra"],
      progressions: [],
      regressions: [],
      contraindications: [],
      references: [],
      version: 1,
      superseded: false,
    };

    render(
      <ExerciseFormModal
        open={true}
        exercise={existing}
        pending={false}
        error={null}
        onClose={vi.fn()}
        onSave={onSave}
      />
    );

    expect(screen.getByText(/nova versão \(v2\)/i)).toBeInTheDocument();
    const nameInput = screen.getByLabelText("Nome do exercício *");
    expect(nameInput).toHaveValue("Supino reto");

    await user.clear(nameInput);
    await user.type(nameInput, "Supino reto com pausa");
    await user.click(screen.getByRole("button", { name: "Salvar nova versão (v2)" }));

    await waitFor(() => {
      expect(onSave).toHaveBeenCalledWith(
        expect.objectContaining({
          name: "Supino reto com pausa",
          muscleGroups: ["PEITO", "TRICEPS"],
          equipment: "BARRA_LIVRE",
        }),
        "ex-1"
      );
    });
  });
});
