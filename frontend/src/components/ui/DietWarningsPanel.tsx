import {
  DietWarning,
  DietWarningSeverity,
  Macronutrients,
  computeDerivedMacros,
  hasRelevantDietWarnings,
} from "@/lib/diets";
import "./DietWarningsPanel.css";

const SEVERITY_LABEL: Record<DietWarningSeverity, string> = {
  CRITICAL: "Crítico",
  WARNING: "Aviso",
  INFO: "Info",
};

function formatValue(value: number, suffix = ""): string {
  return `${value.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}${suffix}`;
}

export type DietWarningsPanelProps = {
  warnings: DietWarning[];
  macros?: Macronutrients;
  weightKg?: number | null;
  tmbKcal?: number | null;
  getKcal?: number | null;
  acknowledged?: boolean;
  onAcknowledgeChange?: (acknowledged: boolean) => void;
  showReferenceRanges?: boolean;
};

export function DietWarningsPanel({
  warnings,
  macros,
  weightKg,
  tmbKcal,
  getKcal,
  acknowledged = false,
  onAcknowledgeChange,
  showReferenceRanges = true,
}: DietWarningsPanelProps) {
  const derived = macros ? computeDerivedMacros(macros, weightKg) : null;
  const hasRelevant = hasRelevantDietWarnings(warnings);

  const balancePct =
    macros && macros.calories > 0 && getKcal != null && getKcal > 0
      ? ((macros.calories - getKcal) / getKcal) * 100
      : null;

  return (
    <section
      className="diet-warnings"
      aria-label="Avisos nutricionais e faixas de referência"
    >
      <div className="diet-warnings__header">
        <h2 className="diet-warnings__title">
          Avisos nutricionais e faixas de referência
        </h2>
      </div>

      {showReferenceRanges && (
        <div className="diet-warnings__ranges" aria-label="Faixas de referência">
          <div className="diet-warnings__range-item">
            <span className="diet-warnings__range-label">Proteína (ref. 1,6–2,2 g/kg)</span>
            <span className="diet-warnings__range-value">
              {derived?.proteinPerKg != null
                ? `${formatValue(derived.proteinPerKg, " g/kg")}`
                : "Informe avaliação"}
            </span>
          </div>
          <div className="diet-warnings__range-item">
            <span className="diet-warnings__range-label">Gordura (ref. 0,5–1,0 g/kg)</span>
            <span className="diet-warnings__range-value">
              {derived?.fatPerKg != null
                ? `${formatValue(derived.fatPerKg, " g/kg")}`
                : "Informe avaliação"}
            </span>
          </div>
          <div className="diet-warnings__range-item">
            <span className="diet-warnings__range-label">Fibras (ref. 25–38 g/dia)</span>
            <span className="diet-warnings__range-value">25–38 g/dia</span>
          </div>
          <div className="diet-warnings__range-item">
            <span className="diet-warnings__range-label">Checagem 4/4/9 (±5% ou ±50 kcal)</span>
            <span className="diet-warnings__range-value">
              {derived ? formatValue(derived.kcalFromMacros, " kcal") : "—"}
            </span>
          </div>
          {tmbKcal != null && tmbKcal > 0 && (
            <div className="diet-warnings__range-item">
              <span className="diet-warnings__range-label">TMB de referência</span>
              <span className="diet-warnings__range-value">
                {formatValue(tmbKcal, " kcal")}
              </span>
            </div>
          )}
          {getKcal != null && getKcal > 0 && (
            <div className="diet-warnings__range-item">
              <span className="diet-warnings__range-label">GET (déficit seguro ≤ 25%)</span>
              <span className="diet-warnings__range-value">
                {formatValue(getKcal, " kcal")}
                {balancePct !== null ? ` (${balancePct > 0 ? "+" : ""}${balancePct.toFixed(1)}%)` : ""}
              </span>
            </div>
          )}
        </div>
      )}

      {warnings.length === 0 ? (
        <p className="diet-warnings__empty">
          Nenhum aviso nutricional para os valores atuais.
        </p>
      ) : (
        <ul className="diet-warnings__list">
          {warnings.map((warning) => (
            <li
              key={`${warning.code}-${warning.message}`}
              className={`diet-warnings__item diet-warnings__item--${warning.severity}`}
              data-severity={warning.severity}
              data-warning-code={warning.code}
            >
              <span
                className={`diet-warnings__badge diet-warnings__badge--${warning.severity}`}
              >
                {SEVERITY_LABEL[warning.severity]}
              </span>
              <span>{warning.message}</span>
            </li>
          ))}
        </ul>
      )}

      {hasRelevant && onAcknowledgeChange && (
        <div className="diet-warnings__ack">
          <label className="diet-warnings__ack-label">
            <input
              type="checkbox"
              checked={acknowledged}
              onChange={(e) => onAcknowledgeChange(e.target.checked)}
            />
            <span>
              Estou ciente dos avisos nutricionais e assumo a decisão clínica desta
              prescrição.
            </span>
          </label>
          <p className="diet-warnings__ack-hint">
            Os avisos são informativos e não bloqueiam o salvamento. A decisão e a
            aprovação permanecem com o coach.
          </p>
        </div>
      )}
    </section>
  );
}
