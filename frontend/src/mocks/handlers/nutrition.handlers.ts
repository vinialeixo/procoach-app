import { http, HttpResponse } from "msw";
import {
  DIET_STATUS_FLOW,
  type AddMealItemPayload,
  type AddMealPayload,
  type ApiDiet,
  type DietObjective,
  type DietStatus,
  type EnergyClassification,
  type Macronutrients,
  type Meal,
  type MealItem,
} from "@/lib/diets";
import { mockDb, nextMockId } from "../db";

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

function sumMacros(items: ReadonlyArray<Macronutrients>): Macronutrients {
  return items.reduce<Macronutrients>(
    (acc, item) => ({
      calories: Number((acc.calories + item.calories).toFixed(1)),
      protein: Number((acc.protein + item.protein).toFixed(1)),
      carbohydrates: Number((acc.carbohydrates + item.carbohydrates).toFixed(1)),
      fat: Number((acc.fat + item.fat).toFixed(1)),
    }),
    { calories: 0, protein: 0, carbohydrates: 0, fat: 0 }
  );
}

function recalculateDietTotals(diet: ApiDiet): void {
  const meals = diet.meals ?? [];
  for (const meal of meals) {
    meal.totals = sumMacros(meal.items);
  }
  const hasItems = meals.some((meal) => meal.items.length > 0);
  diet.totals = hasItems
    ? sumMacros(meals.flatMap((meal) => meal.items))
    : {
        calories: diet.calories,
        protein: diet.protein,
        carbohydrates: diet.carbohydrates,
        fat: diet.fat,
      };
}

function cloneMeals(meals: Meal[] | undefined): Meal[] {
  return (meals ?? []).map((meal) => ({
    ...meal,
    id: nextMockId("meal"),
    items: meal.items.map((item) => ({
      ...item,
      id: nextMockId("item"),
    })),
  }));
}

function buildEnergyBalanceResponse(diet: ApiDiet) {
  const latestAssessment = mockDb.assessments
    .filter((item) => item.athleteId === diet.athleteId)
    .slice()
    .sort((a, b) => b.assessmentDate.localeCompare(a.assessmentDate))[0];

  if (
    !latestAssessment ||
    latestAssessment.metrics.getKcal === null ||
    latestAssessment.metrics.tmbKcal === null
  ) {
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

    const created: ApiDiet = {
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
      meals: [],
      totals: {
        calories: body.calories,
        protein: body.protein,
        carbohydrates: body.carbohydrates,
        fat: body.fat,
      },
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
      return HttpResponse.json(
        { message: "Versão de origem não encontrada." },
        { status: 404 }
      );
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

    const clonedMeals = cloneMeals(source.meals);
    const created: ApiDiet = {
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
      meals: clonedMeals,
    };
    recalculateDietTotals(created);

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

  // CRUD de refeições e itens (T-085 / T-091 / T-093) — retorna ApiDiet atualizado
  http.post("/api/v1/diets/:id/meals", async ({ params, request }) => {
    const diet = mockDb.diets.find((item) => item.id === params.id && !item.deleted);
    if (!diet) {
      return HttpResponse.json({ message: "Dieta não encontrada." }, { status: 404 });
    }
    if (diet.readOnly) {
      return HttpResponse.json(
        { message: "Versão congelada não permite editar refeições." },
        { status: 409 }
      );
    }

    const body = (await request.json()) as AddMealPayload;
    const meals = diet.meals ?? [];
    const createdMeal: Meal = {
      id: nextMockId("meal"),
      name: body.name,
      order: body.order ?? meals.length + 1,
      time: body.time ?? null,
      notes: body.notes ?? null,
      target: body.target ?? null,
      items: [],
      totals: { calories: 0, protein: 0, carbohydrates: 0, fat: 0 },
    };

    diet.meals = [...meals, createdMeal].sort((a, b) => a.order - b.order);
    recalculateDietTotals(diet);
    diet.updatedAt = new Date().toISOString();
    diet.updatedBy = mockDb.coach.id;

    return HttpResponse.json(diet, { status: 201 });
  }),

  http.delete("/api/v1/diets/:id/meals/:mealId", ({ params }) => {
    const diet = mockDb.diets.find((item) => item.id === params.id && !item.deleted);
    if (!diet) {
      return HttpResponse.json({ message: "Dieta não encontrada." }, { status: 404 });
    }
    if (diet.readOnly) {
      return HttpResponse.json(
        { message: "Versão congelada não permite remover refeições." },
        { status: 409 }
      );
    }

    const meals = diet.meals ?? [];
    if (!meals.some((meal) => meal.id === params.mealId)) {
      return HttpResponse.json({ message: "Refeição não encontrada." }, { status: 404 });
    }

    diet.meals = meals.filter((meal) => meal.id !== params.mealId);
    recalculateDietTotals(diet);
    diet.updatedAt = new Date().toISOString();
    diet.updatedBy = mockDb.coach.id;

    return HttpResponse.json(diet);
  }),

  http.post("/api/v1/diets/:id/meals/:mealId/items", async ({ params, request }) => {
    const diet = mockDb.diets.find((item) => item.id === params.id && !item.deleted);
    if (!diet) {
      return HttpResponse.json({ message: "Dieta não encontrada." }, { status: 404 });
    }
    if (diet.readOnly) {
      return HttpResponse.json(
        { message: "Versão congelada não permite adicionar itens." },
        { status: 409 }
      );
    }

    const meal = (diet.meals ?? []).find((item) => item.id === params.mealId);
    if (!meal) {
      return HttpResponse.json({ message: "Refeição não encontrada." }, { status: 404 });
    }

    const body = (await request.json()) as AddMealItemPayload;
    const createdItem: MealItem = {
      id: nextMockId("item"),
      foodId: body.foodId ?? null,
      name: body.name,
      source: body.source,
      kind: body.kind,
      state: body.state,
      quantityG: body.quantityG,
      householdMeasure: body.householdMeasure ?? null,
      preparation: body.preparation ?? null,
      calories: body.calories,
      protein: body.protein,
      carbohydrates: body.carbohydrates,
      fat: body.fat,
    };

    meal.items.push(createdItem);
    recalculateDietTotals(diet);
    diet.updatedAt = new Date().toISOString();
    diet.updatedBy = mockDb.coach.id;

    return HttpResponse.json(diet, { status: 201 });
  }),

  http.delete("/api/v1/diets/:id/meals/:mealId/items/:itemId", ({ params }) => {
    const diet = mockDb.diets.find((item) => item.id === params.id && !item.deleted);
    if (!diet) {
      return HttpResponse.json({ message: "Dieta não encontrada." }, { status: 404 });
    }
    if (diet.readOnly) {
      return HttpResponse.json(
        { message: "Versão congelada não permite remover itens." },
        { status: 409 }
      );
    }

    const meal = (diet.meals ?? []).find((item) => item.id === params.mealId);
    if (!meal) {
      return HttpResponse.json({ message: "Refeição não encontrada." }, { status: 404 });
    }

    meal.items = meal.items.filter((item) => item.id !== params.itemId);
    recalculateDietTotals(diet);
    diet.updatedAt = new Date().toISOString();
    diet.updatedBy = mockDb.coach.id;

    return HttpResponse.json(diet);
  }),
];
