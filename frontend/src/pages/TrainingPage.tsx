import { FormEvent, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { COACH_NAV } from "@/lib/coach-nav";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { SelectField } from "@/components/ui/SelectField";
import { useAthletes } from "@/lib/athletes";
import {
  AddExercisePayload,
  ExercisePrescription,
  MuscleGroup,
  TrainingSession,
  TrainingStatus,
  UpdateExercisePrescriptionPayload,
  comparePlanVersions,
  useAddExerciseToSession,
  useApplyCurrentExerciseVersion,
  useAthleteTrainingSessions,
  useCreatePeriodization,
  useCreateTrainingPlan,
  useCreateTrainingPlanVersion,
  useCreateTrainingSession,
  useExercises,
  usePeriodizations,
  useRemoveExercisePrescription,
  useReorderSessionExercises,
  useTrainingPlans,
  useTransitionTrainingPlan,
  useTransitionTrainingSession,
  useUpdateExercisePrescription,
} from "@/lib/training";
import { PlanComparisonModal } from "./training/PlanComparisonModal";
import { PlanOverviewView } from "./training/PlanOverviewView";
import { PlanVersionModal } from "./training/PlanVersionModal";
import { SessionEditorView } from "./training/SessionEditorView";
import { errorMessage, formatDate } from "./training/training-constants";
import "./TrainingPage.css";

export function TrainingPage() {
  const [searchParams, setSearchParams] = useSearchParams();

  const rawAthleteId = searchParams.get("athlete") ?? "";
  const rawPeriodizationId = searchParams.get("periodization") ?? "";
  const rawPlanId = searchParams.get("plan") ?? "";
  const rawSessionId = searchParams.get("session") ?? "";

  const [periodizationDateError, setPeriodizationDateError] = useState(false);
  const [versioningPlan, setVersioningPlan] = useState(false);
  const [comparingPlan, setComparingPlan] = useState(false);
  const [focusAddExerciseToken, setFocusAddExerciseToken] = useState(0);
  const [searchQuery, setSearchQuery] = useState<{
    name: string;
    muscle: MuscleGroup | "";
  }>({ name: "", muscle: "" });

  const athletesQuery = useAthletes();

  // Validação: se o ID na URL não existe na lista carregada, marca como inválido
  const isAthleteInvalid = Boolean(
    rawAthleteId && athletesQuery.isSuccess && !athletesQuery.data.some((a) => a.id === rawAthleteId)
  );
  const athleteId = isAthleteInvalid ? "" : rawAthleteId;

  const periodizationsQuery = usePeriodizations(athleteId);
  const periodizations = periodizationsQuery.data ?? [];

  const isPeriodizationInvalid = Boolean(
    rawPeriodizationId &&
      periodizationsQuery.isSuccess &&
      !periodizations.some((p) => p.id === rawPeriodizationId)
  );
  const periodizationId = isPeriodizationInvalid ? "" : rawPeriodizationId;

  const plansQuery = useTrainingPlans(athleteId);
  const plans = (plansQuery.data ?? []).filter((plan) => plan.periodizationId === periodizationId);

  const isPlanInvalid = Boolean(
    rawPlanId && plansQuery.isSuccess && !plans.some((p) => p.id === rawPlanId)
  );
  const planId = isPlanInvalid ? "" : rawPlanId;

  const sessionsQuery = useAthleteTrainingSessions(athleteId, planId);
  const sessions = useMemo(
    () =>
      (sessionsQuery.data ?? [])
        .slice()
        .sort((a, b) => a.weekNumber - b.weekNumber || a.dayOfWeek - b.dayOfWeek),
    [sessionsQuery.data]
  );

  const isSessionInvalid = Boolean(
    rawSessionId && sessionsQuery.isSuccess && !sessions.some((s) => s.id === rawSessionId)
  );
  const sessionId = isSessionInvalid ? "" : rawSessionId;

  function updateSelection(params: {
    athlete?: string;
    periodization?: string;
    plan?: string;
    session?: string;
  }) {
    const next = new URLSearchParams();
    if (params.athlete) next.set("athlete", params.athlete);
    if (params.periodization) next.set("periodization", params.periodization);
    if (params.plan) next.set("plan", params.plan);
    if (params.session) next.set("session", params.session);
    setSearchParams(next);
  }

  function handleAddExerciseShortcut(targetSessionId: string) {
    updateSelection({
      athlete: athleteId,
      periodization: periodizationId,
      plan: planId,
      session: targetSessionId,
    });
    setFocusAddExerciseToken((prev) => prev + 1);
  }

  const exercisesQuery = useExercises(
    searchQuery.name,
    searchQuery.muscle,
    Boolean(sessionId)
  );

  const createPeriodization = useCreatePeriodization(athleteId);
  const createPlan = useCreateTrainingPlan(athleteId);
  const createPlanVersion = useCreateTrainingPlanVersion(athleteId, periodizationId);
  const createSession = useCreateTrainingSession(athleteId, periodizationId);
  const addExercise = useAddExerciseToSession(periodizationId, sessionId);
  const reorderExercises = useReorderSessionExercises(periodizationId, sessionId);
  const updatePrescription = useUpdateExercisePrescription(periodizationId, sessionId);
  const removePrescription = useRemoveExercisePrescription(periodizationId, sessionId);
  const applyCurrentVersion = useApplyCurrentExerciseVersion(periodizationId, sessionId);
  const transitionPlan = useTransitionTrainingPlan(athleteId, periodizationId);
  const transitionSession = useTransitionTrainingSession(periodizationId, sessionId);

  const selectedPeriodization = periodizations.find((item) => item.id === periodizationId);
  const selectedPlan = plans.find((item) => item.id === planId);
  const selectedSession = sessions.find((item) => item.id === sessionId);

  const previousPlan = selectedPlan?.previousVersionId
    ? plans.find((plan) => plan.id === selectedPlan.previousVersionId)
    : undefined;
  const previousPlanSessionsQuery = useAthleteTrainingSessions(
    athleteId,
    previousPlan?.id ?? ""
  );
  const previousPlanSessions = previousPlanSessionsQuery.data ?? [];
  const planComparison =
    selectedPlan && previousPlan
      ? comparePlanVersions(
          previousPlan,
          selectedPlan,
          previousPlanSessions,
          sessions
        )
      : null;

  async function onCreatePeriodization(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const startDate = String(form.get("startDate"));
    const endDate = String(form.get("endDate"));
    if (endDate < startDate) {
      setPeriodizationDateError(true);
      return;
    }
    setPeriodizationDateError(false);
    try {
      const created = await createPeriodization.mutateAsync({
        name: String(form.get("name")),
        description: String(form.get("description") ?? ""),
        startDate,
        endDate,
      });
      updateSelection({ athlete: athleteId, periodization: created.id });
      formElement.reset();
    } catch {
      // A mensagem da mutação aparece junto ao formulário.
    }
  }

  async function onCreatePlan(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    try {
      const created = await createPlan.mutateAsync({
        periodizationId,
        name: String(form.get("name")),
        description: String(form.get("description") ?? ""),
      });
      updateSelection({ athlete: athleteId, periodization: periodizationId, plan: created.id });
      formElement.reset();
    } catch {
      // A mensagem da mutação aparece junto ao formulário.
    }
  }

  async function onCreatePlanVersion(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedPlan) return;
    const form = new FormData(event.currentTarget);
    const strategy = String(form.get("strategy") ?? "duplicate");
    const copySessions = strategy !== "blank";
    try {
      const created = await createPlanVersion.mutateAsync({
        planId: selectedPlan.id,
        newName: String(form.get("newName") ?? "").trim(),
        newDescription: String(form.get("newDescription") ?? "").trim(),
        copySessions,
      });
      updateSelection({ athlete: athleteId, periodization: periodizationId, plan: created.id });
      setVersioningPlan(false);
      setComparingPlan(false);
    } catch {
      // A mensagem da mutação aparece dentro do modal.
    }
  }

  async function onCreateSession(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    try {
      const created = await createSession.mutateAsync({
        planId,
        name: String(form.get("name")),
        description: String(form.get("description") ?? ""),
        weekNumber: Number(form.get("weekNumber")),
        dayOfWeek: Number(form.get("dayOfWeek")),
      });
      updateSelection({
        athlete: athleteId,
        periodization: periodizationId,
        plan: planId,
        session: created.id,
      });
      formElement.reset();
    } catch {
      // A mensagem da mutação aparece junto ao formulário.
    }
  }

  return (
    <AppShell navItems={COACH_NAV}>
      <div className="training-page">
        <header className="training-page__header">
          <div>
            <p className="training-page__eyebrow">Planejamento</p>
            <h1>Treinos e biblioteca</h1>
            <p className="training-page__intro">
              Organize um ciclo, monte sessões e prescreva exercícios do catálogo. Cada sessão fica ligada à versão do plano.
            </p>
          </div>
        </header>

        {isAthleteInvalid && (
          <p className="training-page__alert" role="alert">
            Atleta informado na URL não foi encontrado ou você não tem permissão para acessá-lo.
          </p>
        )}
        {isPeriodizationInvalid && (
          <p className="training-page__alert" role="alert">
            Periodização informada na URL não foi encontrada para este atleta.
          </p>
        )}
        {isPlanInvalid && (
          <p className="training-page__alert" role="alert">
            Plano informado na URL não foi encontrado nesta periodização.
          </p>
        )}
        {isSessionInvalid && (
          <p className="training-page__alert" role="alert">
            Sessão informada na URL não foi encontrada neste plano.
          </p>
        )}

        {athletesQuery.isError && (
          <p className="training-page__alert" role="alert">Não foi possível carregar os atletas.</p>
        )}

        <section className="training-page__section" aria-labelledby="training-context-title">
          <div className="training-page__section-heading">
            <div>
              <p className="training-page__eyebrow">01 / Contexto</p>
              <h2 id="training-context-title">Escolha o atleta e o plano</h2>
            </div>
          </div>

          <div className="training-page__selectors">
            <SelectField
              label="Atleta"
              value={athleteId}
              onChange={(event) => updateSelection({ athlete: event.target.value })}
              placeholder={athletesQuery.isLoading ? "Carregando atletas…" : "Selecione um atleta"}
              options={(athletesQuery.data ?? []).map((athlete) => ({ value: athlete.id, label: athlete.name }))}
              disabled={athletesQuery.isLoading}
            />
            {athleteId && (
              <SelectField
                label="Periodização"
                value={periodizationId}
                onChange={(event) =>
                  updateSelection({ athlete: athleteId, periodization: event.target.value })
                }
                placeholder="Selecione uma periodização"
                options={periodizations.map((item) => ({
                  value: item.id,
                  label: `${item.name} · ${formatDate(item.startDate)}–${formatDate(item.endDate)}`,
                }))}
                disabled={periodizationsQuery.isLoading}
              />
            )}
            {periodizationId && (
              <SelectField
                label="Versão do plano"
                value={planId}
                onChange={(event) =>
                  updateSelection({
                    athlete: athleteId,
                    periodization: periodizationId,
                    plan: event.target.value,
                  })
                }
                placeholder="Selecione uma versão"
                options={plans.map((item) => ({
                  value: item.id,
                  label: `${item.name} · v${item.version} · ${
                    item.status === "ACTIVE"
                      ? "Ativo"
                      : item.status === "FINISHED"
                      ? "Finalizado"
                      : item.status === "IN_REVIEW"
                      ? "Em revisão"
                      : "Rascunho"
                  }`,
                }))}
                disabled={plansQuery.isLoading}
              />
            )}
          </div>

          {(periodizationsQuery.isError || plansQuery.isError || sessionsQuery.isError) && (
            <p className="training-page__alert" role="alert">
              Não foi possível carregar todos os dados do plano. Atualize a página e tente novamente.
            </p>
          )}

          {athleteId && !periodizationsQuery.isLoading && !periodizationsQuery.isError && (
            <form key={`periodization-${athleteId}`} className="training-page__form training-page__form--inline" onSubmit={onCreatePeriodization}>
              <h3>Criar periodização</h3>
              <div className="training-page__form-grid">
                <Field name="name" label="Nome da periodização" required placeholder="Ex.: Macrociclo de hipertrofia" />
                <Field name="description" label="Descrição da periodização" placeholder="Objetivo e estrutura do ciclo" />
                <Field name="startDate" label="Data de início" type="date" required />
                <Field name="endDate" label="Data de término" type="date" required />
              </div>
              {periodizationDateError && <p className="training-page__alert" role="alert">A data de término deve ser igual ou posterior ao início.</p>}
              {createPeriodization.isError && <p className="training-page__alert" role="alert">{errorMessage(createPeriodization.error, "Não foi possível criar a periodização.")}</p>}
              <Button type="submit" loading={createPeriodization.isPending}>Criar periodização</Button>
            </form>
          )}

          <PlanOverviewView
            athleteId={athleteId}
            periodizationId={periodizationId}
            planId={planId}
            sessionId={sessionId}
            selectedPeriodization={selectedPeriodization}
            plans={plans}
            selectedPlan={selectedPlan}
            previousPlan={previousPlan}
            sessions={sessions}
            planComparison={planComparison}
            plansLoading={plansQuery.isLoading}
            plansError={plansQuery.isError}
            createPlanPending={createPlan.isPending}
            createPlanError={createPlan.error}
            transitionPlanPending={transitionPlan.isPending}
            transitionPlanError={transitionPlan.error}
            createSessionPending={createSession.isPending}
            createSessionError={createSession.error}
            onCreatePlan={onCreatePlan}
            onCreateSession={onCreateSession}
            onSelectSession={(id) =>
              updateSelection({
                athlete: athleteId,
                periodization: periodizationId,
                plan: planId,
                session: id,
              })
            }
            onAddExerciseShortcut={handleAddExerciseShortcut}
            onOpenVersioning={() => setVersioningPlan(true)}
            onOpenComparing={() => setComparingPlan(true)}
            onTransitionPlan={(targetStatus: TrainingStatus) => {
              if (selectedPlan) {
                transitionPlan.mutate({ planId: selectedPlan.id, targetStatus });
              }
            }}
          />
        </section>

        {selectedSession && (
          <SessionEditorView
            selectedSession={selectedSession}
            exercises={exercisesQuery.data ?? []}
            exercisesLoading={exercisesQuery.isLoading}
            exercisesError={exercisesQuery.isError}
            autoFocusAddExercise={focusAddExerciseToken > 0}
            transitionSessionPending={transitionSession.isPending}
            transitionSessionError={transitionSession.error}
            reorderExercisesPending={reorderExercises.isPending}
            reorderExercisesError={reorderExercises.error}
            applyCurrentVersionPending={applyCurrentVersion.isPending}
            applyCurrentVersionError={applyCurrentVersion.error}
            addExercisePending={addExercise.isPending}
            addExerciseError={addExercise.error}
            updatePrescriptionPending={updatePrescription.isPending}
            updatePrescriptionError={updatePrescription.error}
            removePrescriptionPending={removePrescription.isPending}
            removePrescriptionError={removePrescription.error}
            onSearchExercises={(name, muscle) => setSearchQuery({ name, muscle })}
            onTransitionSession={(targetStatus: TrainingSession["status"]) =>
              transitionSession.mutate(targetStatus)
            }
            onReorderExercises={(prescriptionIds: string[]) =>
              reorderExercises.mutate(prescriptionIds)
            }
            onApplyCurrentVersion={async (exercise: ExercisePrescription) => {
              await applyCurrentVersion.mutateAsync(exercise);
            }}
            onAddExercise={async (payload: AddExercisePayload) => {
              await addExercise.mutateAsync(payload);
            }}
            onUpdatePrescription={async (
              prescriptionId: string,
              payload: UpdateExercisePrescriptionPayload
            ) => {
              await updatePrescription.mutateAsync({ prescriptionId, payload });
            }}
            onRemovePrescription={async (prescriptionId: string) => {
              await removePrescription.mutateAsync(prescriptionId);
            }}
          />
        )}

        <PlanVersionModal
          open={versioningPlan && selectedPlan !== undefined}
          selectedPlan={selectedPlan}
          sessionsCount={sessions.length}
          isPending={createPlanVersion.isPending}
          error={createPlanVersion.error}
          onClose={() => setVersioningPlan(false)}
          onSubmit={onCreatePlanVersion}
        />

        <PlanComparisonModal
          open={comparingPlan && planComparison !== null}
          comparison={planComparison}
          onClose={() => setComparingPlan(false)}
        />
      </div>
    </AppShell>
  );
}
