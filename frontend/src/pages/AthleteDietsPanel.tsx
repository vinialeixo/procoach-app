import { useState } from "react";
import { DataTable, Column } from "@/components/ui/DataTable";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { Modal } from "@/components/ui/Modal";
import { Button } from "@/components/ui/Button";
import { ButtonLink } from "@/components/ui/ButtonLink";
import {
  DietStatus,
  DietVersion,
  DIET_STATUS_FLOW,
  OBJECTIVE_LABEL,
  STATUS_LABEL,
  findCurrentDietVersion,
  useDiets,
  useChangeDietStatus,
} from "@/lib/diets";
import "./AthleteDiets.css";

// T-034 — Painel "Dietas" da ficha do atleta (épico E3: US-011, US-014,
// US-015, US-016, US-017). Persona: Coach.
//
// Regras de negócio refletidas aqui (ver lib/diets.ts — espelho do domínio):
// - nenhuma edição sobrescreve (US-015): a ação de conteúdo é sempre
//   "Nova versão", nunca editar no lugar;
// - fluxo de status travado (US-017): o card da versão atual só oferece a
//   PRÓXIMA transição (Rascunho -> Em Revisão -> Aprovada -> Arquivada);
//   versões antigas (readOnly) não têm ação alguma;
// - auditoria (US-016): cada linha mostra quando, quem (UUID, o núcleo não
//   expõe nomes) e o motivo de cada versão;
// - comparação de versões lado a lado (US-015, critério 3): mesma técnica do
//   DataTable transposto da T-027, com destaque nas diferenças.

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
  return new Date(iso).toLocaleDateString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "2-digit",
  });
}

function formatNumber(value: number, suffix = ""): string {
  return `${value.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}${suffix}`;
}

function shortId(uuid: string): string {
  return `${uuid.slice(0, 8)}…`;
}

// Ação ofertada pelo card da versão atual = sempre a próxima transição do
// fluxo travado (US-017). Os textos do modal de confirmação explicam a
// consequência — a transição não tem volta.
const TRANSITION_ACTION: Record<DietStatus, string> = {
  DRAFT: "Enviar para revisão",
  IN_REVIEW: "Aprovar dieta",
  APPROVED: "Arquivar dieta",
  ARCHIVED: "",
};

type TransitionCopy = {
  title: string;
  body: (version: number) => string;
};

const TRANSITION_COPY: Record<DietStatus, TransitionCopy> = {
  DRAFT: {
    title: "Enviar para revisão",
    body: (n) =>
      `A v${n} sai de Rascunho e vai para Em Revisão. O fluxo é travado: depois disso só é possível aprovar — qualquer correção gera uma nova versão e congela a v${n}.`,
  },
  IN_REVIEW: {
    title: "Aprovar dieta",
    body: (n) =>
      `A v${n} sai de Em Revisão e vira a versão aprovada (vigente) da dieta do atleta. Mudanças futuras geram novas versões a partir dela.`,
  },
  APPROVED: {
    title: "Arquivar dieta",
    body: (n) =>
      `A v${n} sai de Aprovada e vai para Arquivada — estado terminal do fluxo de aprovação. Um novo ciclo para o atleta começa como nova versão.`,
  },
  ARCHIVED: {
    title: "",
    body: () => "",
  },
};

type CompareRow = {
  key: string;
  label: string;
  values: (string | null)[]; // alinhado à ordem das colunas (versões)
};

function hasDiff(row: CompareRow): boolean {
  return new Set(row.values).size > 1;
}

export function AthleteDietsPanel({ athleteId }: { athleteId: string }) {
  const { data: versions, isLoading, isError } = useDiets(athleteId);
  const changeStatus = useChangeDietStatus(athleteId);
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [pendingTransition, setPendingTransition] = useState<DietStatus | null>(
    null
  );
  const [transitionError, setTransitionError] = useState(false);
  // Fecha o modal de confirmação e limpa o erro da tentativa anterior.
  function closeTransitionModal() {
    setPendingTransition(null);
    setTransitionError(false);
  }

  // soft-delete (regra inegociável 5): versões deletadas nunca aparecem.
  const active = (versions ?? [])
    .filter((v) => !v.deleted)
    .sort((a, b) => a.version - b.version);
  const current = findCurrentDietVersion(active);

  function toggleSelected(id: string) {
    setSelectedIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]
    );
  }

  async function confirmTransition() {
    if (!current || !pendingTransition) return;
    setTransitionError(false);
    try {
      await changeStatus.mutateAsync({
        versionId: current.id,
        payload: { status: pendingTransition },
      });
      setPendingTransition(null);
    } catch {
      setTransitionError(true);
    }
  }

  const selected = active.filter((v) => selectedIds.includes(v.id));

  const listColumns: Column<DietVersion>[] = [
    {
      key: "compare",
      header: "Comparar",
      render: (v) => (
        <input
          type="checkbox"
          checked={selectedIds.includes(v.id)}
          onChange={() => toggleSelected(v.id)}
          aria-label={`Selecionar versão v${v.version} para comparação`}
        />
      ),
    },
    {
      key: "version",
      header: "Versão",
      render: (v) => {
        const isCurrent = current?.id === v.id;
        return (
          <span
            className={
              "diets__ver" +
              (isCurrent ? " diets__ver--current" : "") +
              (v.readOnly ? " diets__ver--frozen" : "")
            }
            title={
              v.readOnly
                ? `v${v.version} imutável — congelada quando uma versão posterior foi criada`
                : isCurrent
                  ? "Versão atual da dieta"
                  : undefined
            }
          >
            v{v.version}
          </span>
        );
      },
    },
    {
      key: "status",
      header: "Status",
      render: (v) => <StatusBadge status={v.status} />,
    },
    {
      key: "objective",
      header: "Objetivo",
      render: (v) => OBJECTIVE_LABEL[v.objective],
    },
    {
      key: "calories",
      header: "Calorias",
      render: (v) => (
        <span className="diets__mono">
          {formatNumber(v.macros.calories, " kcal")}
        </span>
      ),
    },
    {
      key: "audit",
      header: "Auditoria",
      render: (v) => (
        <span className="diets__audit">
          <span
            className="diets__audit-meta"
            title={`UUID completo: ${v.createdBy}`}
          >
            {formatInstant(v.createdAt)} · por {shortId(v.createdBy)}
          </span>
          <span className="diets__audit-reason">
            {v.changeReason ? `Motivo: ${v.changeReason}` : "—"}
          </span>
        </span>
      ),
    },
  ];

  // US-015, critério 3: comparação lado a lado do conteúdo (objetivo +
  // macros) e da trilha de auditoria entre as versões selecionadas.
  const compareRows: CompareRow[] = [
    {
      key: "objective",
      label: "Objetivo",
      values: selected.map((v) => OBJECTIVE_LABEL[v.objective]),
    },
    {
      key: "calories",
      label: "Calorias (kcal)",
      values: selected.map((v) => formatNumber(v.macros.calories)),
    },
    {
      key: "protein",
      label: "Proteínas (g)",
      values: selected.map((v) => formatNumber(v.macros.protein)),
    },
    {
      key: "carbohydrates",
      label: "Carboidratos (g)",
      values: selected.map((v) => formatNumber(v.macros.carbohydrates)),
    },
    {
      key: "fat",
      label: "Gorduras (g)",
      values: selected.map((v) => formatNumber(v.macros.fat)),
    },
    {
      key: "status",
      label: "Status",
      values: selected.map((v) => STATUS_LABEL[v.status]),
    },
    {
      key: "changeReason",
      label: "Motivo da alteração",
      values: selected.map((v) => v.changeReason),
    },
  ];

  const compareColumns: Column<CompareRow>[] = [
    {
      key: "field",
      header: "Campo",
      render: (row) => (
        <span className={hasDiff(row) ? "diets__diff" : undefined}>
          {row.label}
        </span>
      ),
    },
    ...selected.map((v, i) => ({
      key: v.id,
      header: `v${v.version} · ${formatShortDate(v.createdAt)}`,
      render: (row: CompareRow) => (
        <span className={hasDiff(row) ? "diets__diff" : undefined}>
          {row.values[i] ?? "—"}
        </span>
      ),
    })),
  ];

  const nextTransition = current ? DIET_STATUS_FLOW[current.status] : null;

  return (
    <div className="diets">
      <div className="diets__header">
        <h2 className="diets__title">Dieta</h2>
      </div>

      {isError && (
        <p className="diets__error" role="alert">
          Não foi possível carregar a dieta. Tente novamente.
        </p>
      )}

      {isLoading ? (
        <p className="diets__loading">Carregando dieta...</p>
      ) : active.length === 0 ? (
        <div className="diets__empty">
          <p>Nenhuma dieta registrada para este atleta.</p>
          <p className="diets__empty-hint">
            Dietas são versionadas: nenhuma alteração sobrescreve — toda edição
            cria uma nova versão (v1 → v2 → v3…), com motivo registrado.
          </p>
          <ButtonLink to={`/coach/athletes/${athleteId}/diets/new`}>Criar dieta</ButtonLink>
        </div>
      ) : (
        <>
          {current && (
            <section className="diets__current" aria-label="Versão atual">
              <div className="diets__current-row">
                <span className="diets__ver diets__ver--current">
                  v{current.version}
                </span>
                <StatusBadge status={current.status} />
                <span className="diets__meta">
                  versão atual · criada em {formatInstant(current.createdAt)}{" "}
                  por {shortId(current.createdBy)}
                </span>
              </div>
              {nextTransition && (
                <p className="diets__meta">
                  Próxima transição permitida: {STATUS_LABEL[current.status]} →{" "}
                  {STATUS_LABEL[nextTransition]} (o fluxo de status é travado).
                </p>
              )}
              <div className="diets__current-actions">
                <ButtonLink
                  to={`/coach/athletes/${athleteId}/diets/${current.id}/new-version`} variant="ghost">Nova versão (não sobrescreve)</ButtonLink>
                {nextTransition ? (
                  <Button onClick={() => setPendingTransition(nextTransition)}>
                    {TRANSITION_ACTION[nextTransition]}
                  </Button>
                ) : (
                  <p className="diets__flow-end">
                    Estado terminal (Arquivada) — sem transições restantes.
                    Alterações geram nova versão.
                  </p>
                )}
              </div>
            </section>
          )}

          <h3 className="diets__section-title">
            Histórico de versões
            {current && (
              <span className="diets__meta">
                {" "}
                — versões anteriores são somente leitura
              </span>
            )}
          </h3>
          <DataTable
            columns={listColumns}
            rows={active}
            rowKey={(v) => v.id}
            emptyMessage="Nenhuma versão registrada."
          />

          {selected.length >= 2 && (
            <>
              <h3 className="diets__section-title">
                Comparação ({selected.length} versões)
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

      <Modal
        open={pendingTransition !== null}
        title={pendingTransition ? TRANSITION_COPY[pendingTransition].title : ""}
        onClose={closeTransitionModal}
      >
        {pendingTransition && current && (
          <>
            <p>
              <StatusBadge status={current.status} /> →{" "}
              <StatusBadge status={pendingTransition} />
            </p>
            <p>{TRANSITION_COPY[pendingTransition].body(current.version)}</p>
            {transitionError && (
              <p className="diets__error" role="alert">
                Não foi possível concluir a transição. Verifique o estado atual da
                versão.
              </p>
            )}
            <div className="pc-modal__actions">
              <Button
                type="button"
                variant="ghost"
                onClick={closeTransitionModal}
              >
                Cancelar
              </Button>
              <Button
                type="button"
                loading={changeStatus.isPending}
                onClick={confirmTransition}
              >
                Confirmar
              </Button>
            </div>
          </>
        )}
      </Modal>
    </div>
  );
}
