import { describe, expect, it } from "vitest";
import {
  ExercisePrescription,
  TrainingPlan,
  TrainingSession,
  comparePlanVersions,
  currentPlanVersion,
  previousPlanVersions,
  prescriptionUpdatePayload,
  sortPlanVersions,
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

function plan(overrides: Partial<TrainingPlan> = {}): TrainingPlan {
  return {
    id: "plan-v1",
    athleteId: "athlete-1",
    periodizationId: "periodization-1",
    name: "Hipertrofia — bloco 1",
    description: "Foco em membros superiores.",
    version: 1,
    previousVersionId: null,
    status: "ACTIVE",
    createdAt: "2026-09-01T10:00:00Z",
    updatedAt: "2026-09-01T10:00:00Z",
    deleted: false,
    ...overrides,
  };
}

function session(overrides: Partial<TrainingSession> = {}): TrainingSession {
  return {
    id: "session-1",
    periodizationId: "periodization-1",
    planId: "plan-v1",
    athleteId: "athlete-1",
    name: "Treino A",
    description: "Superiores",
    weekNumber: 1,
    dayOfWeek: 1,
    status: "ACTIVE",
    exercises: [],
    deleted: false,
    ...overrides,
  };
}

function exercise(
  overrides: Partial<ExercisePrescription> = {}
): ExercisePrescription {
  return prescription(overrides);
}

describe("plan versions (T-074)", () => {
  it("sorts versions from the newest to the oldest and ignores soft-deleted plans", () => {
    const versions = sortPlanVersions([
      plan({ id: "v1", version: 1 }),
      plan({ id: "v3", version: 3, status: "DRAFT" }),
      plan({ id: "deleted", version: 4, deleted: true }),
      plan({ id: "v2", version: 2 }),
    ]);

    expect(versions.map((item) => item.id)).toEqual(["v3", "v2", "v1"]);
  });

  it("treats the newest ACTIVE plan as the athlete's current version", () => {
    const current = currentPlanVersion([
      plan({ id: "v1", version: 1, status: "FINISHED" }),
      plan({ id: "v2", version: 2, status: "ACTIVE" }),
      plan({ id: "v3", version: 3, status: "IN_REVIEW" }),
      plan({ id: "v4", version: 4, status: "DRAFT" }),
    ]);

    expect(current?.id).toBe("v2");
  });

  it("lists only versions older than the current one, newest first", () => {
    const current = plan({ id: "v3", version: 3 });
    const history = previousPlanVersions(
      [
        plan({ id: "v1", version: 1, status: "FINISHED" }),
        plan({ id: "v2", version: 2, status: "FINISHED" }),
        current,
        plan({ id: "v4", version: 4, status: "DRAFT" }),
      ],
      current
    );

    expect(history.map((item) => item.id)).toEqual(["v2", "v1"]);
  });
});

describe("comparePlanVersions (T-074)", () => {
  const previous = plan({ id: "v1", version: 1 });
  const current = plan({
    id: "v2",
    version: 2,
    previousVersionId: "v1",
    status: "DRAFT",
  });

  it("detects added, changed and unchanged sessions with exercise details", () => {
    const previousSessions = [
      session({
        id: "session-a-v1",
        planId: previous.id,
        exercises: [
          exercise({ id: "a1", exerciseName: "Supino reto" }),
          exercise({ id: "a2", exerciseName: "Remada curvada", orderIndex: 1 }),
        ],
      }),
      session({
        id: "session-b-v1",
        planId: previous.id,
        name: "Treino B",
        dayOfWeek: 3,
        exercises: [
          exercise({ id: "b1", exerciseName: "Agachamento livre" }),
        ],
      }),
    ];
    const currentSessions = [
      session({
        id: "session-a-v2",
        planId: current.id,
        status: "DRAFT",
        exercises: [
          exercise({ id: "a1", exerciseName: "Supino reto" }),
          exercise({ id: "a2", exerciseName: "Remada curvada", orderIndex: 1 }),
          exercise({ id: "a3", exerciseName: "Crucifixo", orderIndex: 2 }),
        ],
      }),
      session({
        id: "session-b-v2",
        planId: current.id,
        name: "Treino B",
        dayOfWeek: 3,
        status: "DRAFT",
        exercises: [
          exercise({
            id: "b1",
            exerciseName: "Agachamento livre",
            load: 100,
          }),
        ],
      }),
      session({
        id: "session-c-v2",
        planId: current.id,
        name: "Treino C",
        dayOfWeek: 5,
        status: "DRAFT",
        exercises: [exercise({ id: "c1", exerciseName: "Leg press" })],
      }),
    ];

    const comparison = comparePlanVersions(
      previous,
      current,
      previousSessions,
      currentSessions
    );

    expect(comparison.summary).toEqual({
      added: 1,
      removed: 0,
      changed: 2,
      unchanged: 0,
      previousExerciseCount: 3,
      currentExerciseCount: 5,
    });
    expect(comparison.sessions.map((item) => item.name)).toEqual([
      "Treino A",
      "Treino B",
      "Treino C",
    ]);
    expect(
      comparison.sessions[0].exerciseChanges.find(
        (item) => item.exerciseName === "Crucifixo"
      )?.change
    ).toBe("ADDED");
    expect(comparison.sessions[1].exerciseChanges[0].change).toBe("CHANGED");
    expect(comparison.sessions[2].change).toBe("ADDED");
  });

  it("reports removed sessions and no changes for identical content", () => {
    const previousSessions = [
      session({ id: "a-v1", planId: previous.id }),
      session({
        id: "removed-v1",
        planId: previous.id,
        name: "Treino removido",
        dayOfWeek: 5,
      }),
    ];
    const currentSessions = [session({ id: "a-v2", planId: current.id })];

    const comparison = comparePlanVersions(
      previous,
      current,
      previousSessions,
      currentSessions
    );

    expect(comparison.sessions[1].change).toBe("REMOVED");
    expect(comparison.summary.removed).toBe(1);
    expect(comparison.sessions[0].change).toBe("UNCHANGED");
  });
});

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
