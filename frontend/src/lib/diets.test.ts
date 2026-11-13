import { describe, expect, it } from "vitest";
import {
  ApiDiet,
  buildShoppingList,
  computeDerivedMacros,
  DIET_STATUS_FLOW,
  DietVersion,
  diffDietVersions,
  findCurrentDietVersion,
  FOOD_KIND_LABEL,
  FOOD_SOURCE_LABEL,
  FOOD_STATE_LABEL,
  Meal,
  MealItem,
  toDietVersion,
} from "./diets";

function diet(overrides: Partial<DietVersion> = {}): DietVersion {
  const macros = overrides.macros ?? { calories: 2000, protein: 120, carbohydrates: 220, fat: 65 };
  return {
    id: "diet-1",
    coachId: "coach-1",
    athleteId: "athlete-1",
    objective: "MAINTENANCE",
    macros,
    meals: [],
    totals: macros,
    status: "DRAFT",
    version: 1,
    previousVersionId: null,
    changeReason: null,
    createdBy: "coach-1",
    updatedBy: "coach-1",
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    readOnly: false,
    deleted: false,
    ...overrides,
  };
}

function mealItem(overrides: Partial<MealItem> = {}): MealItem {
  return {
    id: "item-1",
    foodId: "taco:frango",
    name: "Peito de frango",
    source: "TACO",
    kind: "FOOD",
    state: "RAW",
    quantityG: 150,
    householdMeasure: "1 filé médio",
    preparation: "grelhado",
    calories: 240,
    protein: 45,
    carbohydrates: 0,
    fat: 5,
    ...overrides,
  };
}

function meal(overrides: Partial<Meal> = {}): Meal {
  return {
    id: "meal-1",
    name: "Almoço",
    order: 1,
    time: "12:00",
    notes: null,
    target: null,
    items: [],
    totals: null,
    ...overrides,
  };
}

describe("findCurrentDietVersion", () => {
  it("returns the newest editable version regardless of input order", () => {
    const older = diet({ id: "diet-1", version: 1, readOnly: true });
    const current = diet({ id: "diet-3", version: 3, status: "IN_REVIEW" });
    const middle = diet({ id: "diet-2", version: 2, readOnly: true });

    expect(findCurrentDietVersion([older, current, middle])).toEqual(current);
  });

  it("skips deleted and read-only versions when finding the current version", () => {
    const deleted = diet({ id: "deleted", version: 4, deleted: true });
    const historical = diet({ id: "historical", version: 3, readOnly: true });
    const current = diet({ id: "current", version: 2 });

    expect(findCurrentDietVersion([deleted, historical, current])).toEqual(current);
  });

  it("returns undefined when no editable version exists", () => {
    expect(findCurrentDietVersion([
      diet({ readOnly: true }),
      diet({ id: "deleted", version: 2, deleted: true }),
    ])).toBeUndefined();
  });
});

describe("DIET_STATUS_FLOW", () => {
  it.each([
    ["DRAFT", "IN_REVIEW"],
    ["IN_REVIEW", "APPROVED"],
    ["APPROVED", "ARCHIVED"],
    ["ARCHIVED", null],
  ] as const)("maps %s to its only next status", (status, nextStatus) => {
    expect(DIET_STATUS_FLOW[status]).toBe(nextStatus);
  });
});

describe("toDietVersion & labels", () => {
  const baseApiDiet: ApiDiet = {
    id: "diet-1",
    coachId: "coach-1",
    athleteId: "athlete-1",
    objective: "HYPERTROPHY",
    calories: 2500,
    protein: 180,
    carbohydrates: 280,
    fat: 70,
    status: "DRAFT",
    version: 1,
    previousVersionId: null,
    changeReason: null,
    createdAt: "2026-01-01T00:00:00Z",
    updatedAt: "2026-01-01T00:00:00Z",
    readOnly: false,
    deleted: false,
    createdBy: "coach-1",
    updatedBy: "coach-1",
  };

  it("falls back to empty meals and macros as totals when T-085 fields are absent", () => {
    const mapped = toDietVersion(baseApiDiet);
    expect(mapped.meals).toEqual([]);
    expect(mapped.macros).toEqual({
      calories: 2500,
      protein: 180,
      carbohydrates: 280,
      fat: 70,
    });
    expect(mapped.totals).toEqual(mapped.macros);
  });

  it("preserves meals and totals when provided by the API", () => {
    const item = mealItem();
    const m = meal({ items: [item], totals: { calories: 240, protein: 45, carbohydrates: 0, fat: 5 } });
    const totals = { calories: 240, protein: 45, carbohydrates: 0, fat: 5 };
    const mapped = toDietVersion({ ...baseApiDiet, meals: [m], totals });

    expect(mapped.meals).toEqual([m]);
    expect(mapped.totals).toEqual(totals);
  });

  it("exports human-readable labels for food source, kind, and state", () => {
    expect(FOOD_SOURCE_LABEL.TACO).toBe("TACO");
    expect(FOOD_SOURCE_LABEL.CUSTOM).toBeTruthy();
    expect(FOOD_SOURCE_LABEL.USDA).toBe("USDA");
    expect(FOOD_KIND_LABEL.FOOD).toBeTruthy();
    expect(FOOD_KIND_LABEL.SUPPLEMENT_CALORIC).toBeTruthy();
    expect(FOOD_STATE_LABEL.RAW).toBeTruthy();
    expect(FOOD_STATE_LABEL.AS_SERVED).toBeTruthy();
  });
});

describe("computeDerivedMacros", () => {
  it("computes kcalFromMacros, macro percentages, and per-kg metrics", () => {
    // 150*4 + 200*4 + 50*9 = 600 + 800 + 450 = 1850 kcal
    const derived = computeDerivedMacros(
      { calories: 1850, protein: 150, carbohydrates: 200, fat: 50 },
      75
    );

    expect(derived.kcalFromMacros).toBe(1850);
    expect(derived.calorieDeltaPct).toBeCloseTo(0);
    expect(derived.hasCalorieInconsistency).toBe(false);
    expect(derived.zeroMacros).toEqual([]);
    expect(derived.proteinPct).toBeCloseTo((600 / 1850) * 100);
    expect(derived.carbsPct).toBeCloseTo((800 / 1850) * 100);
    expect(derived.fatPct).toBeCloseTo((450 / 1850) * 100);
    expect(derived.proteinPerKg).toBeCloseTo(2);
    expect(derived.carbsPerKg).toBeCloseTo(200 / 75);
    expect(derived.fatPerKg).toBeCloseTo(50 / 75);
    expect(derived.kcalPerKg).toBeCloseTo(1850 / 75);
  });

  it("flags calorie inconsistency when delta exceeds 5% and macros > 0", () => {
    const derived = computeDerivedMacros({
      calories: 2000,
      protein: 200,
      carbohydrates: 200,
      fat: 100, // 800 + 800 + 900 = 2500 kcal -> +25%
    });

    expect(derived.kcalFromMacros).toBe(2500);
    expect(derived.calorieDeltaPct).toBeCloseTo(25);
    expect(derived.hasCalorieInconsistency).toBe(true);
  });

  it("does not flag inconsistency when macros are all zero", () => {
    const derived = computeDerivedMacros(
      { calories: 0, protein: 0, carbohydrates: 0, fat: 0 },
      null
    );

    expect(derived.kcalFromMacros).toBe(0);
    expect(derived.hasCalorieInconsistency).toBe(false);
    expect(derived.zeroMacros).toEqual(["protein", "carbohydrates", "fat"]);
    expect(derived.proteinPct).toBe(0);
    expect(derived.proteinPerKg).toBeUndefined();
  });

  it("identifies individual zero macros and omits per-kg when weightKg <= 0", () => {
    const derived = computeDerivedMacros(
      { calories: 800, protein: 100, carbohydrates: 100, fat: 0 },
      0
    );

    expect(derived.zeroMacros).toEqual(["fat"]);
    expect(derived.kcalPerKg).toBeUndefined();
  });
});

describe("diffDietVersions", () => {
  it("computes macro deltas and meal/item diffs across versions", () => {
    const prev = diet({
      version: 1,
      totals: { calories: 2000, protein: 150, carbohydrates: 200, fat: 66 },
      meals: [
        meal({
          id: "m1-v1",
          name: "Café da manhã",
          order: 0,
          items: [
            mealItem({ id: "i1-v1", name: "Aveia", quantityG: 50, calories: 190, protein: 7, carbohydrates: 33, fat: 3 }),
            mealItem({ id: "i2-v1", name: "Banana", quantityG: 100, calories: 89, protein: 1, carbohydrates: 23, fat: 0 }),
          ],
        }),
        meal({
          id: "m2-v1",
          name: "Ceia",
          order: 2,
          items: [mealItem({ id: "i3-v1", name: "Iogurte", quantityG: 170, calories: 100, protein: 10, carbohydrates: 8, fat: 3 })],
        }),
      ],
    });

    const curr = diet({
      version: 2,
      totals: { calories: 2200, protein: 170, carbohydrates: 220, fat: 70 },
      meals: [
        meal({
          id: "m1-v2",
          name: "Café da manhã",
          order: 0,
          items: [
            mealItem({ id: "i1-v2", name: "Aveia", quantityG: 80, calories: 304, protein: 11, carbohydrates: 53, fat: 5 }),
            mealItem({ id: "i4-v2", name: "Whey", kind: "SUPPLEMENT_CALORIC", quantityG: 30, calories: 120, protein: 24, carbohydrates: 2, fat: 1 }),
          ],
        }),
        meal({
          id: "m3-v2",
          name: "Almoço",
          order: 1,
          items: [mealItem({ id: "i5-v2", name: "Arroz", quantityG: 200, calories: 260, protein: 5, carbohydrates: 56, fat: 1 })],
        }),
      ],
    });

    const diff = diffDietVersions(prev, curr);

    expect(diff.previousVersion).toBe(1);
    expect(diff.currentVersion).toBe(2);
    expect(diff.macroDeltas).toEqual({
      calories: 200,
      protein: 20,
      carbohydrates: 20,
      fat: 4,
    });

    expect(diff.meals).toHaveLength(3);
    expect(diff.meals[0].name).toBe("Café da manhã");
    expect(diff.meals[0].status).toBe("CHANGED");
    expect(diff.meals[0].items.map((i) => [i.name, i.status])).toEqual([
      ["Aveia", "CHANGED"],
      ["Whey", "ADDED"],
      ["Banana", "REMOVED"],
    ]);
    expect(diff.meals[0].items[0].quantityDeltaG).toBe(30);

    expect(diff.meals[1].name).toBe("Almoço");
    expect(diff.meals[1].status).toBe("ADDED");
    expect(diff.meals[1].items[0].status).toBe("ADDED");

    expect(diff.meals[2].name).toBe("Ceia");
    expect(diff.meals[2].status).toBe("REMOVED");
    expect(diff.meals[2].items[0].status).toBe("REMOVED");
  });

  it("marks identical meals and items as UNCHANGED", () => {
    const sharedMeal = meal({
      id: "m1",
      name: "Almoço",
      order: 1,
      items: [mealItem({ id: "i1", name: "Frango", quantityG: 150 })],
    });
    const prev = diet({ version: 1, meals: [sharedMeal] });
    const curr = diet({ version: 2, meals: [sharedMeal] });

    const diff = diffDietVersions(prev, curr);
    expect(diff.meals[0].status).toBe("UNCHANGED");
    expect(diff.meals[0].items[0].status).toBe("UNCHANGED");
  });
});

describe("buildShoppingList", () => {
  it("groups FOOD items by normalized name + state and separates SUPPLEMENT_CALORIC", () => {
    const meals: Meal[] = [
      meal({
        id: "m1",
        name: "Almoço",
        order: 1,
        items: [
          mealItem({
            id: "i1",
            name: "  Peito de   Frango ",
            state: "RAW",
            kind: "FOOD",
            quantityG: 150,
            householdMeasure: "1 filé",
          }),
          mealItem({
            id: "i2",
            name: "Whey Protein",
            state: "AS_SERVED",
            kind: "SUPPLEMENT_CALORIC",
            quantityG: 30,
            householdMeasure: "1 scoop",
          }),
        ],
      }),
      meal({
        id: "m2",
        name: "Jantar",
        order: 2,
        items: [
          mealItem({
            id: "i3",
            name: "peito de frango",
            state: "RAW",
            kind: "FOOD",
            quantityG: 200,
            householdMeasure: "2 filés pequenos",
          }),
          mealItem({
            id: "i4",
            name: "Peito de Frango",
            state: "AS_SERVED",
            kind: "FOOD",
            quantityG: 120,
            householdMeasure: "1 filé",
          }),
          mealItem({
            id: "i5",
            name: "whey protein",
            state: "AS_SERVED",
            kind: "SUPPLEMENT_CALORIC",
            quantityG: 30,
            householdMeasure: "1 scoop",
          }),
        ],
      }),
    ];

    const list = buildShoppingList(meals);

    expect(list.foods).toHaveLength(2);
    const rawChicken = list.foods.find((f) => f.state === "RAW");
    const servedChicken = list.foods.find((f) => f.state === "AS_SERVED");

    expect(rawChicken).toMatchObject({
      key: "peito de frango::RAW",
      state: "RAW",
      kind: "FOOD",
      totalQuantityG: 350,
      householdMeasures: ["1 filé", "2 filés pequenos"],
      occurrences: 2,
    });
    expect(servedChicken).toMatchObject({
      key: "peito de frango::AS_SERVED",
      state: "AS_SERVED",
      kind: "FOOD",
      totalQuantityG: 120,
      householdMeasures: ["1 filé"],
      occurrences: 1,
    });

    expect(list.supplements).toHaveLength(1);
    expect(list.supplements[0]).toMatchObject({
      key: "whey protein::AS_SERVED",
      kind: "SUPPLEMENT_CALORIC",
      totalQuantityG: 60,
      householdMeasures: ["1 scoop"],
      occurrences: 2,
    });
  });
});
