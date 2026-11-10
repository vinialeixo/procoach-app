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
  const plansQuery = useTrainingPlans(athleteId ?? "");
  const plan = currentPlanVersion(plansQuery.data ?? []);
  const history = previousPlanVersions(plansQuery.data ?? [], plan);
  const sessionsQuery = useAthleteTrainingSessions(athleteId ?? "", plan?.id ?? "");
  const error = plansQuery.error ?? sessionsQuery.error;
  const status = error && typeof error === "object" && "response" in error
    ? (error as { response?: { status?: number } }).response?.status : undefined;

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
                    <div className="athlete-content__sessions">
                      {[...sessionsQuery.data].sort(sessionOrder).map((session) => (
                        <article className="athlete-content__card" key={session.id}>
                          <p className="athlete-content__eyebrow">
                            Semana {session.weekNumber} · dia {session.dayOfWeek}
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
