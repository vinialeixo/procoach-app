import { http, HttpResponse } from "msw";
import type {
  AddExercisePayload,
  ExercisePrescription,
  MuscleGroup,
  Periodization,
  TrainingPlan,
  TrainingSession,
  TrainingStatus,
  UpdateExercisePrescriptionPayload,
} from "@/lib/training";
import { mockDb, nextMockId } from "../db";

type CreatePeriodizationBody = {
  athleteId: string;
  name: string;
  description: string;
  startDate: string;
  endDate: string;
};

type CreatePlanBody = {
  athleteId: string;
  periodizationId: string;
  name: string;
  description: string;
};

type CreatePlanVersionBody = {
  newName: string;
  newDescription: string;
  copySessions?: boolean;
};

type CreateSessionBody = {
  athleteId: string;
  periodizationId: string;
  planId: string;
  name: string;
  description: string;
  weekNumber: number;
  dayOfWeek: number;
};

type TransitionBody = {
  targetStatus: TrainingStatus;
};

type ReorderBody = {
  orderedPrescriptionIds: string[];
};

export const trainingHandlers = [
  http.get("/api/library/exercises", ({ request }) => {
    const url = new URL(request.url);
    const name = url.searchParams.get("name")?.trim().toLowerCase() ?? "";
    const muscleGroup = url.searchParams.get("muscleGroup") as MuscleGroup | null;

    const list = mockDb.exercises.filter((exercise) => {
      if (name && !exercise.name.toLowerCase().includes(name)) return false;
      if (muscleGroup && !exercise.muscleGroups.includes(muscleGroup)) return false;
      return true;
    });

    return HttpResponse.json(list);
  }),

  http.get("/api/library/exercises/:id", ({ params }) => {
    const exercise = mockDb.exercises.find((item) => item.id === params.id);
    if (!exercise) {
      return HttpResponse.json({ message: "Exercício não encontrado." }, { status: 404 });
    }
    return HttpResponse.json(exercise);
  }),

  http.get("/api/training/periodizations/athlete/:athleteId", ({ params }) => {
    const list = mockDb.periodizations.filter(
      (item) => item.athleteId === params.athleteId && !item.deleted
    );
    return HttpResponse.json(list);
  }),

  http.post("/api/training/periodizations", async ({ request }) => {
    const body = (await request.json()) as CreatePeriodizationBody;
    const created: Periodization = {
      id: nextMockId("per"),
      athleteId: body.athleteId,
      name: body.name,
      description: body.description || null,
      startDate: body.startDate || null,
      endDate: body.endDate || null,
      status: "ACTIVE",
      deleted: false,
    };
    mockDb.periodizations.push(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.get("/api/training/plans/athlete/:athleteId", ({ params }) => {
    const list = mockDb.plans.filter(
      (item) => item.athleteId === params.athleteId && !item.deleted
    );
    return HttpResponse.json(list);
  }),

  http.post("/api/training/plans", async ({ request }) => {
    const body = (await request.json()) as CreatePlanBody;
    const now = new Date().toISOString();
    const created: TrainingPlan = {
      id: nextMockId("plan"),
      athleteId: body.athleteId,
      periodizationId: body.periodizationId,
      name: body.name,
      description: body.description || null,
      version: 1,
      previousVersionId: null,
      status: "DRAFT",
      createdAt: now,
      updatedAt: now,
      deleted: false,
    };
    mockDb.plans.push(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.post("/api/training/plans/:planId/versions", async ({ params, request }) => {
    const body = (await request.json()) as CreatePlanVersionBody;
    const source = mockDb.plans.find((item) => item.id === params.planId && !item.deleted);
    if (!source) {
      return HttpResponse.json({ message: "Plano de origem não encontrado." }, { status: 404 });
    }

    const highestVersion = mockDb.plans
      .filter((item) => item.athleteId === source.athleteId && !item.deleted)
      .reduce((max, item) => Math.max(max, item.version), 0);

    const now = new Date().toISOString();
    const created: TrainingPlan = {
      id: nextMockId("plan"),
      athleteId: source.athleteId,
      periodizationId: source.periodizationId,
      name: body.newName || source.name,
      description: body.newDescription ?? source.description,
      version: highestVersion + 1,
      previousVersionId: source.id,
      status: "DRAFT",
      createdAt: now,
      updatedAt: now,
      deleted: false,
    };
    mockDb.plans.push(created);

    if (body.copySessions !== false) {
      const sourceSessions = mockDb.sessions.filter(
        (session) => session.planId === source.id && !session.deleted
      );
      for (const session of sourceSessions) {
        const clonedSession: TrainingSession = {
          ...session,
          id: nextMockId("sess"),
          planId: created.id,
          status: "DRAFT",
          exercises: session.exercises.map((exercise) => ({
            ...exercise,
            id: nextMockId("presc"),
          })),
        };
        mockDb.sessions.push(clonedSession);
      }
    }

    return HttpResponse.json(created, { status: 201 });
  }),

  http.post("/api/training/plans/:planId/transition", async ({ params, request }) => {
    const body = (await request.json()) as TransitionBody;
    const plan = mockDb.plans.find((item) => item.id === params.planId && !item.deleted);
    if (!plan) {
      return HttpResponse.json({ message: "Plano não encontrado." }, { status: 404 });
    }
    plan.status = body.targetStatus;
    plan.updatedAt = new Date().toISOString();
    return HttpResponse.json(plan);
  }),

  http.get("/api/training/trainings/periodization/:periodizationId", ({ params }) => {
    const list = mockDb.sessions.filter(
      (item) => item.periodizationId === params.periodizationId && !item.deleted
    );
    return HttpResponse.json(list);
  }),

  http.get("/api/training/trainings/athlete/:athleteId", ({ params, request }) => {
    const url = new URL(request.url);
    const planId = url.searchParams.get("planId");
    const list = mockDb.sessions.filter(
      (item) =>
        item.athleteId === params.athleteId &&
        !item.deleted &&
        (!planId || item.planId === planId)
    );
    return HttpResponse.json(list);
  }),

  http.post("/api/training/trainings", async ({ request }) => {
    const body = (await request.json()) as CreateSessionBody;
    const created: TrainingSession = {
      id: nextMockId("sess"),
      periodizationId: body.periodizationId,
      planId: body.planId,
      athleteId: body.athleteId,
      name: body.name,
      description: body.description || null,
      weekNumber: body.weekNumber,
      dayOfWeek: body.dayOfWeek,
      status: "DRAFT",
      exercises: [],
      deleted: false,
    };
    mockDb.sessions.push(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.post("/api/training/trainings/:sessionId/transition", async ({ params, request }) => {
    const body = (await request.json()) as TransitionBody;
    const session = mockDb.sessions.find((item) => item.id === params.sessionId && !item.deleted);
    if (!session) {
      return HttpResponse.json({ message: "Sessão não encontrada." }, { status: 404 });
    }
    session.status = body.targetStatus;
    return HttpResponse.json(session);
  }),

  http.post("/api/training/trainings/:sessionId/exercises", async ({ params, request }) => {
    const body = (await request.json()) as AddExercisePayload & {
      customExercise?: boolean;
      notes?: string | null;
    };
    const session = mockDb.sessions.find((item) => item.id === params.sessionId && !item.deleted);
    if (!session) {
      return HttpResponse.json({ message: "Sessão não encontrada." }, { status: 404 });
    }

    const exercise = mockDb.exercises.find((item) => item.id === body.exerciseId);
    const prescription: ExercisePrescription = {
      id: nextMockId("presc"),
      exerciseId: body.exerciseId,
      exerciseName: body.exerciseName,
      customExercise: Boolean(body.customExercise),
      orderIndex: body.orderIndex ?? session.exercises.length + 1,
      sets: body.sets,
      repetitions: body.repetitions,
      load: body.load,
      intensityType: body.intensityType,
      intensityValue: body.intensityValue,
      restSeconds: body.restSeconds,
      notes: body.notes ?? null,
      exerciseVersion: exercise?.version ?? 1,
      newerVersionAvailable: Boolean(exercise?.superseded),
      currentExerciseId: null,
    };

    session.exercises.push(prescription);
    return HttpResponse.json(session, { status: 201 });
  }),

  http.put("/api/training/trainings/:sessionId/exercises/reorder", async ({ params, request }) => {
    const body = (await request.json()) as ReorderBody;
    const session = mockDb.sessions.find((item) => item.id === params.sessionId && !item.deleted);
    if (!session) {
      return HttpResponse.json({ message: "Sessão não encontrada." }, { status: 404 });
    }

    const orderMap = new Map<string, number>();
    body.orderedPrescriptionIds.forEach((id, index) => orderMap.set(id, index + 1));

    session.exercises = session.exercises
      .map((exercise) => ({
        ...exercise,
        orderIndex: orderMap.get(exercise.id) ?? exercise.orderIndex,
      }))
      .sort((a, b) => a.orderIndex - b.orderIndex);

    return HttpResponse.json(session);
  }),

  http.put(
    "/api/training/trainings/:sessionId/exercises/:prescriptionId",
    async ({ params, request }) => {
      const body = (await request.json()) as UpdateExercisePrescriptionPayload;
      const session = mockDb.sessions.find(
        (item) => item.id === params.sessionId && !item.deleted
      );
      if (!session) {
        return HttpResponse.json({ message: "Sessão não encontrada." }, { status: 404 });
      }

      const prescription = session.exercises.find(
        (item) => item.id === params.prescriptionId
      );
      if (!prescription) {
        return HttpResponse.json({ message: "Prescrição não encontrada." }, { status: 404 });
      }

      const targetExercise = body.exerciseId
        ? mockDb.exercises.find((item) => item.id === body.exerciseId)
        : undefined;

      prescription.exerciseId = body.exerciseId;
      prescription.exerciseName = body.exerciseName;
      prescription.sets = body.sets;
      prescription.repetitions = body.repetitions;
      prescription.load = body.load;
      prescription.intensityType = body.intensityType;
      prescription.intensityValue = body.intensityValue;
      prescription.restSeconds = body.restSeconds;
      prescription.notes = body.notes;
      if (targetExercise) {
        prescription.exerciseVersion = targetExercise.version;
        prescription.newerVersionAvailable = targetExercise.superseded;
        prescription.currentExerciseId = targetExercise.superseded
          ? prescription.currentExerciseId
          : null;
      }

      return HttpResponse.json(session);
    }
  ),

  http.delete(
    "/api/training/trainings/:sessionId/exercises/:prescriptionId",
    ({ params }) => {
      const session = mockDb.sessions.find(
        (item) => item.id === params.sessionId && !item.deleted
      );
      if (!session) {
        return HttpResponse.json({ message: "Sessão não encontrada." }, { status: 404 });
      }

      session.exercises = session.exercises
        .filter((item) => item.id !== params.prescriptionId)
        .map((item, index) => ({ ...item, orderIndex: index + 1 }));

      return HttpResponse.json(session);
    }
  ),
];
