import { FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { SelectField } from "@/components/ui/SelectField";
import { StatusBadge } from "@/components/ui/StatusBadge";
import {
  Periodization,
  PlanComparison,
  TrainingPlan,
  TrainingSession,
  TrainingStatus,
} from "@/lib/training";
import {
  DAY_LABELS,
  DAY_OPTIONS,
  PLAN_STATUS_ACTION,
  STATUS_NEXT,
  errorMessage,
  formatDate,
} from "./training-constants";

interface PlanOverviewViewProps {
  athleteId: string;
  periodizationId: string;
  planId: string;
  sessionId: string;
  selectedPeriodization: Periodization | undefined;
  plans: TrainingPlan[];
  selectedPlan: TrainingPlan | undefined;
  previousPlan: TrainingPlan | undefined;
  sessions: TrainingSession[];
  planComparison: PlanComparison | null;
  plansLoading: boolean;
  plansError: boolean;
  createPlanPending: boolean;
  createPlanError: unknown;
  transitionPlanPending: boolean;
  transitionPlanError: unknown;
  createSessionPending: boolean;
  createSessionError: unknown;
  onCreatePlan: (event: FormEvent<HTMLFormElement>) => void;
  onCreateSession: (event: FormEvent<HTMLFormElement>) => void;
  onSelectSession: (id: string) => void;
  onAddExerciseShortcut?: (id: string) => void;
  onOpenVersioning: () => void;
  onOpenComparing: () => void;
  onTransitionPlan: (targetStatus: TrainingStatus) => void;
}

export function PlanOverviewView({
  selectedPeriodization,
  plans,
  selectedPlan,
  previousPlan,
  sessions,
  sessionId,
  planComparison,
  plansLoading,
  plansError,
  createPlanPending,
  createPlanError,
  transitionPlanPending,
  transitionPlanError,
  createSessionPending,
  createSessionError,
  onCreatePlan,
  onCreateSession,
  onSelectSession,
  onAddExerciseShortcut,
  onOpenVersioning,
  onOpenComparing,
  onTransitionPlan,
}: PlanOverviewViewProps) {
  return (
    <>
      {selectedPeriodization && plans.length === 0 && !plansLoading && !plansError && (
        <form className="training-page__form training-page__form--inline" onSubmit={onCreatePlan}>
          <h3>Criar plano v1</h3>
          <div className="training-page__form-grid">
            <Field name="name" label="Nome do plano" required placeholder="Ex.: Hipertrofia — bloco 1" />
            <Field name="description" label="Descrição do plano" placeholder="Foco e orientação do plano" />
          </div>
          {Boolean(createPlanError) && (
            <p className="training-page__alert" role="alert">
              {errorMessage(createPlanError, "Não foi possível criar o plano.")}
            </p>
          )}
          <Button type="submit" loading={createPlanPending}>Criar plano</Button>
        </form>
      )}

      {selectedPlan && (
        <div className="training-page__plan-summary">
          <div>
            <span className="training-page__mono">VERSÃO {selectedPlan.version}</span>
            <h3>{selectedPlan.name}</h3>
            {selectedPlan.description && <p>{selectedPlan.description}</p>}
          </div>
          <div className="training-page__plan-actions">
            <StatusBadge status={selectedPlan.status} />
            {previousPlan && (
              <Button
                variant="ghost"
                disabled={!planComparison}
                onClick={onOpenComparing}
              >
                Comparar com v{previousPlan.version}
              </Button>
            )}
            <Button
              variant="ghost"
              onClick={onOpenVersioning}
            >
              Criar nova versão
            </Button>
            {STATUS_NEXT[selectedPlan.status] && (
              <Button
                variant="ghost"
                loading={transitionPlanPending}
                onClick={() => onTransitionPlan(STATUS_NEXT[selectedPlan.status]!)}
              >
                {PLAN_STATUS_ACTION[STATUS_NEXT[selectedPlan.status]!]}
              </Button>
            )}
          </div>
          {selectedPlan.status === "DRAFT" && (
            <p className="training-page__notice" role="status">
              Rascunho: o atleta ainda não vê esta versão. Envie para revisão e publique para liberar as sessões.
            </p>
          )}
          {selectedPlan.status === "IN_REVIEW" && (
            <p className="training-page__notice" role="status">
              Em revisão: ao publicar, esta passa a ser a versão corrente do atleta. A versão anterior continua no histórico.
            </p>
          )}
          {Boolean(transitionPlanError) && (
            <p className="training-page__alert" role="alert">
              {errorMessage(transitionPlanError, "Não foi possível alterar o status do plano.")}
            </p>
          )}
        </div>
      )}

      {selectedPlan && selectedPeriodization && (
        <section className="training-page__section" aria-labelledby="training-sessions-title">
          <div className="training-page__section-heading">
            <div>
              <p className="training-page__eyebrow">02 / Cronograma</p>
              <h2 id="training-sessions-title">Sessões da periodização</h2>
              <p className="training-page__muted">
                {formatDate(selectedPeriodization.startDate)} — {formatDate(selectedPeriodization.endDate)}
              </p>
            </div>
          </div>

          {sessions.length > 0 && (
            <ol className="training-page__schedule">
              {sessions.map((session) => (
                <li
                  key={session.id}
                  className={
                    session.id === sessionId
                      ? "training-page__schedule-item training-page__schedule-item--selected"
                      : "training-page__schedule-item"
                  }
                >
                  <button
                    type="button"
                    aria-current={session.id === sessionId ? "true" : undefined}
                    className={
                      session.id === sessionId
                        ? "training-page__session training-page__session--selected"
                        : "training-page__session"
                    }
                    onClick={() => onSelectSession(session.id)}
                  >
                    <span className="training-page__week">SEM {session.weekNumber}</span>
                    <span className="training-page__session-name">
                      <strong>{session.name}</strong>
                      <small>
                        {DAY_LABELS[String(session.dayOfWeek)]} · {session.exercises.length} exercícios
                      </small>
                    </span>
                    <StatusBadge status={session.status} />
                  </button>
                  <div className="training-page__schedule-item-actions">
                    <Button
                      type="button"
                      variant="ghost"
                      disabled={session.status === "FINISHED"}
                      onClick={() =>
                        onAddExerciseShortcut
                          ? onAddExerciseShortcut(session.id)
                          : onSelectSession(session.id)
                      }
                      title={
                        session.status === "FINISHED"
                          ? "Sessão finalizada não permite novos exercícios"
                          : "Adicionar exercício a esta sessão"
                      }
                    >
                      + Adicionar exercício
                    </Button>
                  </div>
                </li>
              ))}
            </ol>
          )}
          {sessions.length === 0 && (
            <p className="training-page__muted">Ainda não há sessões nesta versão do plano.</p>
          )}

          <form className="training-page__form training-page__form--inline" onSubmit={onCreateSession}>
            <h3>Adicionar sessão ao cronograma</h3>
            <div className="training-page__form-grid training-page__form-grid--session">
              <Field name="name" label="Nome da sessão" required placeholder="Ex.: Treino A — superiores" />
              <Field name="description" label="Descrição" placeholder="Foco da sessão" />
              <Field name="weekNumber" label="Semana" type="number" min="1" step="1" required />
              <SelectField name="dayOfWeek" label="Dia" options={DAY_OPTIONS} placeholder="Selecione o dia" required />
            </div>
            {Boolean(createSessionError) && (
              <p className="training-page__alert" role="alert">
                {errorMessage(createSessionError, "Não foi possível criar a sessão.")}
              </p>
            )}
            <Button type="submit" loading={createSessionPending}>Adicionar sessão</Button>
          </form>
        </section>
      )}
    </>
  );
}
