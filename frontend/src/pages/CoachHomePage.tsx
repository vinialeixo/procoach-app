import { Link } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { StateMessage } from "@/components/ui/StateMessage";
import { useAthletes } from "@/lib/athletes";
import { COACH_NAV } from "@/lib/coach-nav";
import "./HomeCards.css";

const SHORTCUTS = [
  {
    to: "/coach/athletes",
    title: "Atletas",
    text: "Veja a carteira, abra fichas, avaliações e dietas.",
  },
  {
    to: "/coach/athletes/new",
    title: "Novo atleta",
    text: "Cadastre um atleta e convide-o para o app.",
  },
  {
    to: "/coach/training",
    title: "Treinos e biblioteca",
    text: "Monte planos, sessões e prescrições.",
  },
];

export function CoachHomePage() {
  const { data: athletes, isLoading, isError, refetch } = useAthletes();

  const allAthletes = athletes ?? [];
  const activeAthletes = allAthletes.filter((a) => a.status === "ACTIVE");
  const pausedOrFinished = allAthletes.filter((a) => a.status !== "ACTIVE");
  const recentAthletes = activeAthletes.slice(0, 5);

  return (
    <AppShell navItems={COACH_NAV}>
      <header className="coach-home__header">
        <h1>Painel do coach</h1>
        <p>Pulso da carteira e atalhos principais de trabalho.</p>
      </header>

      <section className="coach-home__pulse" aria-label="Pulso clínico">
        <div className="coach-home__kpi">
          <span className="coach-home__kpi-label">Atletas ativos</span>
          <strong className="coach-home__kpi-value">
            {isLoading ? "—" : activeAthletes.length}
          </strong>
          <span className="coach-home__kpi-meta">Em acompanhamento corrente</span>
        </div>
        <div className="coach-home__kpi">
          <span className="coach-home__kpi-label">Pausados / finalizados</span>
          <strong className="coach-home__kpi-value">
            {isLoading ? "—" : pausedOrFinished.length}
          </strong>
          <span className="coach-home__kpi-meta">Histórico preservado</span>
        </div>
        <div className="coach-home__kpi">
          <span className="coach-home__kpi-label">Total na carteira</span>
          <strong className="coach-home__kpi-value">
            {isLoading ? "—" : allAthletes.length}
          </strong>
          <span className="coach-home__kpi-meta">Carteira sob sua gestão</span>
        </div>
      </section>

      {isError ? (
        <StateMessage
          kind="error"
          title="Não foi possível carregar o resumo da carteira"
          description="Verifique a conexão e tente atualizar."
          onRetry={() => refetch()}
        />
      ) : (
        recentAthletes.length > 0 && (
          <section className="coach-home__recent" aria-label="Acesso rápido a atletas">
            <div className="coach-home__recent-head">
              <h2>Acesso rápido — atletas ativos</h2>
              <Link to="/coach/athletes" className="coach-home__see-all">
                Ver todos
              </Link>
            </div>
            <ul className="coach-home__athlete-list">
              {recentAthletes.map((athlete) => (
                <li key={athlete.id}>
                  <Link
                    to={`/coach/athletes/${athlete.id}`}
                    className="coach-home__athlete-row"
                  >
                    <span className="coach-home__athlete-name">{athlete.name}</span>
                    <span className="coach-home__athlete-goal">
                      {athlete.goal || "Sem objetivo registrado"}
                    </span>
                    <StatusBadge status={athlete.status} />
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        )
      )}

      <ul className="home-cards">
        {SHORTCUTS.map((item) => (
          <li key={item.to}>
            <Link to={item.to} className="home-cards__card">
              <span className="home-cards__title">{item.title}</span>
              <span className="home-cards__text">{item.text}</span>
            </Link>
          </li>
        ))}
      </ul>
    </AppShell>
  );
}
