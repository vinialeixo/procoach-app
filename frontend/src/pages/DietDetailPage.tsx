import { Link, useParams } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { COACH_NAV } from "@/lib/coach-nav";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { ButtonLink } from "@/components/ui/ButtonLink";
import { useAthlete } from "@/lib/athletes";
import { useAssessments } from "@/lib/assessments";
import {
  CLASSIFICATION_LABEL,
  DiffStatus,
  FOOD_KIND_LABEL,
  FOOD_SOURCE_LABEL,
  FOOD_STATE_LABEL,
  MacroKey,
  OBJECTIVE_LABEL,
  computeDerivedMacros,
  diffDietVersions,
  useDiet,
  useDietEnergyBalance,
  useDiets,
} from "@/lib/diets";
import "./AthleteDiets.css";

function formatInstant(iso: string): string {
  return new Date(iso).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function formatShortDate(iso: string): string {
  const normalized = /^\d{4}-\d{2}-\d{2}$/.test(iso) ? `${iso}T00:00:00` : iso;
  return new Date(normalized).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "2-digit",
  });
}

function formatNumber(value: number, suffix = ""): string {
  return `${value.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}${suffix}`;
}

function formatSigned(value: number, suffix = ""): string {
  const sign = value > 0 ? "+" : "";
  return `${sign}${formatNumber(value, suffix)}`;
}

function isHttpStatus(error: unknown, status: number): boolean {
  if (error && typeof error === "object" && "response" in error) {
    const response = (error as { response?: { status?: number } }).response;
    return response?.status === status;
  }
  return false;
}

const MACRO_LABEL: Record<MacroKey, string> = {
  protein: "Proteínas",
  carbohydrates: "Carboidratos",
  fat: "Gorduras",
};

const DIFF_STATUS_LABEL: Record<DiffStatus, string> = {
  ADDED: "Adicionado",
  REMOVED: "Removido",
  CHANGED: "Alterado",
  UNCHANGED: "Sem alterações",
};

export function DietDetailPage() {
  const { id: athleteId = "", versionId = "" } = useParams();

  const athleteQuery = useAthlete(athleteId);
  const dietQuery = useDiet(versionId);
  const dietsQuery = useDiets(athleteId);
  const assessmentsQuery = useAssessments(athleteId);
  const energyBalanceQuery = useDietEnergyBalance(versionId);

  const diet = dietQuery.data;
  const athlete = athleteQuery.data;

  if (dietQuery.isError) {
    return (
      <AppShell navItems={COACH_NAV}>
        <div className="diet-detail">
          <Link className="diet-detail__back" to={`/coach/athletes/${athleteId}?tab=dietas`}>
            ← Voltar para dietas do atleta
          </Link>
          <p className="diets__error" role="alert">
            Não foi possível carregar os detalhes desta versão da dieta.
          </p>
        </div>
      </AppShell>
    );
  }

  if (dietQuery.isLoading || !diet) {
    return (
      <AppShell navItems={COACH_NAV}>
        <p className="diets__loading">Carregando detalhes da dieta...</p>
      </AppShell>
    );
  }

  const latestWeightKg =
    [...(assessmentsQuery.data ?? [])].sort((a, b) =>
      b.assessmentDate.localeCompare(a.assessmentDate)
    )[0]?.measurements.weightKg ?? null;

  const derived = computeDerivedMacros(diet.macros, latestWeightKg);
  const energyBalanceMissingAssessment =
    energyBalanceQuery.isError && isHttpStatus(energyBalanceQuery.error, 409);

  const sortedMeals = [...(diet.meals ?? [])].sort((a, b) => a.order - b.order);

  const previousVersion = diet.previousVersionId
    ? (dietsQuery.data ?? []).find((v) => v.id === diet.previousVersionId)
    : undefined;
  const versionDiff = previousVersion ? diffDietVersions(previousVersion, diet) : null;

  return (
    <AppShell navItems={COACH_NAV}>
      <div className="diet-detail">
        <Link className="diet-detail__back" to={`/coach/athletes/${athleteId}?tab=dietas`}>
          ← Voltar para dietas do atleta
        </Link>

        <header className="diet-detail__header">
          <div className="diet-detail__title-group">
            <h1 className="diet-detail__title">
              {athlete ? `${athlete.name} — ` : ""}Dieta v{diet.version}
            </h1>
            <StatusBadge status={diet.status} />
            <span className="diets__ver">{OBJECTIVE_LABEL[diet.objective]}</span>
            {diet.readOnly && <span className="diets__ver diets__ver--frozen">Somente leitura</span>}
          </div>
          {diet.status === "DRAFT" && !diet.readOnly && (
            <ButtonLink to={`/coach/athletes/${athleteId}/diets/${versionId}/meals`}>
              Gerenciar refeições
            </ButtonLink>
          )}
        </header>

        <section className="diet-detail__card" aria-label="Macronutrientes e métricas derivadas">
          <h2 className="diets__section-title">Macronutrientes</h2>
          <div className="diets__macros-grid">
            <div className="diets__macro-card">
              <span className="diets__macro-label">Calorias</span>
              <span className="diets__macro-value">
                {formatNumber(diet.macros.calories, " kcal")}
              </span>
              {derived.kcalPerKg != null && (
                <span className="diets__macro-sub">
                  {formatNumber(derived.kcalPerKg, " kcal/kg")}
                </span>
              )}
            </div>
            <div className="diets__macro-card">
              <span className="diets__macro-label">Proteínas</span>
              <span className="diets__macro-value">
                {formatNumber(diet.macros.protein, " g")}
              </span>
              <span className="diets__macro-sub">
                {formatNumber(derived.proteinPct, "% kcal")}
                {derived.proteinPerKg != null &&
                  ` · ${formatNumber(derived.proteinPerKg, " g/kg")}`}
              </span>
            </div>
            <div className="diets__macro-card">
              <span className="diets__macro-label">Carboidratos</span>
              <span className="diets__macro-value">
                {formatNumber(diet.macros.carbohydrates, " g")}
              </span>
              <span className="diets__macro-sub">
                {formatNumber(derived.carbsPct, "% kcal")}
                {derived.carbsPerKg != null &&
                  ` · ${formatNumber(derived.carbsPerKg, " g/kg")}`}
              </span>
            </div>
            <div className="diets__macro-card">
              <span className="diets__macro-label">Gorduras</span>
              <span className="diets__macro-value">
                {formatNumber(diet.macros.fat, " g")}
              </span>
              <span className="diets__macro-sub">
                {formatNumber(derived.fatPct, "% kcal")}
                {derived.fatPerKg != null &&
                  ` · ${formatNumber(derived.fatPerKg, " g/kg")}`}
              </span>
            </div>
          </div>

          {(derived.hasCalorieInconsistency || derived.zeroMacros.length > 0) && (
            <div className="diets__warnings" role="status" aria-label="Avisos nutricionais">
              {derived.hasCalorieInconsistency && (
                <div className="diets__warning">
                  Aviso nutricional: a soma dos macronutrientes pela regra 4/4/9 (
                  {formatNumber(derived.kcalFromMacros, " kcal")}) diverge das calorias
                  informadas ({formatNumber(diet.macros.calories, " kcal")}) em{" "}
                  {formatNumber(Math.abs(derived.calorieDeltaPct), "%")}.
                </div>
              )}
              {derived.zeroMacros.length > 0 && (
                <div className="diets__warning">
                  Aviso nutricional: macronutriente(s) zerado(s):{" "}
                  {derived.zeroMacros.map((k) => MACRO_LABEL[k]).join(", ")}.
                </div>
              )}
            </div>
          )}

          {energyBalanceQuery.data && (
            <div className="diets__energy" aria-label="Balanço energético">
              <div className="diets__energy-row">
                <span className="diets__energy-badge">
                  {CLASSIFICATION_LABEL[energyBalanceQuery.data.classification]}
                </span>
                <span>
                  GET:{" "}
                  <strong className="diets__mono">
                    {formatNumber(energyBalanceQuery.data.getKcal, " kcal")}
                  </strong>
                </span>
                <span>
                  Delta:{" "}
                  <strong className="diets__mono">
                    {formatSigned(energyBalanceQuery.data.balanceKcal, " kcal")}
                  </strong>
                </span>
                <span className="diets__meta">
                  Avaliação de {formatShortDate(energyBalanceQuery.data.basedOnAssessmentDate)}
                </span>
              </div>
            </div>
          )}

          {energyBalanceMissingAssessment && (
            <div className="diets__energy diets__energy--empty" role="status">
              <div className="diets__energy-row">
                <span>
                  Balanço energético indisponível: registre uma avaliação física para
                  calcular o GET e comparar com a dieta.
                </span>
                <ButtonLink
                  to={`/coach/athletes/${athleteId}/assessments/new`}
                  variant="ghost"
                >
                  Registrar avaliação
                </ButtonLink>
              </div>
            </div>
          )}
        </section>

        <section className="diet-detail__card" aria-label="Refeições">
          <h2 className="diets__section-title">Refeições ({sortedMeals.length})</h2>
          {sortedMeals.length === 0 ? (
            <p className="diets__meta">Nenhuma refeição cadastrada nesta versão.</p>
          ) : (
            <div className="diet-detail__meals">
              {sortedMeals.map((meal) => (
                <article key={meal.id} className="diet-detail__meal">
                  <div className="diet-detail__meal-header">
                    <h3 className="diet-detail__meal-title">
                      {meal.order}. {meal.name}
                      {meal.time ? ` · ${meal.time}` : ""}
                    </h3>
                    {meal.totals && (
                      <span className="diets__mono">
                        {formatNumber(meal.totals.calories, " kcal")} · P{" "}
                        {formatNumber(meal.totals.protein, "g")} · C{" "}
                        {formatNumber(meal.totals.carbohydrates, "g")} · G{" "}
                        {formatNumber(meal.totals.fat, "g")}
                      </span>
                    )}
                  </div>
                  {meal.notes && <p className="diets__meta">{meal.notes}</p>}
                  {meal.items.length === 0 ? (
                    <p className="diets__meta">Sem itens nesta refeição.</p>
                  ) : (
                    <ul className="diet-detail__items">
                      {meal.items.map((item) => (
                        <li key={item.id} className="diet-detail__item">
                          <div>
                            <strong>{item.name}</strong> —{" "}
                            <span className="diets__mono">
                              {formatNumber(item.quantityG, "g")}
                            </span>
                            {item.householdMeasure && ` (${item.householdMeasure})`}
                            <div className="diet-detail__item-meta">
                              <span>{FOOD_KIND_LABEL[item.kind]}</span>
                              <span>· {FOOD_STATE_LABEL[item.state]}</span>
                              <span>· {FOOD_SOURCE_LABEL[item.source]}</span>
                              {item.preparation && <span>· Preparo: {item.preparation}</span>}
                            </div>
                          </div>
                          <span className="diets__mono">
                            {formatNumber(item.calories, " kcal")} · P{" "}
                            {formatNumber(item.protein, "g")} · C{" "}
                            {formatNumber(item.carbohydrates, "g")} · G{" "}
                            {formatNumber(item.fat, "g")}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                </article>
              ))}
            </div>
          )}
        </section>

        {versionDiff && (
          <section className="diet-detail__card" aria-label="Diferenças em relação à versão anterior">
            <h2 className="diets__section-title">
              Comparação com v{versionDiff.previousVersion} → v{versionDiff.currentVersion}
            </h2>
            <div className="diets__macros-grid">
              <div className="diets__macro-card">
                <span className="diets__macro-label">Delta Calorias</span>
                <span className="diets__macro-value">
                  {formatSigned(versionDiff.macroDeltas.calories, " kcal")}
                </span>
              </div>
              <div className="diets__macro-card">
                <span className="diets__macro-label">Delta Proteínas</span>
                <span className="diets__macro-value">
                  {formatSigned(versionDiff.macroDeltas.protein, " g")}
                </span>
              </div>
              <div className="diets__macro-card">
                <span className="diets__macro-label">Delta Carboidratos</span>
                <span className="diets__macro-value">
                  {formatSigned(versionDiff.macroDeltas.carbohydrates, " g")}
                </span>
              </div>
              <div className="diets__macro-card">
                <span className="diets__macro-label">Delta Gorduras</span>
                <span className="diets__macro-value">
                  {formatSigned(versionDiff.macroDeltas.fat, " g")}
                </span>
              </div>
            </div>

            <ul className="diet-detail__diff-list">
              {versionDiff.meals.map((mealDiff, idx) => (
                <li key={`${mealDiff.order}-${mealDiff.name}-${idx}`} className="diet-detail__diff-meal">
                  <div>
                    <span className={`diet-detail__diff-tag diet-detail__diff-tag--${mealDiff.status}`}>
                      {DIFF_STATUS_LABEL[mealDiff.status]}
                    </span>
                    <strong>{mealDiff.name}</strong>
                  </div>
                  {mealDiff.items.some((item) => item.status !== "UNCHANGED") && (
                    <ul className="diet-detail__items">
                      {mealDiff.items
                        .filter((item) => item.status !== "UNCHANGED")
                        .map((item, itemIdx) => (
                          <li key={`${item.name}-${itemIdx}`} className="diet-detail__item">
                            <span>
                              <span className={`diet-detail__diff-tag diet-detail__diff-tag--${item.status}`}>
                                {DIFF_STATUS_LABEL[item.status]}
                              </span>
                              {item.name}
                            </span>
                            <span className="diets__mono">
                              {formatSigned(item.quantityDeltaG, "g")} ·{" "}
                              {formatSigned(item.caloriesDelta, " kcal")}
                            </span>
                          </li>
                        ))}
                    </ul>
                  )}
                </li>
              ))}
            </ul>
          </section>
        )}

        <section className="diet-detail__card" aria-label="Auditoria">
          <h2 className="diets__section-title">Auditoria</h2>
          <div className="diet-detail__audit-grid">
            <div>
              <div className="diets__macro-label">Criada por</div>
              <div className="diets__mono">{diet.createdBy}</div>
            </div>
            <div>
              <div className="diets__macro-label">Criada em</div>
              <div className="diets__mono">{formatInstant(diet.createdAt)}</div>
            </div>
            <div>
              <div className="diets__macro-label">Atualizada por</div>
              <div className="diets__mono">{diet.updatedBy}</div>
            </div>
            <div>
              <div className="diets__macro-label">Atualizada em</div>
              <div className="diets__mono">{formatInstant(diet.updatedAt)}</div>
            </div>
            <div>
              <div className="diets__macro-label">Motivo da alteração</div>
              <div>{diet.changeReason ?? "—"}</div>
            </div>
          </div>
        </section>
      </div>
    </AppShell>
  );
}
