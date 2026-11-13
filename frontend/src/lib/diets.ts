import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiClient } from "./api-client";

/**
 * E3 — Dietas versionadas (US-011, US-014, US-015, US-016, US-017).
 *
 * Contrato alinhado ao backend (DietController, /api/v1/diets): a criação
 * recebe athleteId no corpo, a listagem usa /diets/history?athleteId= e a
 * resposta traz os macros planos (calories/protein/...). Este módulo traduz
 * para o formato da UI (`macros` aninhado) nas bordas.
 * Os nomes de campos espelham o agregado de domínio
 * (com.fitness.procoach.nutrition: Diet + Macronutrients, T-050) e as regras
 * que ele impõe:
 *
 * - nunca sobrescrever (US-015 / regra inegociável 3): este módulo só expõe
 *   criação da v1 e criação de nova versão a partir de uma versão existente;
 *   não existe PUT/PATCH/DELETE aqui. No domínio, criar nova versão congela
 *   a anterior (readOnly) e exige changeReason;
 * - fluxo de status travado (US-017): DRAFT -> IN_REVIEW -> APPROVED ->
 *   ARCHIVED (terminal). DIET_STATUS_FLOW espelha a máquina de estados do
 *   domínio — a UI só oferece a transição seguinte e bloqueia as demais;
 * - auditoria (US-016): cada versão traz quem criou (createdBy), quando
 *   (createdAt) e o motivo (changeReason). createdBy/updatedBy são UUIDs de
 *   usuário: o núcleo não expõe nomes — a tela exibe o ID (mono).
 *
 * O domínio guarda apenas os totais de macronutrientes — sem refeições ou
 * alimentos por enquanto.
 */

export const DIET_OBJECTIVES = [
  "CUTTING",
  "HYPERTROPHY",
  "MAINTENANCE",
  "BULKING",
  "WEIGHT_LOSS",
] as const;

export type DietObjective = (typeof DIET_OBJECTIVES)[number];

export type DietStatus = "DRAFT" | "IN_REVIEW" | "APPROVED" | "ARCHIVED";

export type Macronutrients = {
  calories: number; // kcal
  protein: number; // g
  carbohydrates: number; // g
  fat: number; // g
};

export type FoodSource = "TACO" | "CUSTOM" | "USDA";
export type FoodKind = "FOOD" | "SUPPLEMENT_CALORIC";
export type FoodState = "RAW" | "AS_SERVED";

export type MealTarget = {
  calories: number;
  protein: number;
  carbohydrates: number;
  fat: number;
};

export type MealItem = {
  id: string;
  foodId: string | null;
  name: string;
  source: FoodSource;
  kind: FoodKind;
  state: FoodState;
  quantityG: number;
  householdMeasure: string | null;
  preparation: string | null;
  calories: number;
  protein: number;
  carbohydrates: number;
  fat: number;
};

export type Meal = {
  id: string;
  name: string;
  order: number;
  time: string | null;
  notes: string | null;
  target: MealTarget | null;
  items: MealItem[];
  totals: Macronutrients | null;
};

// Uma linha = uma versão da dieta (v1, v2, ...). No domínio cada versão é
// uma instância do agregado Diet encadeada por previousVersionId.
export type DietVersion = {
  id: string;
  coachId: string; // UUID do coach (tenant) — espelho de CoachId
  athleteId: string; // UUID do atleta — espelho de AthleteId
  objective: DietObjective;
  macros: Macronutrients;
  meals: Meal[];
  totals: Macronutrients;
  status: DietStatus;
  version: number; // v1 = 1
  previousVersionId: string | null; // null na v1
  changeReason: string | null; // motivo da alteração (auditoria, US-016); null na v1
  createdBy: string; // UUID de quem criou ESTA versão
  updatedBy: string; // UUID do último usuário que tocou o registro
  createdAt: string; // ISO-8601 (Instant do domínio)
  updatedAt: string; // ISO-8601 (Instant do domínio)
  readOnly: boolean; // versão anterior já congelada — imutável
  deleted: boolean; // soft-delete (regra inegociável 5) — nunca renderizar
};

export type CreateDietPayload = {
  objective: DietObjective;
  macros: Macronutrients;
};

export type CreateDietVersionPayload = CreateDietPayload & {
  changeReason: string; // obrigatório no domínio (auditoria, US-016)
};

export type ChangeDietStatusPayload = {
  status: DietStatus;
};

export type AddMealPayload = {
  name: string;
  order: number;
  time?: string | null;
  notes?: string | null;
  target?: MealTarget | null;
};

export type AddMealItemPayload = {
  foodId?: string | null;
  name: string;
  source: FoodSource;
  kind: FoodKind;
  state: FoodState;
  quantityG: number;
  householdMeasure?: string | null;
  preparation?: string | null;
  calories: number;
  protein: number;
  carbohydrates: number;
  fat: number;
};

// Espelho client-side da máquina de estados do domínio (US-017, critério 2:
// transições inválidas bloqueadas). Sempre oferecer no máximo a PRÓXIMA
// transição; ARCHIVED é terminal.
export const DIET_STATUS_FLOW: Record<DietStatus, DietStatus | null> = {
  DRAFT: "IN_REVIEW",
  IN_REVIEW: "APPROVED",
  APPROVED: "ARCHIVED",
  ARCHIVED: null,
};

export const OBJECTIVE_LABEL: Record<DietObjective, string> = {
  CUTTING: "Cutting",
  HYPERTROPHY: "Hipertrofia",
  MAINTENANCE: "Manutenção",
  BULKING: "Bulking",
  WEIGHT_LOSS: "Perda de peso",
};

export const STATUS_LABEL: Record<DietStatus, string> = {
  DRAFT: "Rascunho",
  IN_REVIEW: "Em revisão",
  APPROVED: "Aprovada",
  ARCHIVED: "Arquivada",
};

export const FOOD_SOURCE_LABEL: Record<FoodSource, string> = {
  TACO: "TACO",
  CUSTOM: "Personalizado",
  USDA: "USDA",
};

export const FOOD_KIND_LABEL: Record<FoodKind, string> = {
  FOOD: "Alimento",
  SUPPLEMENT_CALORIC: "Suplemento calórico",
};

export const FOOD_STATE_LABEL: Record<FoodState, string> = {
  RAW: "Cru",
  AS_SERVED: "Como servido",
};

// Formato do backend (DietResponse).
export type ApiDiet = {
  id: string;
  coachId: string;
  athleteId: string;
  objective: DietObjective;
  calories: number;
  protein: number;
  carbohydrates: number;
  fat: number;
  status: DietStatus;
  version: number;
  previousVersionId: string | null;
  changeReason: string | null;
  createdAt: string;
  updatedAt: string;
  readOnly: boolean;
  deleted: boolean;
  createdBy: string;
  updatedBy: string;
  meals?: Meal[];
  totals?: Macronutrients;
};

export function toDietVersion(d: ApiDiet): DietVersion {
  const { calories, protein, carbohydrates, fat, meals, totals, ...rest } = d;
  const macros: Macronutrients = { calories, protein, carbohydrates, fat };
  return {
    ...rest,
    macros,
    meals: meals ?? [],
    totals: totals ?? macros,
  };
}

function toApiBody(payload: CreateDietPayload) {
  return { objective: payload.objective, ...payload.macros };
}

// --- Chamadas ----------------------------------------------------------------

async function listDiets(athleteId: string): Promise<DietVersion[]> {
  const { data } = await apiClient.get<ApiDiet[]>("/diets/history", {
    params: { athleteId },
  });
  return data.map(toDietVersion);
}

async function getCurrentAthleteDiet(athleteId: string): Promise<DietVersion | null> {
  try {
    const { data } = await apiClient.get<ApiDiet>("/diets", { params: { athleteId } });
    return toDietVersion(data);
  } catch (error) {
    // For an athlete, this endpoint returns 404 when no approved diet exists.
    // Keep authorization and session errors visible to the page.
    if (error && typeof error === "object" && "response" in error) {
      const response = (error as { response?: { status?: number } }).response;
      if (response?.status === 404) return null;
    }
    throw error;
  }
}

async function createDiet(
  athleteId: string,
  payload: CreateDietPayload
): Promise<DietVersion> {
  const { data } = await apiClient.post<ApiDiet>("/diets", {
    athleteId,
    ...toApiBody(payload),
  });
  return toDietVersion(data);
}

// Cria a próxima versão a partir de `versionId`; o backend congela a versão
// de origem (readOnly). Nunca é um update — é um POST de nova versão.
async function createDietVersion(
  _athleteId: string,
  versionId: string,
  payload: CreateDietVersionPayload
): Promise<DietVersion> {
  const { data } = await apiClient.post<ApiDiet>(`/diets/${versionId}/versions`, {
    ...toApiBody(payload),
    changeReason: payload.changeReason,
  });
  return toDietVersion(data);
}

async function changeDietStatus(
  _athleteId: string,
  versionId: string,
  payload: ChangeDietStatusPayload
): Promise<DietVersion> {
  const { data } = await apiClient.post<ApiDiet>(`/diets/${versionId}/status`, payload);
  return toDietVersion(data);
}

// --- Balanço energético (T-030) ---------------------------------------------

export type EnergyClassification = "DEFICIT" | "MAINTENANCE" | "SURPLUS";

// Calorias da dieta atual contra o GET da avaliação mais recente. Calculado
// pelo núcleo (GET /diets/energy-balance?athleteId=) — o front só exibe.
export type EnergyBalance = {
  dietId: string;
  calories: number;
  tmbKcal: number;
  getKcal: number;
  balanceKcal: number; // calorias da dieta - GET; negativo = déficit
  balancePercent: number;
  classification: EnergyClassification;
  basedOnAssessmentDate: string; // ISO yyyy-mm-dd
  weightKg: number;
};

export const CLASSIFICATION_LABEL: Record<EnergyClassification, string> = {
  DEFICIT: "Déficit",
  MAINTENANCE: "Manutenção",
  SURPLUS: "Superávit",
};

async function getEnergyBalance(athleteId: string): Promise<EnergyBalance> {
  const { data } = await apiClient.get<EnergyBalance>("/diets/energy-balance", {
    params: { athleteId },
  });
  return data;
}

async function getDietById(dietId: string): Promise<DietVersion> {
  const { data } = await apiClient.get<ApiDiet>(`/diets/${dietId}`);
  return toDietVersion(data);
}

async function getDietEnergyBalance(dietId: string): Promise<EnergyBalance> {
  const { data } = await apiClient.get<EnergyBalance>(`/diets/${dietId}/energy-balance`);
  return data;
}

async function addMeal(dietId: string, payload: AddMealPayload): Promise<DietVersion> {
  const { data } = await apiClient.post<ApiDiet>(`/diets/${dietId}/meals`, payload);
  return toDietVersion(data);
}

async function addMealItem(
  dietId: string,
  mealId: string,
  payload: AddMealItemPayload
): Promise<DietVersion> {
  const { data } = await apiClient.post<ApiDiet>(
    `/diets/${dietId}/meals/${mealId}/items`,
    payload
  );
  return toDietVersion(data);
}

async function removeMealItem(
  dietId: string,
  mealId: string,
  itemId: string
): Promise<DietVersion> {
  const { data } = await apiClient.delete<ApiDiet>(
    `/diets/${dietId}/meals/${mealId}/items/${itemId}`
  );
  return toDietVersion(data);
}

async function removeMeal(dietId: string, mealId: string): Promise<DietVersion> {
  const { data } = await apiClient.delete<ApiDiet>(`/diets/${dietId}/meals/${mealId}`);
  return toDietVersion(data);
}

// Versão corrente da cadeia: a única que o domínio permite mutar (não está
// readOnly nem deleted). Na prática é sempre a de maior número — v(n) vira
// readOnly assim que v(n+1) é criada a partir dela.
export function findCurrentDietVersion(
  versions: DietVersion[]
): DietVersion | undefined {
  return [...versions]
    .sort((a, b) => b.version - a.version)
    .find((v) => !v.readOnly && !v.deleted);
}

// --- Hooks -----------------------------------------------------------------

export function useDiets(athleteId: string) {
  return useQuery({
    queryKey: ["diets", athleteId],
    queryFn: () => listDiets(athleteId),
    enabled: Boolean(athleteId),
  });
}

export function useCurrentAthleteDiet(athleteId: string) {
  return useQuery({
    queryKey: ["diets", "current-athlete", athleteId],
    queryFn: () => getCurrentAthleteDiet(athleteId),
    enabled: Boolean(athleteId),
    retry: false,
  });
}

export function useCreateDiet(athleteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateDietPayload) => createDiet(athleteId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["diets", athleteId] });
    },
  });
}

export function useCreateDietVersion(athleteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      versionId,
      payload,
    }: {
      versionId: string;
      payload: CreateDietVersionPayload;
    }) => createDietVersion(athleteId, versionId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["diets", athleteId] });
    },
  });
}

export function useChangeDietStatus(athleteId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({
      versionId,
      payload,
    }: {
      versionId: string;
      payload: ChangeDietStatusPayload;
    }) => changeDietStatus(athleteId, versionId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["diets", athleteId] });
    },
  });
}

// 404 (atleta sem dieta) e 409 (sem avaliação) são estados esperados da tela,
// não falhas transitórias: sem retry.
export function useEnergyBalance(athleteId: string) {
  return useQuery({
    queryKey: ["diets", "energy-balance", athleteId],
    queryFn: () => getEnergyBalance(athleteId),
    enabled: Boolean(athleteId),
    retry: false,
  });
}

export function useDiet(dietId: string) {
  return useQuery({
    queryKey: ["diets", "detail", dietId],
    queryFn: () => getDietById(dietId),
    enabled: Boolean(dietId),
  });
}

// 404 (dieta não encontrada) e 409 (sem avaliação) são estados esperados da tela.
export function useDietEnergyBalance(dietId: string) {
  return useQuery({
    queryKey: ["diets", "energy-balance", "diet", dietId],
    queryFn: () => getDietEnergyBalance(dietId),
    enabled: Boolean(dietId),
    retry: false,
  });
}

function invalidateDietQueries(
  queryClient: ReturnType<typeof useQueryClient>,
  athleteId: string,
  dietId: string
) {
  queryClient.invalidateQueries({ queryKey: ["diets", athleteId] });
  queryClient.invalidateQueries({ queryKey: ["diets", "detail", dietId] });
  queryClient.invalidateQueries({ queryKey: ["diets", "energy-balance", athleteId] });
  queryClient.invalidateQueries({ queryKey: ["diets", "energy-balance", "diet", dietId] });
}

export function useAddMeal(athleteId: string, dietId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: AddMealPayload) => addMeal(dietId, payload),
    onSuccess: () => invalidateDietQueries(queryClient, athleteId, dietId),
  });
}

export function useAddMealItem(athleteId: string, dietId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ mealId, payload }: { mealId: string; payload: AddMealItemPayload }) =>
      addMealItem(dietId, mealId, payload),
    onSuccess: () => invalidateDietQueries(queryClient, athleteId, dietId),
  });
}

export function useRemoveMealItem(athleteId: string, dietId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ mealId, itemId }: { mealId: string; itemId: string }) =>
      removeMealItem(dietId, mealId, itemId),
    onSuccess: () => invalidateDietQueries(queryClient, athleteId, dietId),
  });
}

export function useRemoveMeal(athleteId: string, dietId: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (mealId: string) => removeMeal(dietId, mealId),
    onSuccess: () => invalidateDietQueries(queryClient, athleteId, dietId),
  });
}

// --- Helpers puros (T-085 / T-092 / T-095) -----------------------------------

export type MacroKey = "protein" | "carbohydrates" | "fat";

export type DerivedMacros = {
  kcalFromMacros: number;
  calorieDeltaPct: number;
  hasCalorieInconsistency: boolean;
  zeroMacros: MacroKey[];
  proteinPct: number;
  carbsPct: number;
  fatPct: number;
  proteinPerKg?: number;
  carbsPerKg?: number;
  fatPerKg?: number;
  kcalPerKg?: number;
};

/**
 * Calcula métricas derivadas de macronutrientes: calorias pela regra 4/4/9,
 * divergência percentual em relação às calorias informadas, macros zerados,
 * distribuição percentual e, opcionalmente, valores por kg de peso corporal.
 */
export function computeDerivedMacros(
  macros: Macronutrients,
  weightKg?: number | null
): DerivedMacros {
  const proteinKcal = macros.protein * 4;
  const carbsKcal = macros.carbohydrates * 4;
  const fatKcal = macros.fat * 9;
  const kcalFromMacros = proteinKcal + carbsKcal + fatKcal;

  const calorieDeltaPct =
    macros.calories > 0
      ? ((kcalFromMacros - macros.calories) / macros.calories) * 100
      : kcalFromMacros > 0
        ? 100
        : 0;

  const hasCalorieInconsistency =
    kcalFromMacros > 0 && (macros.calories <= 0 || Math.abs(calorieDeltaPct) > 5);

  const zeroMacros = (["protein", "carbohydrates", "fat"] as const).filter(
    (key) => macros[key] === 0
  );

  const proteinPct = kcalFromMacros > 0 ? (proteinKcal / kcalFromMacros) * 100 : 0;
  const carbsPct = kcalFromMacros > 0 ? (carbsKcal / kcalFromMacros) * 100 : 0;
  const fatPct = kcalFromMacros > 0 ? (fatKcal / kcalFromMacros) * 100 : 0;

  const result: DerivedMacros = {
    kcalFromMacros,
    calorieDeltaPct,
    hasCalorieInconsistency,
    zeroMacros,
    proteinPct,
    carbsPct,
    fatPct,
  };

  if (weightKg != null && weightKg > 0) {
    result.proteinPerKg = macros.protein / weightKg;
    result.carbsPerKg = macros.carbohydrates / weightKg;
    result.fatPerKg = macros.fat / weightKg;
    result.kcalPerKg = macros.calories / weightKg;
  }

  return result;
}

export type DiffStatus = "ADDED" | "REMOVED" | "CHANGED" | "UNCHANGED";

export type MealItemDiff = {
  status: DiffStatus;
  name: string;
  previous: MealItem | null;
  current: MealItem | null;
  quantityDeltaG: number;
  caloriesDelta: number;
  proteinDelta: number;
  carbohydratesDelta: number;
  fatDelta: number;
};

export type MealDiff = {
  status: DiffStatus;
  name: string;
  order: number;
  previous: Meal | null;
  current: Meal | null;
  items: MealItemDiff[];
};

export type DietVersionDiff = {
  previousVersion: number;
  currentVersion: number;
  macroDeltas: Macronutrients;
  meals: MealDiff[];
};

function normalizeText(value: string): string {
  return value.trim().toLowerCase().replace(/\s+/g, " ");
}

function targetsEqual(a: MealTarget | null, b: MealTarget | null): boolean {
  if (a === b) return true;
  if (!a || !b) return false;
  return (
    a.calories === b.calories &&
    a.protein === b.protein &&
    a.carbohydrates === b.carbohydrates &&
    a.fat === b.fat
  );
}

function mealItemsEqual(a: MealItem, b: MealItem): boolean {
  return (
    a.name === b.name &&
    a.source === b.source &&
    a.kind === b.kind &&
    a.state === b.state &&
    a.quantityG === b.quantityG &&
    a.householdMeasure === b.householdMeasure &&
    a.preparation === b.preparation &&
    a.calories === b.calories &&
    a.protein === b.protein &&
    a.carbohydrates === b.carbohydrates &&
    a.fat === b.fat
  );
}

function findMatchingItemIndex(
  target: MealItem,
  candidates: MealItem[],
  usedIndices: Set<number>
): number {
  const byId = candidates.findIndex((c, idx) => !usedIndices.has(idx) && c.id === target.id);
  if (byId !== -1) return byId;

  if (target.foodId) {
    const byFoodId = candidates.findIndex(
      (c, idx) =>
        !usedIndices.has(idx) &&
        c.foodId === target.foodId &&
        c.state === target.state &&
        c.kind === target.kind
    );
    if (byFoodId !== -1) return byFoodId;
  }

  const normalized = normalizeText(target.name);
  return candidates.findIndex(
    (c, idx) =>
      !usedIndices.has(idx) &&
      normalizeText(c.name) === normalized &&
      c.state === target.state &&
      c.kind === target.kind
  );
}

function diffMealItems(prevItems: MealItem[], currItems: MealItem[]): MealItemDiff[] {
  const usedPrev = new Set<number>();
  const diffs: MealItemDiff[] = [];

  for (const curr of currItems) {
    const matchIdx = findMatchingItemIndex(curr, prevItems, usedPrev);
    if (matchIdx === -1) {
      diffs.push({
        status: "ADDED",
        name: curr.name,
        previous: null,
        current: curr,
        quantityDeltaG: curr.quantityG,
        caloriesDelta: curr.calories,
        proteinDelta: curr.protein,
        carbohydratesDelta: curr.carbohydrates,
        fatDelta: curr.fat,
      });
    } else {
      usedPrev.add(matchIdx);
      const prev = prevItems[matchIdx];
      diffs.push({
        status: mealItemsEqual(prev, curr) ? "UNCHANGED" : "CHANGED",
        name: curr.name,
        previous: prev,
        current: curr,
        quantityDeltaG: curr.quantityG - prev.quantityG,
        caloriesDelta: curr.calories - prev.calories,
        proteinDelta: curr.protein - prev.protein,
        carbohydratesDelta: curr.carbohydrates - prev.carbohydrates,
        fatDelta: curr.fat - prev.fat,
      });
    }
  }

  prevItems.forEach((prev, idx) => {
    if (!usedPrev.has(idx)) {
      diffs.push({
        status: "REMOVED",
        name: prev.name,
        previous: prev,
        current: null,
        quantityDeltaG: -prev.quantityG,
        caloriesDelta: -prev.calories,
        proteinDelta: -prev.protein,
        carbohydratesDelta: -prev.carbohydrates,
        fatDelta: -prev.fat,
      });
    }
  });

  return diffs;
}

function findMatchingMealIndex(
  target: Meal,
  candidates: Meal[],
  usedIndices: Set<number>
): number {
  const byId = candidates.findIndex((c, idx) => !usedIndices.has(idx) && c.id === target.id);
  if (byId !== -1) return byId;

  const normalized = normalizeText(target.name);
  const byNameAndOrder = candidates.findIndex(
    (c, idx) =>
      !usedIndices.has(idx) &&
      normalizeText(c.name) === normalized &&
      c.order === target.order
  );
  if (byNameAndOrder !== -1) return byNameAndOrder;

  return candidates.findIndex(
    (c, idx) => !usedIndices.has(idx) && normalizeText(c.name) === normalized
  );
}

/**
 * Compara duas versões de dieta (`previous` e `current`), retornando os deltas
 * de macronutrientes e o diff refeição a refeição e item a item (T-092).
 */
export function diffDietVersions(
  previous: DietVersion,
  current: DietVersion
): DietVersionDiff {
  const prevTotals = previous.totals ?? previous.macros;
  const currTotals = current.totals ?? current.macros;
  const macroDeltas: Macronutrients = {
    calories: currTotals.calories - prevTotals.calories,
    protein: currTotals.protein - prevTotals.protein,
    carbohydrates: currTotals.carbohydrates - prevTotals.carbohydrates,
    fat: currTotals.fat - prevTotals.fat,
  };

  const prevMeals = previous.meals ?? [];
  const currMeals = current.meals ?? [];
  const usedPrev = new Set<number>();
  const meals: MealDiff[] = [];

  for (const curr of currMeals) {
    const matchIdx = findMatchingMealIndex(curr, prevMeals, usedPrev);
    if (matchIdx === -1) {
      meals.push({
        status: "ADDED",
        name: curr.name,
        order: curr.order,
        previous: null,
        current: curr,
        items: diffMealItems([], curr.items),
      });
    } else {
      usedPrev.add(matchIdx);
      const prev = prevMeals[matchIdx];
      const itemDiffs = diffMealItems(prev.items, curr.items);
      const metadataChanged =
        prev.name !== curr.name ||
        prev.order !== curr.order ||
        prev.time !== curr.time ||
        prev.notes !== curr.notes ||
        !targetsEqual(prev.target, curr.target);
      const itemsChanged = itemDiffs.some((item) => item.status !== "UNCHANGED");

      meals.push({
        status: metadataChanged || itemsChanged ? "CHANGED" : "UNCHANGED",
        name: curr.name,
        order: curr.order,
        previous: prev,
        current: curr,
        items: itemDiffs,
      });
    }
  }

  prevMeals.forEach((prev, idx) => {
    if (!usedPrev.has(idx)) {
      meals.push({
        status: "REMOVED",
        name: prev.name,
        order: prev.order,
        previous: prev,
        current: null,
        items: diffMealItems(prev.items, []),
      });
    }
  });

  meals.sort((a, b) => a.order - b.order);

  return {
    previousVersion: previous.version,
    currentVersion: current.version,
    macroDeltas,
    meals,
  };
}

export type ShoppingListItem = {
  key: string;
  name: string;
  state: FoodState;
  kind: FoodKind;
  totalQuantityG: number;
  householdMeasures: string[];
  occurrences: number;
};

export type ShoppingList = {
  foods: ShoppingListItem[];
  supplements: ShoppingListItem[];
};

function groupShoppingItems(items: MealItem[]): ShoppingListItem[] {
  const grouped = new Map<string, ShoppingListItem>();

  for (const item of items) {
    const key = `${normalizeText(item.name)}::${item.state}`;
    const existing = grouped.get(key);
    const measure = item.householdMeasure?.trim();

    if (existing) {
      existing.totalQuantityG += item.quantityG;
      existing.occurrences += 1;
      if (measure && !existing.householdMeasures.includes(measure)) {
        existing.householdMeasures.push(measure);
      }
    } else {
      grouped.set(key, {
        key,
        name: item.name.trim(),
        state: item.state,
        kind: item.kind,
        totalQuantityG: item.quantityG,
        householdMeasures: measure ? [measure] : [],
        occurrences: 1,
      });
    }
  }

  return Array.from(grouped.values()).sort((a, b) => a.name.localeCompare(b.name));
}

/**
 * Gera a lista de compras consolidada a partir das refeições (T-095): agrupa
 * alimentos (`FOOD`) por nome normalizado + estado somando `quantityG` e
 * reunindo medidas caseiras, e separa suplementos calóricos (`SUPPLEMENT_CALORIC`).
 */
export function buildShoppingList(meals: Meal[]): ShoppingList {
  const allItems = meals.flatMap((meal) => meal.items);
  const foodItems = allItems.filter((item) => item.kind === "FOOD");
  const supplementItems = allItems.filter((item) => item.kind === "SUPPLEMENT_CALORIC");

  return {
    foods: groupShoppingItems(foodItems),
    supplements: groupShoppingItems(supplementItems),
  };
}
