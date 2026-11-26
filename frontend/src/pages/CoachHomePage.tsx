import { Link } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { StateMessage } from "@/components/ui/StateMessage";
import {
  Athlete,
  GOAL_LABEL,
  getAthleteAccessStatus,
  useAthletes,
} from "@/lib/athletes";
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

type AttentionItem = {
  athlete: Athlete;
  kind: "INVITE_EXPIRING" | "INVITE_PENDING" | "NO_ACCESS";
  label: string;
  cta: string;
};

function buildAttentionInbox(activeAthletes: Athlete[]): {
  items: AttentionItem[];
  totalCount: number;
} {
  const now = Date.now();
  const fortyEightHoursMs = 48 * 60 * 60 * 1000;
  const items: AttentionItem[] = [];

  for (const athlete of activeAthletes) {
    const access = getAthleteAccessStatus(athlete);
    if (access === "INVITE_PENDING" && athlete.pendingInvite) {
      const expiresAtMs = Date.parse(athlete.pendingInvite.expiresAt);
      const remainingMs = Number.isNaN(expiresAtMs) ? Infinity : expiresAtMs - now;
      if (remainingMs <= 0) {
        items.push({
          athlete,
          kind: "INVITE_EXPIRING",
          label: "Convite expirado",
          cta: "Reenviar convite",
        });
      } else if (remainingMs <= fortyEightHoursMs) {
        const hours = Math.max(1, Math.round(remainingMs / (60 * 60 * 1000)));
        items.push({
          athlete,
          kind: "INVITE_EXPIRING",
          label: `Convite expira em ${hours}h`,
          cta: "Gerenciar convite",
        });
      } else {
        items.push({
          athlete,
          kind: "INVITE_PENDING",
          label: "Convite aguardando aceite",
          cta: "Ver convite",
        });
      }
    } else if (access === "NO_ACCESS") {
      items.push({
        athlete,
        kind: "NO_ACCESS",
        label: "Sem acesso ao app",
        cta: "Convidar",
      });
    }
  }

  return { items: items.slice(0, 6), totalCount: items.length };
}

export function CoachHomePage() {
  const { data: athletes, isLoading, isError, refetch } = useAthletes();

  const allAthletes = athletes ?? [];
  const activeAthletes = allAthletes.filter((a) => a.status === "ACTIVE");
  const pausedOrFinished = allAthletes.filter((a) => a.status !== "ACTIVE");
  const recentAthletes = activeAthletes.slice(0, 5);
  const { items: attentionItems, totalCount: totalAttentionCount } =
    buildAttentionInbox(activeAthletes);

  return (
    <AppShell navItems={COACH_NAV}>
      <header className="coach-home__header">
        <h1>Painel do coach</h1>
        <p>Pulso da carteira, pendências de acesso e atalhos de trabalho.</p>
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
          <span className="coach-home__kpi-label">Atenção pendente</span>
          <strong className="coach-home__kpi-value">
            {isLoading ? "—" : totalAttentionCount}
          </strong>
          <span className="coach-home__kpi-meta">Convites e acessos em aberto</span>
        </div>
        <div className="coach-home__kpi">
          <span className="coach-home__kpi-label">Pausados / finalizados</span>
          <strong className="coach-home__kpi-value">
            {isLoading ? "—" : pausedOrFinished.length}
          </strong>
          <span className="coach-home__kpi-meta">Histórico preservado</span>
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
        <>
          {attentionItems.length > 0 && (
            <section className="coach-home__recent" aria-label="Inbox de atenção">
              <div className="coach-home__recent-head">
                <h2>
                  Inbox de atenção
                  {totalAttentionCount > attentionItems.length && (
                    <span className="coach-home__kpi-meta">
                      {" "}
                      (mostrando {attentionItems.length} de {totalAttentionCount})
                    </span>
                  )}
                </h2>
                <Link to="/coach/athletes" className="coach-home__see-all">
                  Gerenciar carteira
                </Link>
              </div>
              <ul className="coach-home__athlete-list">
                {attentionItems.map(({ athlete, kind, label, cta }) => (
                  <li key={athlete.id}>
                    <Link
                      to={`/coach/athletes/${athlete.id}`}
                      className="coach-home__athlete-row"
                    >
                      <span className="coach-home__athlete-name">
                        {athlete.name}
                      </span>
                      <span
                        className={`coach-home__inbox-tag coach-home__inbox-tag--${
                          kind === "INVITE_EXPIRING" ? "warn" : "neutral"
                        }`}
                      >
                        {label}
                      </span>
                      <span className="coach-home__inbox-cta">{cta}</span>
                    </Link>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {recentAthletes.length > 0 && (
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
                        {GOAL_LABEL[athlete.goal] ?? athlete.goal ?? "Sem objetivo registrado"}
                      </span>
                      <StatusBadge status={athlete.status} />
                    </Link>
                  </li>
                ))}
              </ul>
            </section>
          )}
        </>
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
