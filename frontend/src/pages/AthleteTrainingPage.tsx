import { getAthleteId } from "@/lib/auth";
import { ATHLETE_NAV } from "@/lib/athlete-nav";
import { AppShell } from "@/layouts/AppShell";
import { TrainingSession, useAthleteTrainingSessions, useTrainingPlans } from "@/lib/training";
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
  const plan = plansQuery.data?.filter((item) => item.status === "ACTIVE" && !item.deleted)
    .sort((a, b) => b.version - a.version)[0];
  const sessionsQuery = useAthleteTrainingSessions(athleteId ?? "", plan?.id ?? "");
  const error = plansQuery.error ?? sessionsQuery.error;
  const status = error && typeof error === "object" && "response" in error
    ? (error as { response?: { status?: number } }).response?.status : undefined;

  return (
    <AppShell navItems={ATHLETE_NAV}>
      <section className="athlete-content">
        <h1>Meu treino</h1>
        {!athleteId ? <p role="alert">Não foi possível identificar sua conta. Entre novamente.</p>
          : plansQuery.isPending || (plan && sessionsQuery.isPending) ? <p role="status">Carregando seu treino…</p>
          : error ? <RequestError status={status} />
          : !plan || !sessionsQuery.data?.length ? <p>Ainda não há um treino ativo disponível.</p>
          : <>
              <article className="athlete-content__card">
                <p className="athlete-content__eyebrow">Plano ativo · versão {plan.version}</p>
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
      </section>
    </AppShell>
  );
}
