import { FormEvent, useMemo, useState } from "react";
import { AppShell } from "@/layouts/AppShell";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { SelectField } from "@/components/ui/SelectField";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { useAthletes } from "@/lib/athletes";
import {
  AddExercisePayload,
  Exercise,
  IntensityType,
  MUSCLE_GROUPS,
  MuscleGroup,
  TrainingStatus,
  useAddExerciseToSession,
  useCreatePeriodization,
  useCreateTrainingPlan,
  useCreateTrainingSession,
  useExercises,
  usePeriodizations,
  useReorderSessionExercises,
  useTrainingPlans,
  useTrainingSessions,
  useTransitionTrainingPlan,
  useTransitionTrainingSession,
} from "@/lib/training";
import "./TrainingPage.css";

const MUSCLE_LABELS: Record<MuscleGroup, string> = {
  PEITO: "Peito",
  COSTAS: "Costas",
  OMBROS: "Ombros",
  BICEPS: "Bíceps",
  TRICEPS: "Tríceps",
  ANTEBRACO: "Antebraço",
  QUADRICEPS: "Quadríceps",
  POSTERIOR_DE_COXA: "Posterior de coxa",
  GLUTEOS: "Glúteos",
  ADUTORES: "Adutores",
  ABDUTORES: "Abdutores",
  PANTURRILHA: "Panturrilha",
  ABDOMEN: "Abdômen",
  LOMBAR: "Lombar",
  TRAPECIO: "Trapézio",
  FOREARM: "Antebraço",
};

const EQUIPMENT_LABELS: Record<string, string> = {
  BARRA_LIVRE: "Barra livre",
  HALTERES: "Halteres",
  MAQUINA: "Máquina",
  CROSSOVER: "Crossover",
  POLIA: "Polia",
  BANCO: "Banco",
  PESO_CORPORAL: "Peso corporal",
  KETTLEBELL: "Kettlebell",
  BOLA_SUICA: "Bola suíça",
  ELASTICO: "Elástico",
  SMITH: "Smith",
  OUTRO: "Outro",
};

const INTENSITY_OPTIONS = [
  { value: "RPE", label: "RPE" },
  { value: "RIR", label: "RIR" },
  { value: "PERCENT_1RM", label: "% de 1RM" },
  { value: "ABSOLUTE_LOAD", label: "Carga absoluta" },
];

const DAY_OPTIONS = [
  { value: "1", label: "Segunda-feira" },
  { value: "2", label: "Terça-feira" },
  { value: "3", label: "Quarta-feira" },
  { value: "4", label: "Quinta-feira" },
  { value: "5", label: "Sexta-feira" },
  { value: "6", label: "Sábado" },
  { value: "7", label: "Domingo" },
];

const STATUS_NEXT: Partial<Record<TrainingStatus, TrainingStatus>> = {
  DRAFT: "IN_REVIEW",
  IN_REVIEW: "ACTIVE",
  ACTIVE: "FINISHED",
};

const STATUS_ACTION: Record<TrainingStatus, string> = {
  DRAFT: "Enviar para revisão",
  IN_REVIEW: "Ativar",
  ACTIVE: "Finalizar",
  FINISHED: "Finalizado",
};

const DAY_LABELS = Object.fromEntries(DAY_OPTIONS.map((day) => [day.value, day.label]));

function formatDate(date: string | null) {
  if (!date) return "Data não informada";
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium", timeZone: "UTC" }).format(
    new Date(`${date}T00:00:00Z`)
  );
}

function movePrescription(ids: string[], index: number, offset: number) {
  const next = [...ids];
  const targetIndex = index + offset;
  if (targetIndex < 0 || targetIndex >= next.length) return next;
  [next[index], next[targetIndex]] = [next[targetIndex], next[index]];
  return next;
}

function errorMessage(error: unknown, fallback: string) {
  if (error && typeof error === "object" && "response" in error) {
    const status = (error as { response?: { status?: number } }).response?.status;
    if (status === 401) return "Sua sessão expirou. Entre novamente para continuar.";
    if (status === 403) return "Você não tem acesso a este conteúdo.";
    if (status === 409) return "O estado mudou no servidor. Atualize os dados e tente novamente.";
  }
  return fallback;
}

export function TrainingPage() {
  const [athleteId, setAthleteId] = useState("");
  const [periodizationId, setPeriodizationId] = useState("");
  const [planId, setPlanId] = useState("");
  const [sessionId, setSessionId] = useState("");
  const [libraryName, setLibraryName] = useState("");
  const [libraryMuscle, setLibraryMuscle] = useState<MuscleGroup | "">("");
  const [submittedName, setSubmittedName] = useState("");
  const [submittedMuscle, setSubmittedMuscle] = useState<MuscleGroup | "">("");
  const [exerciseId, setExerciseId] = useState("");
  const [periodizationDateError, setPeriodizationDateError] = useState(false);

  const athletesQuery = useAthletes();
  const periodizationsQuery = usePeriodizations(athleteId);
  const plansQuery = useTrainingPlans(athleteId);
  const sessionsQuery = useTrainingSessions(periodizationId);
  const exercisesQuery = useExercises(submittedName, submittedMuscle, Boolean(sessionId));

  const createPeriodization = useCreatePeriodization(athleteId);
  const createPlan = useCreateTrainingPlan(athleteId);
  const createSession = useCreateTrainingSession(athleteId, periodizationId);
  const addExercise = useAddExerciseToSession(periodizationId, sessionId);
  const reorderExercises = useReorderSessionExercises(periodizationId, sessionId);
  const transitionPlan = useTransitionTrainingPlan(athleteId, periodizationId);
  const transitionSession = useTransitionTrainingSession(periodizationId, sessionId);

  const periodizations = periodizationsQuery.data ?? [];
  const plans = (plansQuery.data ?? []).filter((plan) => plan.periodizationId === periodizationId);
  const sessions = useMemo(
    () =>
      (sessionsQuery.data ?? [])
        .filter((session) => session.planId === planId)
        .sort((a, b) => a.weekNumber - b.weekNumber || a.dayOfWeek - b.dayOfWeek),
    [sessionsQuery.data, planId]
  );
  const selectedPeriodization = periodizations.find((item) => item.id === periodizationId);
  const selectedPlan = plans.find((item) => item.id === planId);
  const selectedSession = sessions.find((item) => item.id === sessionId);
  const selectedExercise = exercisesQuery.data?.find((exercise) => exercise.id === exerciseId);

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
      setPeriodizationId(created.id);
      setPlanId("");
      setSessionId("");
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
      setPlanId(created.id);
      setSessionId("");
      formElement.reset();
    } catch {
      // A mensagem da mutação aparece junto ao formulário.
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
      setSessionId(created.id);
      formElement.reset();
    } catch {
      // A mensagem da mutação aparece junto ao formulário.
    }
  }

  async function onAddExercise(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedExercise || !selectedSession) return;
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const payload: AddExercisePayload = {
      exerciseId: selectedExercise.id,
      orderIndex:
        Math.max(-1, ...selectedSession.exercises.map((exercise) => exercise.orderIndex)) + 1,
      sets: Number(form.get("sets")),
      repetitions: Number(form.get("repetitions")),
      load: Number(form.get("load")),
      intensityType: String(form.get("intensityType")) as IntensityType,
      intensityValue: form.get("intensityValue") ? Number(form.get("intensityValue")) : null,
      restSeconds: Number(form.get("restSeconds")),
    };
    try {
      const updated = await addExercise.mutateAsync(payload);
      setSessionId(updated.id);
      setExerciseId("");
      formElement.reset();
    } catch {
      // A mensagem da mutação aparece junto ao formulário.
    }
  }

  function onChangeAthlete(nextAthleteId: string) {
    setAthleteId(nextAthleteId);
    setPeriodizationId("");
    setPlanId("");
    setSessionId("");
  }

  function onChangePeriodization(nextPeriodizationId: string) {
    setPeriodizationId(nextPeriodizationId);
    setPlanId("");
    setSessionId("");
  }

  function onChangePlan(nextPlanId: string) {
    setPlanId(nextPlanId);
    setSessionId("");
  }

  const navItems = [
    { label: "Atletas", href: "/coach/athletes" },
    { label: "Treinos e biblioteca", href: "/coach/training" },
  ];

  return (
    <AppShell navItems={navItems}>
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
              onChange={(event) => onChangeAthlete(event.target.value)}
              placeholder={athletesQuery.isLoading ? "Carregando atletas…" : "Selecione um atleta"}
              options={(athletesQuery.data ?? []).map((athlete) => ({ value: athlete.id, label: athlete.name }))}
              disabled={athletesQuery.isLoading}
            />
            {athleteId && (
              <SelectField
                label="Periodização"
                value={periodizationId}
                onChange={(event) => onChangePeriodization(event.target.value)}
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
                onChange={(event) => onChangePlan(event.target.value)}
                placeholder="Selecione uma versão"
                options={plans.map((item) => ({
                  value: item.id,
                  label: `${item.name} · v${item.version} · ${item.status === "ACTIVE" ? "Ativo" : item.status === "FINISHED" ? "Finalizado" : item.status === "IN_REVIEW" ? "Em revisão" : "Rascunho"}`,
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

          {selectedPeriodization && plans.length === 0 && !plansQuery.isLoading && !plansQuery.isError && (
            <form className="training-page__form training-page__form--inline" onSubmit={onCreatePlan}>
              <h3>Criar plano v1</h3>
              <div className="training-page__form-grid">
                <Field name="name" label="Nome do plano" required placeholder="Ex.: Hipertrofia — bloco 1" />
                <Field name="description" label="Descrição do plano" placeholder="Foco e orientação do plano" />
              </div>
              {createPlan.isError && <p className="training-page__alert" role="alert">{errorMessage(createPlan.error, "Não foi possível criar o plano.")}</p>}
              <Button type="submit" loading={createPlan.isPending}>Criar plano</Button>
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
                {STATUS_NEXT[selectedPlan.status] && (
                  <Button
                    variant="ghost"
                    loading={transitionPlan.isPending}
                    onClick={() => transitionPlan.mutate({ planId: selectedPlan.id, targetStatus: STATUS_NEXT[selectedPlan.status]! })}
                  >
                    {STATUS_ACTION[STATUS_NEXT[selectedPlan.status]!]}
                  </Button>
                )}
              </div>
              {transitionPlan.isError && <p className="training-page__alert" role="alert">{errorMessage(transitionPlan.error, "Não foi possível alterar o status do plano.")}</p>}
            </div>
          )}
        </section>

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
                  <li key={session.id}>
                    <button
                      type="button"
                      className={session.id === sessionId ? "training-page__session training-page__session--selected" : "training-page__session"}
                      onClick={() => setSessionId(session.id)}
                    >
                      <span className="training-page__week">SEM {session.weekNumber}</span>
                      <span className="training-page__session-name">
                        <strong>{session.name}</strong>
                        <small>{DAY_LABELS[String(session.dayOfWeek)]} · {session.exercises.length} exercícios</small>
                      </span>
                      <StatusBadge status={session.status} />
                    </button>
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
              {createSession.isError && <p className="training-page__alert" role="alert">{errorMessage(createSession.error, "Não foi possível criar a sessão.")}</p>}
              <Button type="submit" loading={createSession.isPending}>Adicionar sessão</Button>
            </form>
          </section>
        )}

        {selectedSession && (
          <section className="training-page__section" aria-labelledby="session-detail-title">
            <div className="training-page__section-heading training-page__section-heading--split">
              <div>
                <p className="training-page__eyebrow">03 / Prescrição</p>
                <h2 id="session-detail-title">{selectedSession.name}</h2>
                <p className="training-page__muted">Semana {selectedSession.weekNumber} · {DAY_LABELS[String(selectedSession.dayOfWeek)]}</p>
              </div>
              <div className="training-page__plan-actions">
                <StatusBadge status={selectedSession.status} />
                {STATUS_NEXT[selectedSession.status] && (
                  <Button
                    variant="ghost"
                    loading={transitionSession.isPending}
                    onClick={() => transitionSession.mutate(STATUS_NEXT[selectedSession.status]!)}
                  >
                    {STATUS_ACTION[STATUS_NEXT[selectedSession.status]!]}
                  </Button>
                )}
              </div>
            </div>
            {selectedSession.description && <p className="training-page__muted">{selectedSession.description}</p>}
            {transitionSession.isError && <p className="training-page__alert" role="alert">{errorMessage(transitionSession.error, "Não foi possível alterar o status da sessão.")}</p>}
            {selectedSession.status === "ACTIVE" && (
              <p className="training-page__notice" role="status">
                Esta sessão está ativa. Ao adicionar ou reordenar exercícios, o atleta receberá um aviso.
              </p>
            )}
            {selectedSession.status === "FINISHED" && (
              <p className="training-page__notice" role="status">
                Sessões finalizadas ficam somente para consulta.
              </p>
            )}

            <ol className="training-page__prescriptions">
              {selectedSession.exercises
                .slice()
                .sort((a, b) => a.orderIndex - b.orderIndex)
                .map((exercise, index, ordered) => (
                  <li key={exercise.id} className="training-page__prescription">
                    <span className="training-page__prescription-order">{index + 1}</span>
                    <div className="training-page__prescription-main">
                      <strong>{exercise.exerciseName}</strong>
                      <span>{exercise.sets} séries × {exercise.repetitions} reps · {exercise.load} kg · {exercise.intensityType}{exercise.intensityValue !== null ? ` ${exercise.intensityValue}` : ""} · descanso {exercise.restSeconds}s</span>
                    </div>
                    <div className="training-page__reorder" aria-label={`Reordenar ${exercise.exerciseName}`}>
                      <Button
                        type="button"
                        variant="ghost"
                        disabled={index === 0 || reorderExercises.isPending || selectedSession.status === "FINISHED"}
                        aria-label={`Mover ${exercise.exerciseName} para cima`}
                        onClick={() => reorderExercises.mutate(movePrescription(ordered.map((item) => item.id), index, -1))}
                      >↑</Button>
                      <Button
                        type="button"
                        variant="ghost"
                        disabled={index === ordered.length - 1 || reorderExercises.isPending || selectedSession.status === "FINISHED"}
                        aria-label={`Mover ${exercise.exerciseName} para baixo`}
                        onClick={() => reorderExercises.mutate(movePrescription(ordered.map((item) => item.id), index, 1))}
                      >↓</Button>
                    </div>
                  </li>
                ))}
            </ol>
            {reorderExercises.isError && <p className="training-page__alert" role="alert">{errorMessage(reorderExercises.error, "Não foi possível reordenar os exercícios.")}</p>}

            {selectedSession.status !== "FINISHED" && (
            <div className="training-page__add-prescription">
              <div>
                <p className="training-page__eyebrow">Adicionar exercício</p>
                <h3>Prescrição da biblioteca</h3>
              </div>
              <form
                className="training-page__search"
                onSubmit={(event) => {
                  event.preventDefault();
                  setSubmittedName(libraryName.trim());
                  setSubmittedMuscle(libraryMuscle);
                  setExerciseId("");
                }}
              >
                <Field label="Buscar exercício" value={libraryName} onChange={(event) => setLibraryName(event.target.value)} placeholder="Nome do exercício" />
                <SelectField
                  label="Grupo muscular"
                  value={libraryMuscle}
                  onChange={(event) => setLibraryMuscle(event.target.value as MuscleGroup | "")}
                  options={MUSCLE_GROUPS.map((group) => ({ value: group, label: MUSCLE_LABELS[group] }))}
                  placeholder="Todos os grupos"
                />
                <Button type="submit">Buscar</Button>
              </form>

              {exercisesQuery.isError && <p className="training-page__alert" role="alert">Não foi possível carregar a biblioteca.</p>}
              {exercisesQuery.isLoading ? (
                <p className="training-page__muted">Carregando exercícios…</p>
              ) : exercisesQuery.data?.length ? (
                <div className="training-page__exercise-list">
                  {exercisesQuery.data.map((exercise) => (
                    <ExerciseOption
                      key={exercise.id}
                      exercise={exercise}
                      selected={exercise.id === exerciseId}
                      onSelect={() => setExerciseId(exercise.id)}
                    />
                  ))}
                </div>
              ) : (
                <p className="training-page__muted">Nenhum exercício encontrado com esses filtros.</p>
              )}

              {selectedExercise && (
                <form className="training-page__form" onSubmit={onAddExercise}>
                  <div className="training-page__selected-exercise">
                    <div>
                      <span className="training-page__mono">{EQUIPMENT_LABELS[selectedExercise.equipment] ?? selectedExercise.equipment} · v{selectedExercise.version}</span>
                      <h4>{selectedExercise.name}</h4>
                    </div>
                    <p>{selectedExercise.execution}</p>
                  </div>
                  <div className="training-page__form-grid training-page__form-grid--prescription">
                    <Field name="sets" label="Séries" type="number" min="1" step="1" required />
                    <Field name="repetitions" label="Repetições" type="number" min="1" step="1" required />
                    <Field name="load" label="Carga (kg)" type="number" min="0.1" step="0.1" required />
                    <SelectField name="intensityType" label="Intensidade" options={INTENSITY_OPTIONS} placeholder="Selecione" required />
                    <Field name="intensityValue" label="Valor da intensidade" type="number" min="0.1" step="0.1" />
                    <Field name="restSeconds" label="Descanso (segundos)" type="number" min="1" step="1" required />
                  </div>
                  {addExercise.isError && <p className="training-page__alert" role="alert">{errorMessage(addExercise.error, "Não foi possível adicionar o exercício.")}</p>}
                  <Button type="submit" loading={addExercise.isPending}>Adicionar à sessão</Button>
                </form>
              )}
            </div>
            )}
          </section>
        )}
      </div>
    </AppShell>
  );
}

function ExerciseOption({
  exercise,
  selected,
  onSelect,
}: {
  exercise: Exercise;
  selected: boolean;
  onSelect: () => void;
}) {
  return (
    <article className={selected ? "training-page__exercise training-page__exercise--selected" : "training-page__exercise"}>
      <button type="button" className="training-page__exercise-select" onClick={onSelect} aria-pressed={selected}>
        <span>
          <strong>{exercise.name}</strong>
          <small>{exercise.muscleGroups.map((group) => MUSCLE_LABELS[group]).join(", ")} · {EQUIPMENT_LABELS[exercise.equipment] ?? exercise.equipment}</small>
        </span>
        <span className="training-page__mono">v{exercise.version}</span>
      </button>
      <details className="training-page__exercise-details">
        <summary>Detalhes técnicos e referências</summary>
        <p>{exercise.execution}</p>
        {!!exercise.commonErrors.length && <p><strong>Erros comuns:</strong> {exercise.commonErrors.join("; ")}</p>}
        {!!exercise.contraindications.length && <p><strong>Contraindicações:</strong> {exercise.contraindications.join("; ")}</p>}
        {!!exercise.progressions.length && <p><strong>Progressões:</strong> {exercise.progressions.join("; ")}</p>}
        {!!exercise.regressions.length && <p><strong>Regressões:</strong> {exercise.regressions.join("; ")}</p>}
        {!!exercise.references.length && (
          <ul>
            {exercise.references.map((reference, index) => (
              <li key={`${reference.title}-${index}`}>
                {reference.authors}. <cite>{reference.title}</cite>. {reference.source}{reference.year ? `, ${reference.year}` : ""}.
              </li>
            ))}
          </ul>
        )}
      </details>
    </article>
  );
}
