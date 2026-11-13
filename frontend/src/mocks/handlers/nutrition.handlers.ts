import { http, HttpResponse } from "msw";
import {
  DIET_STATUS_FLOW,
  type DietObjective,
  type DietStatus,
  type EnergyClassification,
} from "@/lib/diets";
import { mockDb, nextMockId, type MockApiDiet, type MockMeal, type MockMealItem } from "../db";

type CreateDietBody = {
  athleteId: string;
  objective: DietObjective;
  calories: number;
  protein: number;
  carbohydrates: number;
  fat: number;
};

type CreateDietVersionBody = Omit<CreateDietBody, "athleteId"> & {
  changeReason: string;
};

type ChangeDietStatusBody = {
  status: DietStatus;
};

type CreateMealBody = {
  name: string;
  time: string;
  orderIndex?: number;
};

type CreateMealItemBody = Omit<MockMealItem, "id">;

function buildEnergyBalanceResponse(diet: MockApiDiet) {
  const latestAssessment = mockDb.assessments
    .filter((item) => item.athleteId === diet.athleteId)
    .slice()
    .sort((a, b) => b.assessmentDate.localeCompare(a.assessmentDate))[0];

  if (!latestAssessment || latestAssessment.metrics.getKcal === null || latestAssessment.metrics.tmbKcal === null) {
    return HttpResponse.json(
      {
        code: "ASSESSMENT_REQUIRED",
        message: "Registre uma avaliação física para calcular o balanço energético.",
      },
      { status: 409 }
    );
  }

  const getKcal = latestAssessment.metrics.getKcal;
  const balanceKcal = Math.round(diet.calories - getKcal);
  const balancePercent = Number(((balanceKcal / getKcal) * 100).toFixed(1));

  let classification: EnergyClassification = "MAINTENANCE";
  if (balanceKcal <= -100) {
    classification = "DEFICIT";
  } else if (balanceKcal >= 100) {
    classification = "SURPLUS";
  }

  return HttpResponse.json({
    dietId: diet.id,
    calories: diet.calories,
    tmbKcal: latestAssessment.metrics.tmbKcal,
    getKcal,
    balanceKcal,
    balancePercent,
    classification,
    basedOnAssessmentDate: latestAssessment.assessmentDate,
    weightKg: latestAssessment.weightKg,
  });
}

export const nutritionHandlers = [
  http.get("/api/v1/diets/history", ({ request }) => {
    const url = new URL(request.url);
    const athleteId = url.searchParams.get("athleteId");
    const list = mockDb.diets
      .filter((item) => !item.deleted && (!athleteId || item.athleteId === athleteId))
      .slice()
      .sort((a, b) => b.version - a.version);
    return HttpResponse.json(list);
  }),

  http.get("/api/v1/diets/energy-balance", ({ request }) => {
    const url = new URL(request.url);
    const athleteId = url.searchParams.get("athleteId");
    const currentDiet = mockDb.diets
      .filter((item) => !item.deleted && item.athleteId === athleteId)
      .slice()
      .sort((a, b) => b.version - a.version)
      .find((item) => !item.readOnly);

    if (!currentDiet) {
      return HttpResponse.json({ message: "Dieta não encontrada." }, { status: 404 });
    }

    return buildEnergyBalanceResponse(currentDiet);
  }),

  http.get("/api/v1/diets/:id/energy-balance", ({ params }) => {
    const diet = mockDb.diets.find((item) => item.id === params.id && !item.deleted);
    if (!diet) {
      return HttpResponse.json({ message: "Dieta não encontrada." }, { status: 404 });
    }
    return buildEnergyBalanceResponse(diet);
  }),

  http.get("/api/v1/diets", ({ request }) => {
    const url = new URL(request.url);
    const athleteId = url.searchParams.get("athleteId");
    const approved = mockDb.diets
      .filter(
        (item) =>
          !item.deleted &&
          item.athleteId === athleteId &&
          item.status === "APPROVED"
      )
      .slice()
      .sort((a, b) => b.version - a.version)[0];

    if (!approved) {
      return HttpResponse.json(
        { message: "Nenhuma dieta aprovada disponível." },
        { status: 404 }
      );
    }

    return HttpResponse.json(approved);
  }),

  http.get("/api/v1/diets/:id", ({ params }) => {
    const diet = mockDb.diets.find((item) => item.id === params.id && !item.deleted);
    if (!diet) {
      return HttpResponse.json({ message: "Dieta não encontrada." }, { status: 404 });
    }
    return HttpResponse.json(diet);
  }),

  http.post("/api/v1/diets", async ({ request }) => {
    const body = (await request.json()) as CreateDietBody;
    const now = new Date().toISOString();

    const created: MockApiDiet = {
      id: nextMockId("diet"),
      coachId: mockDb.coach.id,
      athleteId: body.athleteId,
      objective: body.objective,
      calories: body.calories,
      protein: body.protein,
      carbohydrates: body.carbohydrates,
      fat: body.fat,
      status: "DRAFT",
      version: 1,
      previousVersionId: null,
      changeReason: null,
      createdAt: now,
      updatedAt: now,
      readOnly: false,
      deleted: false,
      createdBy: mockDb.coach.id,
      updatedBy: mockDb.coach.id,
    };

    mockDb.diets.push(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.post("/api/v1/diets/:versionId/versions", async ({ params, request }) => {
    const body = (await request.json()) as CreateDietVersionBody;
    const source = mockDb.diets.find(
      (item) => item.id === params.versionId && !item.deleted
    );

    if (!source) {
      return HttpResponse.json({ message: "Versão de origem não encontrada." }, { status: 404 });
    }
    if (!body.changeReason?.trim()) {
      return HttpResponse.json(
        { message: "Motivo da alteração é obrigatório." },
        { status: 400 }
      );
    }

    const now = new Date().toISOString();
    source.readOnly = true;
    source.updatedAt = now;
    source.updatedBy = mockDb.coach.id;

    const highestVersion = mockDb.diets
      .filter((item) => item.athleteId === source.athleteId && !item.deleted)
      .reduce((max, item) => Math.max(max, item.version), 0);

    const created: MockApiDiet = {
      id: nextMockId("diet"),
      coachId: source.coachId,
      athleteId: source.athleteId,
      objective: body.objective,
      calories: body.calories,
      protein: body.protein,
      carbohydrates: body.carbohydrates,
      fat: body.fat,
      status: "DRAFT",
      version: highestVersion + 1,
      previousVersionId: source.id,
      changeReason: body.changeReason.trim(),
      createdAt: now,
      updatedAt: now,
      readOnly: false,
      deleted: false,
      createdBy: mockDb.coach.id,
      updatedBy: mockDb.coach.id,
    };

    mockDb.diets.push(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.post("/api/v1/diets/:versionId/status", async ({ params, request }) => {
    const body = (await request.json()) as ChangeDietStatusBody;
    const diet = mockDb.diets.find(
      (item) => item.id === params.versionId && !item.deleted
    );

    if (!diet) {
      return HttpResponse.json({ message: "Dieta não encontrada." }, { status: 404 });
    }
    if (diet.readOnly) {
      return HttpResponse.json(
        { message: "Versão congelada não permite alteração de status." },
        { status: 409 }
      );
    }

    const expectedNext = DIET_STATUS_FLOW[diet.status];
    if (expectedNext !== body.status) {
      return HttpResponse.json(
        { message: "Transição de status inválida." },
        { status: 409 }
      );
    }

    diet.status = body.status;
    diet.updatedAt = new Date().toISOString();
    diet.updatedBy = mockDb.coach.id;

    return HttpResponse.json(diet);
  }),

  // CRUD de refeições e itens (T-085)
  http.get("/api/v1/diets/:id/meals", ({ params }) => {
    const meals = mockDb.meals
      .filter((meal) => meal.dietId === params.id)
      .slice()
      .sort((a, b) => a.orderIndex - b.orderIndex);
    return HttpResponse.json(meals);
  }),

  http.post("/api/v1/diets/:id/meals", async ({ params, request }) => {
    const body = (await request.json()) as CreateMealBody;
    const dietId = String(params.id);
    const existingCount = mockDb.meals.filter((meal) => meal.dietId === dietId).length;
    const created: MockMeal = {
      id: nextMockId("meal"),
      dietId,
      name: body.name,
      time: body.time,
      orderIndex: body.orderIndex ?? existingCount + 1,
      items: [],
    };
    mockDb.meals.push(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  http.put("/api/v1/diets/:id/meals/:mealId", async ({ params, request }) => {
    const body = (await request.json()) as Partial<CreateMealBody>;
    const meal = mockDb.meals.find(
      (item) => item.dietId === params.id && item.id === params.mealId
    );
    if (!meal) {
      return HttpResponse.json({ message: "Refeição não encontrada." }, { status: 404 });
    }
    if (body.name !== undefined) meal.name = body.name;
    if (body.time !== undefined) meal.time = body.time;
    if (body.orderIndex !== undefined) meal.orderIndex = body.orderIndex;
    return HttpResponse.json(meal);
  }),

  http.delete("/api/v1/diets/:id/meals/:mealId", ({ params }) => {
    const index = mockDb.meals.findIndex(
      (item) => item.dietId === params.id && item.id === params.mealId
    );
    if (index === -1) {
      return HttpResponse.json({ message: "Refeição não encontrada." }, { status: 404 });
    }
    mockDb.meals.splice(index, 1);
    return new HttpResponse(null, { status: 204 });
  }),

  http.post("/api/v1/diets/:id/meals/:mealId/items", async ({ params, request }) => {
    const body = (await request.json()) as CreateMealItemBody;
    const meal = mockDb.meals.find(
      (item) => item.dietId === params.id && item.id === params.mealId
    );
    if (!meal) {
      return HttpResponse.json({ message: "Refeição não encontrada." }, { status: 404 });
    }
    const createdItem: MockMealItem = {
      id: nextMockId("item"),
      ...body,
    };
    meal.items.push(createdItem);
    return HttpResponse.json(meal, { status: 201 });
  }),

  http.delete("/api/v1/diets/:id/meals/:mealId/items/:itemId", ({ params }) => {
    const meal = mockDb.meals.find(
      (item) => item.dietId === params.id && item.id === params.mealId
    );
    if (!meal) {
      return HttpResponse.json({ message: "Refeição não encontrada." }, { status: 404 });
    }
    meal.items = meal.items.filter((item) => item.id !== params.itemId);
    return HttpResponse.json(meal);
  }),
];
