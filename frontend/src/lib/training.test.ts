import { describe, expect, it } from "vitest";
import {
  ExercisePrescription,
  prescriptionUpdatePayload,
} from "./training";

function prescription(
  overrides: Partial<ExercisePrescription> = {}
): ExercisePrescription {
  return {
    id: "prescription-1",
    exerciseId: "exercise-v1",
    exerciseName: "Supino reto com barra",
    customExercise: false,
    orderIndex: 0,
    sets: 4,
    repetitions: 10,
    load: 60,
    intensityType: "RPE",
    intensityValue: 8,
    restSeconds: 90,
    notes: "Cadência controlada.",
    exerciseVersion: 1,
    newerVersionAvailable: true,
    currentExerciseId: "exercise-v2",
    ...overrides,
  };
}

describe("prescriptionUpdatePayload", () => {
  it("preserves the prescription parameters when editing without swapping the version", () => {
    expect(prescriptionUpdatePayload(prescription())).toEqual({
      exerciseId: "exercise-v1",
      exerciseName: "Supino reto com barra",
      sets: 4,
      repetitions: 10,
      load: 60,
      intensityType: "RPE",
      intensityValue: 8,
      restSeconds: 90,
      notes: "Cadência controlada.",
    });
  });

  it("points to the current library exercise when swapping the version", () => {
    const payload = prescriptionUpdatePayload(prescription(), {
      id: "exercise-v2",
      name: "Supino reto com halteres",
    });

    expect(payload.exerciseId).toBe("exercise-v2");
    expect(payload.exerciseName).toBe("Supino reto com halteres");
    expect(payload.sets).toBe(4);
    expect(payload.notes).toBe("Cadência controlada.");
  });

  it("keeps custom exercises without a library id", () => {
    const payload = prescriptionUpdatePayload(
      prescription({
        customExercise: true,
        exerciseId: null,
        exerciseName: "Exercício do coach",
        exerciseVersion: null,
        newerVersionAvailable: false,
        currentExerciseId: null,
      })
    );

    expect(payload.exerciseId).toBeNull();
    expect(payload.exerciseName).toBe("Exercício do coach");
  });

  it("refuses to swap the version when the intensity value is missing", () => {
    expect(() =>
      prescriptionUpdatePayload(prescription({ intensityValue: null }))
    ).toThrow(/intensidade/i);
  });
});
