import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { SessionEditorView } from "./SessionEditorView";
import { Exercise, TrainingSession } from "@/lib/training";

const mockSessionWithOutdatedExercise: TrainingSession = {
  id: "ses-1",
  athleteId: "ath-1",
  periodizationId: "per-1",
  planId: "plan-1",
  name: "Treino A - Superiores",
  description: "Foco hipertrofia peito/costas",
  weekNumber: 1,
  dayOfWeek: 1,
  status: "ACTIVE",
  exercises: [
    {
      id: "presc-1",
      exerciseId: "ex-old-1",
      exerciseName: "Supino Reto com Barra",
      customExercise: false,
      orderIndex: 0,
      sets: 4,
      repetitions: 10,
      load: 80,
      intensityType: "RPE",
      intensityValue: 8,
      restSeconds: 90,
      notes: "Carga progressiva",
      exerciseVersion: 1,
      newerVersionAvailable: true,
      currentExerciseId: "ex-new-2",
    },
    {
      id: "presc-2",
      exerciseId: "ex-uptodate",
      exerciseName: "Remada Curvada",
      customExercise: false,
      orderIndex: 1,
      sets: 3,
      repetitions: 12,
      load: 60,
      intensityType: "RPE",
      intensityValue: 7.5,
      restSeconds: 60,
      notes: null,
      exerciseVersion: 2,
      newerVersionAvailable: false,
      currentExerciseId: null,
    },
  ],
  deleted: false,
};

const mockLibraryExercises: Exercise[] = [
  {
    id: "ex-new-2",
    name: "Supino Reto com Barra (Atualizado)",
    muscleGroups: ["PEITO"],
    equipment: "BARRA",
    execution: "Execução na barra",
    commonErrors: [],
    progressions: [],
    regressions: [],
    contraindications: [],
    references: [],
    version: 2,
    superseded: false,
  },
];

describe("SessionEditorView (Issue #42)", () => {
  it("exibe badge evidente 'Versão desatualizada (v{exerciseVersion})' na linha do cabeçalho", () => {
    render(
      <SessionEditorView
        selectedSession={mockSessionWithOutdatedExercise}
        exercises={mockLibraryExercises}
        exercisesLoading={false}
        exercisesError={false}
        transitionSessionPending={false}
        transitionSessionError={null}
        reorderExercisesPending={false}
        reorderExercisesError={null}
        applyCurrentVersionPending={false}
        applyCurrentVersionError={null}
        addExercisePending={false}
        addExerciseError={null}
        updatePrescriptionPending={false}
        updatePrescriptionError={null}
        removePrescriptionPending={false}
        removePrescriptionError={null}
        onSearchExercises={vi.fn()}
        onTransitionSession={vi.fn()}
        onReorderExercises={vi.fn()}
        onApplyCurrentVersion={vi.fn()}
        onAddExercise={vi.fn()}
        onUpdatePrescription={vi.fn()}
        onRemovePrescription={vi.fn()}
      />
    );

    const outdatedBadge = screen.getByTestId("badge-outdated-presc-1");
    expect(outdatedBadge).toBeInTheDocument();
    expect(outdatedBadge).toHaveTextContent("Versão desatualizada (v1)");

    expect(screen.queryByTestId("badge-outdated-presc-2")).not.toBeInTheDocument();
  });

  it("permite trocar pela versão atual em 1 clique com feedback de carregamento e sucesso", async () => {
    const user = userEvent.setup();
    const handleApply = vi.fn().mockResolvedValue(undefined);

    render(
      <SessionEditorView
        selectedSession={mockSessionWithOutdatedExercise}
        exercises={mockLibraryExercises}
        exercisesLoading={false}
        exercisesError={false}
        transitionSessionPending={false}
        transitionSessionError={null}
        reorderExercisesPending={false}
        reorderExercisesError={null}
        applyCurrentVersionPending={false}
        applyCurrentVersionError={null}
        addExercisePending={false}
        addExerciseError={null}
        updatePrescriptionPending={false}
        updatePrescriptionError={null}
        removePrescriptionPending={false}
        removePrescriptionError={null}
        onSearchExercises={vi.fn()}
        onTransitionSession={vi.fn()}
        onReorderExercises={vi.fn()}
        onApplyCurrentVersion={handleApply}
        onAddExercise={vi.fn()}
        onUpdatePrescription={vi.fn()}
        onRemovePrescription={vi.fn()}
      />
    );

    const updateButton = screen.getByRole("button", { name: "Trocar pela versão atual" });
    expect(updateButton).toBeInTheDocument();

    await user.click(updateButton);

    expect(handleApply).toHaveBeenCalledWith(mockSessionWithOutdatedExercise.exercises[0]);

    await waitFor(() => {
      expect(
        screen.getByText(/atualizado para a versão atual/)
      ).toBeInTheDocument();
    });
  });

  it("foca o campo de busca quando autoFocusAddExercise=true", () => {
    render(
      <SessionEditorView
        selectedSession={mockSessionWithOutdatedExercise}
        exercises={mockLibraryExercises}
        exercisesLoading={false}
        exercisesError={false}
        autoFocusAddExercise={true}
        transitionSessionPending={false}
        transitionSessionError={null}
        reorderExercisesPending={false}
        reorderExercisesError={null}
        applyCurrentVersionPending={false}
        applyCurrentVersionError={null}
        addExercisePending={false}
        addExerciseError={null}
        updatePrescriptionPending={false}
        updatePrescriptionError={null}
        removePrescriptionPending={false}
        removePrescriptionError={null}
        onSearchExercises={vi.fn()}
        onTransitionSession={vi.fn()}
        onReorderExercises={vi.fn()}
        onApplyCurrentVersion={vi.fn()}
        onAddExercise={vi.fn()}
        onUpdatePrescription={vi.fn()}
        onRemovePrescription={vi.fn()}
      />
    );

    const searchInput = screen.getByLabelText("Buscar exercício");
    expect(searchInput).toHaveFocus();
  });
});
