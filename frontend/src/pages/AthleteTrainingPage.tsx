import { useState } from "react";
import { getAthleteId } from "@/lib/auth";
import { AthleteShell } from "@/layouts/AthleteShell";
import {
  TrainingPlan,
  TrainingSession,
  currentPlanVersion,
  previousPlanVersions,
  useAthleteTrainingSessions,
  useTrainingPlans,
} from "@/lib/training";
import "./AthleteContent.css";

const DAY_LABELS: Record<number, string> = {
  1: "Segunda-feira",
  2: "Terça-feira",
  3: "Quarta-feira",
  4: "Quinta-feira",
  5: "Sexta-feira",
  6: "Sábado",
  7: "Domingo",
};

function RequestError({ status }: { status?: number }) {
  if (status === 401) return <p role="alert">Sua sessão expirou. Entre novamente para consultar seu treino.</p>;
  if (status === 403) return <p role="alert">Você não tem permissão para consultar este conteúdo.</p>;
  return <p role="alert">Não foi possível carregar o treino. Tente novamente mais tarde.</p>;
}

function sessionOrder(a: TrainingSession, b: TrainingSession) {
  return a.weekNumber - b.weekNumber || a.dayOfWeek - b.dayOfWeek || a.name.localeCompare(b.name);
}

export function AthleteTrainingPage() {
  const athleteId = getAthleteId();
  const [selectedDay, setSelectedDay] = useState<number | "all">("all");
  const plansQuery = useTrainingPlans(athleteId ?? "");
  const plan = currentPlanVersion(plansQuery.data ?? []);
  const history = previousPlanVersions(plansQuery.data ?? [], plan);
  const sessionsQuery = useAthleteTrainingSessions(athleteId ?? "", plan?.id ?? "");
  const error = plansQuery.error ?? sessionsQuery.error;
  const status = error && typeof error === "object" && "response" in error
    ? (error as { response?: { status?: number } }).response?.status : undefined;

  const sortedSessions = [...(sessionsQuery.data ?? [])].sort(sessionOrder);
  const availableDays = Array.from(new Set(sortedSessions.map((s) => s.dayOfWeek))).sort(
    (a, b) => a - b
  );
  const jsDay = new Date().getDay();
  const todayDayOfWeek = jsDay === 0 ? 7 : jsDay;
  const visibleSessions =
    selectedDay === "all"
      ? sortedSessions
      : sortedSessions.filter((s) => s.dayOfWeek === selectedDay);

  return (
    <AthleteShell>
      <section className="athlete-content">
        <h1>Meu treino</h1>
        {!athleteId ? <p role="alert">Não foi possível identificar sua conta. Entre novamente.</p>
          : plansQuery.isPending || (plan && sessionsQuery.isPending) ? <p role="status">Carregando seu treino…</p>
          : error ? <RequestError status={status} />
          : <>
              {!plan || !sessionsQuery.data?.length ? <p>Ainda não há um treino ativo disponível.</p>
                : <>
                    <article className="athlete-content__card">
                      <p className="athlete-content__eyebrow">Versão corrente · v{plan.version} · {plan.status === "ACTIVE" ? "ativa" : "finalizada"}</p>
                      <h2>{plan.name}</h2>
                      {plan.description && <p>{plan.description}</p>}
                    </article>
                    {availableDays.length > 1 && (
                      <div className="athlete-content__day-filter" role="group" aria-label="Filtrar por dia da semana">
                        <button
                          type="button"
                          className={`athlete-content__day-btn ${selectedDay === "all" ? "athlete-content__day-btn--active" : ""}`}
                          onClick={() => setSelectedDay("all")}
                        >
                          Todos ({sortedSessions.length})
                        </button>
                        {availableDays.map((day) => (
                          <button
                            key={day}
                            type="button"
                            className={`athlete-content__day-btn ${selectedDay === day ? "athlete-content__day-btn--active" : ""}`}
                            onClick={() => setSelectedDay(day)}
                          >
                            {DAY_LABELS[day] ?? `Dia ${day}`}
                            {day === todayDayOfWeek ? " (Hoje)" : ""}
                          </button>
                        ))}
                      </div>
                    )}
                    <div className="athlete-content__sessions">
                      {visibleSessions.map((session) => (
                        <article className="athlete-content__card" key={session.id}>
                          <p className="athlete-content__eyebrow">
                            Semana {session.weekNumber} · {DAY_LABELS[session.dayOfWeek] ?? `Dia ${session.dayOfWeek}`}
                            {session.dayOfWeek === todayDayOfWeek ? " · Hoje" : ""}
                            {session.status === "FINISHED" ? " · concluído" : " · ativo"}
                          </p>
                          <h2>{session.name}</h2>
                          {session.description && <p>{session.description}</p>}
                          {session.exercises.length ? <ol className="athlete-content__exercise-list">
                            {[...session.exercises].sort((a, b) => a.orderIndex - b.orderIndex).map((exercise) => (
                              <li key={exercise.id}>
                                <strong>{exercise.exerciseName}</strong>
                                <span>{exercise.sets} séries × {exercise.repetitions} repetições</span>
                                {exercise.load > 0 && <span>Carga: {exercise.load} kg</span>}
                                {exercise.restSeconds > 0 && <span>Descanso: {exercise.restSeconds} s</span>}
                                {exercise.notes && <span>{exercise.notes}</span>}
                              </li>
                            ))}
                          </ol> : <p>Sem exercícios prescritos nesta sessão.</p>}
                        </article>
                      ))}
                    </div>
                  </>}

              {history.length > 0 && (
                <section className="athlete-content__history" aria-labelledby="athlete-training-history">
                  <div>
                    <h2 id="athlete-training-history">Histórico de versões</h2>
                    <p className="athlete-content__eyebrow">
                      Versões anteriores ficam somente para consulta, como você as recebeu do coach.
                    </p>
                  </div>
                  {history.map((version) => (
                    <AthletePlanHistoryItem
                      key={version.id}
                      athleteId={athleteId}
                      plan={version}
                    />
                  ))}
                </section>
              )}
            </>}
      </section>
    </AthleteShell>
  );
}

/** T-074 — versão anterior do plano, somente leitura, com as sessões que o atleta recebeu. */
function AthletePlanHistoryItem({
  athleteId,
  plan,
}: {
  athleteId: string;
  plan: TrainingPlan;
}) {
  const sessionsQuery = useAthleteTrainingSessions(athleteId, plan.id);
  const sessions = [...(sessionsQuery.data ?? [])].sort(sessionOrder);

  return (
    <details className="athlete-content__history-item">
      <summary>
        <span>
          Versão {plan.version} · {plan.name}
        </span>
        <span className="athlete-content__eyebrow">
          {plan.status === "FINISHED" ? "Finalizada" : "Ativa"}
          {sessionsQuery.isPending
            ? " · carregando sessões…"
            : sessionsQuery.isError
              ? " · sessões indisponíveis"
              : ` · ${sessions.length} ${sessions.length === 1 ? "sessão" : "sessões"}`}
        </span>
      </summary>
      <div className="athlete-content__history-body">
        {sessionsQuery.isError ? (
          <p role="alert">Não foi possível carregar as sessões desta versão.</p>
        ) : sessionsQuery.isPending ? (
          <p role="status">Carregando sessões…</p>
        ) : sessions.length === 0 ? (
          <p>Nenhuma sessão liberada nesta versão.</p>
        ) : (
          sessions.map((session) => (
            <article className="athlete-content__history-session" key={session.id}>
              <p className="athlete-content__eyebrow">
                Semana {session.weekNumber} · dia {session.dayOfWeek}
              </p>
              <strong>{session.name}</strong>
              {session.description && <p>{session.description}</p>}
              {session.exercises.length > 0 && (
                <ul className="athlete-content__history-exercises">
                  {[...session.exercises]
                    .sort((a, b) => a.orderIndex - b.orderIndex)
                    .map((exercise) => (
                      <li key={exercise.id}>
                        {exercise.exerciseName} · {exercise.sets}×
                        {exercise.repetitions}
                        {exercise.load > 0 ? ` · ${exercise.load} kg` : ""}
                      </li>
                    ))}
                </ul>
              )}
            </article>
          ))
        )}
      </div>
    </details>
  );
}
