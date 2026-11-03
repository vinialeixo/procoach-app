import { useState } from "react";
import { Link } from "react-router-dom";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { DataTable, Column } from "@/components/ui/DataTable";
import { Button } from "@/components/ui/Button";
import { Assessment, useAssessments } from "@/lib/assessments";
import "./AthleteAssessments.css";

// T-027 — Painel "Avaliações" da ficha do atleta (US-007/008/009/010).
// Persona: Coach. Lista cronológica + gráfico de evolução (Recharts,
// restilizado com os tokens) + comparação lado a lado de 2+ avaliações com
// destaque nas diferenças (US-009), reaproveitando o DataTable. Métricas
// calculadas (IMC/TMB/GET/evolução) vêm prontas da API (ver lib/assessments).

type ChartMetric = {
  key: string;
  label: string;
  get: (a: Assessment) => number | null;
};

const CHART_METRICS: ChartMetric[] = [
  { key: "weightKg", label: "Peso (kg)", get: (a) => a.measurements.weightKg },
  {
    key: "bodyFatPercentage",
    label: "% de gordura",
    get: (a) => a.measurements.bodyFatPercentage,
  },
  {
    key: "muscleMassKg",
    label: "Massa muscular (kg)",
    get: (a) => a.measurements.muscleMassKg,
  },
];

// assessmentDate é date-only (yyyy-mm-dd): formatar na mão, sem new Date(),
// para não deslocar o dia por fuso horário.
function formatDate(iso: string): string {
  const [y, m, d] = iso.split("-");
  return `${d}/${m}/${y}`;
}

function formatShortDate(iso: string): string {
  const [y, m, d] = iso.split("-");
  return `${d}/${m}/${y.slice(2)}`;
}

function formatNumber(value: number | null, suffix = ""): string {
  if (value === null) return "—";
  return `${value.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}${suffix}`;
}

function formatSigned(value: number | null, suffix = ""): string {
  if (value === null) return "—";
  const sign = value > 0 ? "+" : "";
  return `${sign}${formatNumber(value, suffix)}`;
}

type MetricRow = {
  key: string;
  label: string;
  values: (number | null)[]; // alinhado à ordem das colunas (avaliações)
  format: (v: number | null) => string;
};

function hasDiff(row: MetricRow): boolean {
  return new Set(row.values).size > 1;
}

export function AthleteAssessmentsPanel({
  athleteId,
  allowCreate = true,
}: {
  athleteId: string;
  allowCreate?: boolean;
}) {
  const { data: assessments, isLoading, isError } = useAssessments(athleteId);
  const [chartMetricKey, setChartMetricKey] = useState(CHART_METRICS[0].key);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);

  // US-007, critério 3: listagem em ordem cronológica (defensivo — o
  // contrato assumido já pede a lista ordenada, mas o gráfico depende disso).
  const sorted = [...(assessments ?? [])].sort((a, b) =>
    a.assessmentDate.localeCompare(b.assessmentDate)
  );

  const chartMetric =
    CHART_METRICS.find((m) => m.key === chartMetricKey) ?? CHART_METRICS[0];
  const chartData = sorted
    .map((a) => ({
      date: formatShortDate(a.assessmentDate),
      value: chartMetric.get(a),
    }))
    .filter((p): p is { date: string; value: number } => p.value !== null);

  function toggleSelected(id: string) {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  }

  const selected = sorted.filter((a) => selectedIds.includes(a.id));

  const listColumns: Column<Assessment>[] = [
    {
      key: "compare",
      header: "Comparar",
      render: (a) => (
        <input
          type="checkbox"
          checked={selectedIds.includes(a.id)}
          onChange={() => toggleSelected(a.id)}
          aria-label={`Selecionar avaliação de ${formatDate(a.assessmentDate)}`}
        />
      ),
    },
    {
      key: "date",
      header: "Data",
      render: (a) => formatDate(a.assessmentDate),
    },
    {
      key: "weight",
      header: "Peso",
      render: (a) => formatNumber(a.measurements.weightKg, " kg"),
    },
    {
      key: "bodyFat",
      header: "% Gordura",
      render: (a) => formatNumber(a.measurements.bodyFatPercentage, "%"),
    },
    {
      key: "imc",
      header: "IMC",
      render: (a) => formatNumber(a.metrics.imc),
    },
    {
      key: "weightChange",
      header: "Δ Peso",
      render: (a) => formatSigned(a.metrics.weightChangeKg, " kg"),
    },
  ];

  // US-009, critério 3: a comparação inclui todas as métricas registradas
  // (medidas brutas + derivadas).
  const compareRows: MetricRow[] = [
    {
      key: "weight",
      label: "Peso (kg)",
      values: selected.map((a) => a.measurements.weightKg),
      format: (v) => formatNumber(v),
    },
    {
      key: "height",
      label: "Altura (cm)",
      values: selected.map((a) => a.measurements.heightCm),
      format: (v) => formatNumber(v),
    },
    {
      key: "bodyFat",
      label: "% de gordura",
      values: selected.map((a) => a.measurements.bodyFatPercentage),
      format: (v) => formatNumber(v),
    },
    {
      key: "muscleMass",
      label: "Massa muscular (kg)",
      values: selected.map((a) => a.measurements.muscleMassKg),
      format: (v) => formatNumber(v),
    },
    {
      key: "waist",
      label: "Cintura (cm)",
      values: selected.map((a) => a.measurements.waistCm),
      format: (v) => formatNumber(v),
    },
    {
      key: "hip",
      label: "Quadril (cm)",
      values: selected.map((a) => a.measurements.hipCm),
      format: (v) => formatNumber(v),
    },
    {
      key: "imc",
      label: "IMC",
      values: selected.map((a) => a.metrics.imc),
      format: (v) => formatNumber(v),
    },
    {
      key: "tmb",
      label: "TMB (kcal)",
      values: selected.map((a) => a.metrics.tmbKcal),
      format: (v) => formatNumber(v),
    },
    {
      key: "get",
      label: "GET (kcal)",
      values: selected.map((a) => a.metrics.getKcal),
      format: (v) => formatNumber(v),
    },
    {
      key: "weightChangeKg",
      label: "Δ Peso vs. anterior (kg)",
      values: selected.map((a) => a.metrics.weightChangeKg),
      format: (v) => formatSigned(v),
    },
    {
      key: "weightChangePct",
      label: "Δ Peso vs. anterior (%)",
      values: selected.map((a) => a.metrics.weightChangePercent),
      format: (v) => formatSigned(v, "%"),
    },
  ];

  const compareColumns: Column<MetricRow>[] = [
    {
      key: "metric",
      header: "Métrica",
      render: (row) => (
        <span className={hasDiff(row) ? "assessments__diff" : undefined}>
          {row.label}
        </span>
      ),
    },
    ...selected.map((a, i) => ({
      key: a.id,
      header: formatDate(a.assessmentDate),
      render: (row: MetricRow) => (
        <span className={hasDiff(row) ? "assessments__diff" : undefined}>
          {row.format(row.values[i])}
        </span>
      ),
    })),
  ];

  return (
    <div className="assessments">
      <div className="assessments__header">
        <h2 className="assessments__title">Avaliações físicas</h2>
        {allowCreate && (
          <Link
            to={`/coach/athletes/${athleteId}/assessments/new`}
            className="assessments__new"
          >
            <Button>Nova avaliação</Button>
          </Link>
        )}
      </div>

      {isError && (
        <p className="assessments__error" role="alert">
          Não foi possível carregar as avaliações. Tente novamente.
        </p>
      )}

      {isLoading ? (
        <p className="assessments__loading">Carregando avaliações...</p>
      ) : (
        <>
          <div className="assessments__chart-card">
            <div className="assessments__chart-head">
              <h3 className="assessments__section-title">Evolução</h3>
              <select
                className="assessments__metric-filter"
                value={chartMetricKey}
                onChange={(e) => setChartMetricKey(e.target.value)}
                aria-label="Métrica do gráfico"
              >
                {CHART_METRICS.map((m) => (
                  <option key={m.key} value={m.key}>
                    {m.label}
                  </option>
                ))}
              </select>
            </div>
            {chartData.length >= 2 ? (
              <ResponsiveContainer width="100%" height={280}>
                <LineChart
                  data={chartData}
                  margin={{ top: 8, right: 16, bottom: 0, left: 0 }}
                >
                  <CartesianGrid
                    stroke="var(--color-border)"
                    strokeDasharray="3 3"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="date"
                    tick={{ fill: "var(--color-ink-muted)", fontSize: 12 }}
                    tickLine={false}
                    axisLine={{ stroke: "var(--color-border)" }}
                  />
                  <YAxis
                    tick={{ fill: "var(--color-ink-muted)", fontSize: 12 }}
                    tickLine={false}
                    axisLine={false}
                    width={48}
                    domain={["auto", "auto"]}
                  />
                  <Tooltip
                    contentStyle={{
                      background: "var(--color-surface)",
                      border: "1px solid var(--color-border)",
                      borderRadius: "var(--radius-sm)",
                      fontSize: "var(--text-sm)",
                    }}
                  />
                  <Line
                    type="monotone"
                    dataKey="value"
                    name={chartMetric.label}
                    stroke="var(--color-accent)"
                    strokeWidth={2}
                    dot={{ fill: "var(--color-accent)", r: 3 }}
                    activeDot={{ r: 5 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <p className="assessments__chart-hint">
                Registre ao menos duas avaliações com "
                {chartMetric.label.toLowerCase()}" para ver a evolução.
              </p>
            )}
          </div>

          <h3 className="assessments__section-title">Histórico</h3>
          <DataTable
            columns={listColumns}
            rows={sorted}
            rowKey={(a) => a.id}
            emptyMessage="Nenhuma avaliação registrada."
          />

          {selected.length >= 2 && (
            <>
              <h3 className="assessments__section-title">
                Comparação ({selected.length} avaliações)
              </h3>
              <DataTable
                columns={compareColumns}
                rows={compareRows}
                rowKey={(r) => r.key}
              />
            </>
          )}
        </>
      )}
    </div>
  );
}
