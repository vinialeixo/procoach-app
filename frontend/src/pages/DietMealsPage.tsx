import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useParams } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { COACH_NAV } from "@/lib/coach-nav";
import { Field } from "@/components/ui/Field";
import { SelectField } from "@/components/ui/SelectField";
import { Button } from "@/components/ui/Button";
import { ButtonLink } from "@/components/ui/ButtonLink";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { useAthlete } from "@/lib/athletes";
import {
  AddMealItemPayload,
  AddMealPayload,
  FOOD_KIND_LABEL,
  FOOD_SOURCE_LABEL,
  FoodKind,
  FoodSource,
  FoodState,
  Meal,
  MealItem,
  OBJECTIVE_LABEL,
  STATUS_LABEL,
  computeDerivedMacros,
  useAddMeal,
  useAddMealItem,
  useDiet,
  useRemoveMeal,
  useRemoveMealItem,
} from "@/lib/diets";
import "./AthleteForm.css";
import "./AthleteDiets.css";
import "./DietEditor.css";

// T-093 — Construtor de refeições e alimentos por versão de dieta.
// Regra US-015: apenas versões em Rascunho (DRAFT) e não congeladas (readOnly = false)
// podem ter refeições ou itens adicionados/removidos.

const STATE_OPTION_LABEL: Record<FoodState, string> = {
  RAW: "Peso cru",
  AS_SERVED: "Como servido",
};

function formatNumber(value: number, suffix = ""): string {
  return `${value.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}${suffix}`;
}

const nonNegativeField = z.coerce
  .number({ error: "Valor inválido" })
  .min(0, "Deve ser >= 0");

const addMealSchema = z.object({
  name: z.string().trim().min(1, "Informe o nome da refeição").max(100, "Máximo 100 caracteres"),
  order: z.coerce.number({ error: "Ordem inválida" }).int("Deve ser inteiro").min(1, "Mínimo 1"),
  time: z.string().optional(),
  notes: z.string().optional(),
  targetCalories: z.string().optional(),
  targetProtein: z.string().optional(),
  targetCarbohydrates: z.string().optional(),
  targetFat: z.string().optional(),
});
type AddMealInput = z.input<typeof addMealSchema>;
type AddMealValues = z.output<typeof addMealSchema>;

const addItemSchema = z.object({
  name: z.string().trim().min(1, "Informe o nome do alimento").max(200, "Máximo 200 caracteres"),
  source: z.enum(["TACO", "CUSTOM", "USDA"]),
  kind: z.enum(["FOOD", "SUPPLEMENT_CALORIC"]),
  state: z.enum(["RAW", "AS_SERVED"]),
  quantityG: z.coerce.number({ error: "Valor inválido" }).positive("Deve ser maior que zero"),
  householdMeasure: z.string().optional(),
  preparation: z.string().optional(),
  calories: nonNegativeField,
  protein: nonNegativeField,
  carbohydrates: nonNegativeField,
  fat: nonNegativeField,
});
type AddItemInput = z.input<typeof addItemSchema>;
type AddItemValues = z.output<typeof addItemSchema>;

function parseOptionalTargetNumber(value: string | undefined): number {
  if (!value || !value.trim()) return 0;
  const parsed = Number(value);
  return Number.isFinite(parsed) && parsed >= 0 ? parsed : 0;
}

type AddMealFormProps = {
  nextOrder: number;
  isSubmitting: boolean;
  onAddMeal: (payload: AddMealPayload) => Promise<void>;
};

function AddMealForm({ nextOrder, isSubmitting, onAddMeal }: AddMealFormProps) {
  const [withTarget, setWithTarget] = useState(false);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<AddMealInput, unknown, AddMealValues>({
    resolver: zodResolver(addMealSchema),
    defaultValues: {
      name: "",
      order: nextOrder,
      time: "",
      notes: "",
    },
  });

  useEffect(() => {
    reset((prev) => ({ ...prev, order: nextOrder }));
  }, [nextOrder, reset]);

  async function onSubmit(values: AddMealValues) {
    try {
      const target = withTarget
        ? {
            calories: parseOptionalTargetNumber(values.targetCalories),
            protein: parseOptionalTargetNumber(values.targetProtein),
            carbohydrates: parseOptionalTargetNumber(values.targetCarbohydrates),
            fat: parseOptionalTargetNumber(values.targetFat),
          }
        : null;

      await onAddMeal({
        name: values.name,
        order: values.order,
        time: values.time?.trim() || null,
        notes: values.notes?.trim() || null,
        target,
      });

      reset({
        name: "",
        order: nextOrder + 1,
        time: "",
        notes: "",
        targetCalories: "",
        targetProtein: "",
        targetCarbohydrates: "",
        targetFat: "",
      });
      setWithTarget(false);
    } catch {
      setError("root", { message: "Não foi possível adicionar a refeição." });
    }
  }

  return (
    <form className="diet-meals__card" onSubmit={handleSubmit(onSubmit)} noValidate>
      <h2 className="diets__section-title" style={{ margin: 0 }}>
        Adicionar refeição
      </h2>
      <div className="diet-meals__form-grid">
        <Field
          id="new-meal-name"
          label="Nome da refeição"
          placeholder="Ex.: Café da manhã"
          error={errors.name?.message}
          {...register("name")}
        />
        <Field
          id="new-meal-order"
          label="Ordem"
          type="number"
          min={1}
          step={1}
          error={errors.order?.message}
          {...register("order")}
        />
        <Field
          id="new-meal-time"
          label="Horário (HH:mm)"
          type="time"
          error={errors.time?.message}
          {...register("time")}
        />
        <Field
          id="new-meal-notes"
          label="Observações"
          placeholder="Ex.: Pré-treino"
          error={errors.notes?.message}
          {...register("notes")}
        />
      </div>

      <label className="diet-meals__toggle">
        <input
          type="checkbox"
          checked={withTarget}
          onChange={(e) => setWithTarget(e.target.checked)}
        />
        Definir metas de macros da refeição (opcional)
      </label>

      {withTarget && (
        <div className="diet-meals__form-grid">
          <Field
            id="new-meal-target-calories"
            label="Meta calorias (kcal)"
            type="number"
            min={0}
            step="1"
            {...register("targetCalories")}
          />
          <Field
            id="new-meal-target-protein"
            label="Meta proteínas (g)"
            type="number"
            min={0}
            step="0.1"
            {...register("targetProtein")}
          />
          <Field
            id="new-meal-target-carbs"
            label="Meta carboidratos (g)"
            type="number"
            min={0}
            step="0.1"
            {...register("targetCarbohydrates")}
          />
          <Field
            id="new-meal-target-fat"
            label="Meta gorduras (g)"
            type="number"
            min={0}
            step="0.1"
            {...register("targetFat")}
          />
        </div>
      )}

      {errors.root && (
        <p className="athlete-form__error" role="alert">
          {errors.root.message}
        </p>
      )}

      <div>
        <Button type="submit" loading={isSubmitting}>
          Adicionar refeição
        </Button>
      </div>
    </form>
  );
}

type AddMealItemFormProps = {
  mealId: string;
  isSubmitting: boolean;
  onAddItem: (mealId: string, payload: AddMealItemPayload) => Promise<void>;
};

function AddMealItemForm({ mealId, isSubmitting, onAddItem }: AddMealItemFormProps) {
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<AddItemInput, unknown, AddItemValues>({
    resolver: zodResolver(addItemSchema),
    defaultValues: {
      name: "",
      source: "TACO",
      kind: "FOOD",
      state: "RAW",
      householdMeasure: "",
      preparation: "",
    },
  });

  async function onSubmit(values: AddItemValues) {
    try {
      await onAddItem(mealId, {
        name: values.name,
        source: values.source as FoodSource,
        kind: values.kind as FoodKind,
        state: values.state as FoodState,
        quantityG: values.quantityG,
        householdMeasure: values.householdMeasure?.trim() || null,
        preparation: values.preparation?.trim() || null,
        calories: values.calories,
        protein: values.protein,
        carbohydrates: values.carbohydrates,
        fat: values.fat,
      });

      reset({
        name: "",
        source: values.source,
        kind: values.kind,
        state: values.state,
        householdMeasure: "",
        preparation: "",
      });
    } catch {
      setError("root", { message: "Não foi possível adicionar o item." });
    }
  }

  return (
    <form className="diet-meals__inline-form" onSubmit={handleSubmit(onSubmit)} noValidate>
      <h4>Adicionar alimento ou suplemento</h4>
      <p className="diet-meals__hint">
        Entrada manual — busca automática no catálogo TACO será integrada após backend T-086.
      </p>

      <div className="diet-meals__form-grid">
        <Field
          id={`meal-${mealId}-item-name`}
          label="Alimento"
          placeholder="Ex.: Peito de frango"
          error={errors.name?.message}
          {...register("name")}
        />
        <SelectField
          id={`meal-${mealId}-item-source`}
          label="Origem"
          error={errors.source?.message}
          options={(["TACO", "CUSTOM", "USDA"] as const).map((src) => ({
            value: src,
            label: FOOD_SOURCE_LABEL[src],
          }))}
          {...register("source")}
        />
        <SelectField
          id={`meal-${mealId}-item-kind`}
          label="Tipo"
          error={errors.kind?.message}
          options={(["FOOD", "SUPPLEMENT_CALORIC"] as const).map((k) => ({
            value: k,
            label: FOOD_KIND_LABEL[k],
          }))}
          {...register("kind")}
        />
        <SelectField
          id={`meal-${mealId}-item-state`}
          label="Estado"
          error={errors.state?.message}
          options={(["RAW", "AS_SERVED"] as const).map((st) => ({
            value: st,
            label: STATE_OPTION_LABEL[st],
          }))}
          {...register("state")}
        />
      </div>

      <div className="diet-meals__form-grid">
        <Field
          id={`meal-${mealId}-item-qty`}
          label="Quantidade (g)"
          type="number"
          step="0.1"
          error={errors.quantityG?.message}
          {...register("quantityG")}
        />
        <Field
          id={`meal-${mealId}-item-measure`}
          label="Medida caseira"
          placeholder="Ex.: 1 filé médio"
          error={errors.householdMeasure?.message}
          {...register("householdMeasure")}
        />
        <Field
          id={`meal-${mealId}-item-prep`}
          label="Preparo"
          placeholder="Ex.: grelhado"
          error={errors.preparation?.message}
          {...register("preparation")}
        />
      </div>

      <div className="diet-meals__form-grid">
        <Field
          id={`meal-${mealId}-item-cal`}
          label="Calorias (kcal)"
          type="number"
          step="0.1"
          error={errors.calories?.message}
          {...register("calories")}
        />
        <Field
          id={`meal-${mealId}-item-prot`}
          label="Proteínas (g)"
          type="number"
          step="0.1"
          error={errors.protein?.message}
          {...register("protein")}
        />
        <Field
          id={`meal-${mealId}-item-carb`}
          label="Carboidratos (g)"
          type="number"
          step="0.1"
          error={errors.carbohydrates?.message}
          {...register("carbohydrates")}
        />
        <Field
          id={`meal-${mealId}-item-fat`}
          label="Gorduras (g)"
          type="number"
          step="0.1"
          error={errors.fat?.message}
          {...register("fat")}
        />
      </div>

      {errors.root && (
        <p className="athlete-form__error" role="alert">
          {errors.root.message}
        </p>
      )}

      <div>
        <Button type="submit" loading={isSubmitting}>
          Adicionar item
        </Button>
      </div>
    </form>
  );
}

type MealItemGroupProps = {
  title: string;
  items: MealItem[];
  isSupplement?: boolean;
  isReadOnly: boolean;
  removingItemId: string | null;
  onRemoveItem: (itemId: string) => void;
};

function MealItemGroup({
  title,
  items,
  isSupplement = false,
  isReadOnly,
  removingItemId,
  onRemoveItem,
}: MealItemGroupProps) {
  if (items.length === 0) return null;

  return (
    <div className="diet-meals__group">
      <h4
        className={`diet-meals__group-title ${
          isSupplement ? "diet-meals__group-title--supplement" : ""
        }`}
      >
        {title}
      </h4>
      <ul className="diet-meals__item-list">
        {items.map((item) => (
          <li
            key={item.id}
            className={`diet-meals__item ${
              isSupplement ? "diet-meals__item--supplement" : ""
            }`}
          >
            <div className="diet-meals__item-info">
              <div className="diet-meals__item-name">
                <span>{item.name}</span>
                <span className="diet-meals__badge">{FOOD_SOURCE_LABEL[item.source]}</span>
                <span
                  className={`diet-meals__badge ${
                    item.state === "RAW" ? "diet-meals__badge--raw" : ""
                  }`}
                >
                  {STATE_OPTION_LABEL[item.state]}
                </span>
              </div>
              <div className="diet-meals__item-details">
                {formatNumber(item.quantityG, " g")}
                {item.householdMeasure ? ` · ${item.householdMeasure}` : ""}
                {item.preparation ? ` · ${item.preparation}` : ""}
                {" · "}
                {formatNumber(item.calories, " kcal")} · P {formatNumber(item.protein, "g")} · C{" "}
                {formatNumber(item.carbohydrates, "g")} · G {formatNumber(item.fat, "g")}
              </div>
            </div>
            {!isReadOnly && (
              <Button
                type="button"
                variant="ghost"
                aria-label={`Remover item ${item.name}`}
                disabled={removingItemId === item.id}
                onClick={() => onRemoveItem(item.id)}
              >
                Remover item
              </Button>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}

type MealCardProps = {
  meal: Meal;
  isReadOnly: boolean;
  isRemovingMeal: boolean;
  isAddingItem: boolean;
  removingItemId: string | null;
  onRemoveMeal: (mealId: string) => void;
  onAddItem: (mealId: string, payload: AddMealItemPayload) => Promise<void>;
  onRemoveItem: (mealId: string, itemId: string) => void;
};

function MealCard({
  meal,
  isReadOnly,
  isRemovingMeal,
  isAddingItem,
  removingItemId,
  onRemoveMeal,
  onAddItem,
  onRemoveItem,
}: MealCardProps) {
  const foodItems = meal.items.filter((i) => i.kind === "FOOD");
  const supplementItems = meal.items.filter((i) => i.kind === "SUPPLEMENT_CALORIC");

  return (
    <article className="diet-meals__card" aria-label={`Refeição ${meal.name}`}>
      <div className="diet-meals__card-header">
        <div>
          <div className="diet-meals__card-title">
            <span className="diets__ver">#{meal.order}</span>
            <h3>{meal.name}</h3>
            {meal.time && <span className="diet-meals__badge">{meal.time}</span>}
          </div>
          {meal.notes && <p className="diet-meals__card-meta">{meal.notes}</p>}
          {meal.totals && (
            <p className="diet-meals__card-meta diets__mono">
              Totais: {formatNumber(meal.totals.calories, " kcal")} · P{" "}
              {formatNumber(meal.totals.protein, "g")} · C{" "}
              {formatNumber(meal.totals.carbohydrates, "g")} · G{" "}
              {formatNumber(meal.totals.fat, "g")}
            </p>
          )}
          {meal.target && (
            <p className="diet-meals__card-meta diets__mono">
              Meta: {formatNumber(meal.target.calories, " kcal")} · P{" "}
              {formatNumber(meal.target.protein, "g")} · C{" "}
              {formatNumber(meal.target.carbohydrates, "g")} · G{" "}
              {formatNumber(meal.target.fat, "g")}
            </p>
          )}
        </div>
        {!isReadOnly && (
          <Button
            type="button"
            variant="ghost"
            aria-label={`Remover refeição ${meal.name}`}
            disabled={isRemovingMeal}
            onClick={() => onRemoveMeal(meal.id)}
          >
            Remover refeição
          </Button>
        )}
      </div>

      {meal.items.length === 0 ? (
        <p className="diet-meals__hint">Nenhum alimento ou suplemento adicionado.</p>
      ) : (
        <>
          <MealItemGroup
            title="Alimentos"
            items={foodItems}
            isReadOnly={isReadOnly}
            removingItemId={removingItemId}
            onRemoveItem={(itemId) => onRemoveItem(meal.id, itemId)}
          />
          <MealItemGroup
            title="Suplementos calóricos"
            items={supplementItems}
            isSupplement
            isReadOnly={isReadOnly}
            removingItemId={removingItemId}
            onRemoveItem={(itemId) => onRemoveItem(meal.id, itemId)}
          />
        </>
      )}

      {!isReadOnly && (
        <AddMealItemForm mealId={meal.id} isSubmitting={isAddingItem} onAddItem={onAddItem} />
      )}
    </article>
  );
}

export function DietMealsPage() {
  const { id: athleteId = "", versionId = "" } = useParams();
  const { data: athlete, isLoading: athleteLoading, isError: athleteFailed } =
    useAthlete(athleteId);
  const { data: diet, isLoading: dietLoading, isError: dietFailed } = useDiet(versionId);

  const addMeal = useAddMeal(athleteId, versionId);
  const removeMeal = useRemoveMeal(athleteId, versionId);
  const addMealItem = useAddMealItem(athleteId, versionId);
  const removeMealItem = useRemoveMealItem(athleteId, versionId);

  const loading = athleteLoading || dietLoading;
  const loadFailed = athleteFailed || dietFailed;

  const isReadOnly = diet ? diet.readOnly || diet.status !== "DRAFT" : true;
  const sortedMeals = diet ? [...diet.meals].sort((a, b) => a.order - b.order) : [];
  const derivedTotals = diet ? computeDerivedMacros(diet.totals) : null;

  return (
    <AppShell navItems={COACH_NAV}>
      <div className="diet-meals">
        <nav className="diet-meals__nav" aria-label="Navegação da dieta">
          <Link to={`/coach/athletes/${athleteId}?tab=dietas`} className="athlete-form__back">
            &larr; Voltar para a ficha
          </Link>
          <Link
            to={`/coach/athletes/${athleteId}/diets/${versionId}`}
            className="diet-meals__summary-link"
          >
            Resumo da dieta
          </Link>
        </nav>

        <header className="diet-meals__header">
          <h1 className="athlete-form__title">
            Refeições da dieta{athlete ? ` — ${athlete.name}` : ""}
          </h1>
          {diet && (
            <div className="diet-meals__badges">
              <span className="diets__ver diets__ver--current">v{diet.version}</span>
              <span>{OBJECTIVE_LABEL[diet.objective]}</span>
              <StatusBadge status={diet.status} />
            </div>
          )}
        </header>

        {loading ? (
          <p>Carregando...</p>
        ) : loadFailed || !diet ? (
          <div className="diet-editor__blocked" role="alert">
            <p>Não foi possível carregar a dieta. Tente novamente.</p>
            <ButtonLink to={`/coach/athletes/${athleteId}?tab=dietas`} variant="ghost">
              Voltar para a ficha
            </ButtonLink>
          </div>
        ) : (
          <>
            {isReadOnly && (
              <div className="diet-meals__readonly-alert" role="alert">
                <strong>Modo somente leitura:</strong>{" "}
                {diet.readOnly
                  ? `A versão v${diet.version} está congelada e não pode ser alterada.`
                  : `A versão v${diet.version} está com status "${STATUS_LABEL[diet.status]}".`}{" "}
                Apenas versões em <strong>Rascunho</strong> e não congeladas podem ser modificadas.
              </div>
            )}

            <section className="diet-meals__macros-panel" aria-label="Metas e totais">
              <div className="diet-meals__macro-box">
                <h2>Metas prescritas</h2>
                <dl className="diet-meals__macro-grid">
                  <div>
                    <dt>Calorias</dt>
                    <dd>{formatNumber(diet.macros.calories, " kcal")}</dd>
                  </div>
                  <div>
                    <dt>Proteínas</dt>
                    <dd>{formatNumber(diet.macros.protein, " g")}</dd>
                  </div>
                  <div>
                    <dt>Carboidratos</dt>
                    <dd>{formatNumber(diet.macros.carbohydrates, " g")}</dd>
                  </div>
                  <div>
                    <dt>Gorduras</dt>
                    <dd>{formatNumber(diet.macros.fat, " g")}</dd>
                  </div>
                </dl>
              </div>

              <div className="diet-meals__macro-box">
                <h2>Totais das refeições</h2>
                <dl className="diet-meals__macro-grid">
                  <div>
                    <dt>Calorias</dt>
                    <dd>{formatNumber(diet.totals.calories, " kcal")}</dd>
                  </div>
                  <div>
                    <dt>Proteínas</dt>
                    <dd>{formatNumber(diet.totals.protein, " g")}</dd>
                  </div>
                  <div>
                    <dt>Carboidratos</dt>
                    <dd>{formatNumber(diet.totals.carbohydrates, " g")}</dd>
                  </div>
                  <div>
                    <dt>Gorduras</dt>
                    <dd>{formatNumber(diet.totals.fat, " g")}</dd>
                  </div>
                </dl>
                {derivedTotals && (
                  <>
                    <p className="diet-meals__kcal-check">
                      Checagem 4/4/9: {formatNumber(derivedTotals.kcalFromMacros, " kcal")} (
                      {Math.round(derivedTotals.proteinPct)}% P ·{" "}
                      {Math.round(derivedTotals.carbsPct)}% C · {Math.round(derivedTotals.fatPct)}
                      % G)
                    </p>
                    {derivedTotals.hasCalorieInconsistency && (
                      <p className="diet-meals__kcal-warning" role="status">
                        Divergência de {formatNumber(derivedTotals.calorieDeltaPct, "%")} entre
                        calorias informadas e regra 4/4/9.
                      </p>
                    )}
                  </>
                )}
              </div>
            </section>

            {!isReadOnly && (
              <AddMealForm
                nextOrder={sortedMeals.length + 1}
                isSubmitting={addMeal.isPending}
                onAddMeal={async (payload) => {
                  await addMeal.mutateAsync(payload);
                }}
              />
            )}

            <section aria-label="Lista de refeições">
              {sortedMeals.length === 0 ? (
                <div className="diets__empty">
                  <p>Nenhuma refeição cadastrada nesta versão.</p>
                </div>
              ) : (
                sortedMeals.map((meal) => (
                  <MealCard
                    key={meal.id}
                    meal={meal}
                    isReadOnly={isReadOnly}
                    isRemovingMeal={removeMeal.isPending && removeMeal.variables === meal.id}
                    isAddingItem={
                      addMealItem.isPending && addMealItem.variables?.mealId === meal.id
                    }
                    removingItemId={
                      removeMealItem.isPending && removeMealItem.variables?.mealId === meal.id
                        ? removeMealItem.variables.itemId
                        : null
                    }
                    onRemoveMeal={(mealId) => removeMeal.mutate(mealId)}
                    onAddItem={async (mealId, payload) => {
                      await addMealItem.mutateAsync({ mealId, payload });
                    }}
                    onRemoveItem={(mealId, itemId) => removeMealItem.mutate({ mealId, itemId })}
                  />
                ))
              )}
            </section>
          </>
        )}
      </div>
    </AppShell>
  );
}

