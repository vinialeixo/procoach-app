import { Link } from "react-router-dom";
import { AthleteShell } from "@/layouts/AthleteShell";
import { getAthleteId } from "@/lib/auth";
import { Meal, OBJECTIVE_LABEL, useCurrentAthleteDiet } from "@/lib/diets";
import { useAthleteNotifications } from "@/lib/notifications";
import {
  currentPlanVersion,
  useAthleteTrainingSessions,
  useTrainingPlans,
} from "@/lib/training";
import "./HomeCards.css";

// Área do atleta (persona ATHLETE, RF-11 / US-038). Nav reduzida e somente
// leitura: quem monta/aprova dieta e treino é sempre o coach; o atleta só
// visualiza os próprios dados.
const SECTIONS = [
  {
    to: "/athlete/dieta",
    title: "Dieta",
    text: "Veja a dieta aprovada pelo seu coach e o histórico de versões.",
  },
  {
    to: "/athlete/treino",
    title: "Treino",
    text: "Consulte o plano e as sessões de treino atuais.",
  },
  {
    to: "/athlete/avaliacoes",
    title: "Avaliações",
    text: "Acompanhe a evolução das suas avaliações físicas.",
  },
  {
    to: "/athlete/avisos",
    title: "Preferências de aviso",
    text: "Escolha como quer receber os avisos do seu coach.",
  },
];

const DAY_SHORT: Record<number, string> = {
  1: "Segunda",
  2: "Terça",
  3: "Quarta",
  4: "Quinta",
  5: "Sexta",
  6: "Sábado",
  7: "Domingo",
};

function pickNextMeal(meals: Meal[]): Meal | undefined {
  if (!meals.length) return undefined;
  const now = new Date();
  const currentHHMM = `${String(now.getHours()).padStart(2, "0")}:${String(
    now.getMinutes()
  ).padStart(2, "0")}`;
  const sorted = [...meals].sort((a, b) => a.order - b.order);
  return sorted.find((m) => m.time && m.time >= currentHHMM) ?? sorted[0];
}

export function AthleteHomePage() {
  const athleteId = getAthleteId() ?? "";
  const plansQuery = useTrainingPlans(athleteId);
  const activePlan = currentPlanVersion(plansQuery.data ?? []);
  const sessionsQuery = useAthleteTrainingSessions(
    athleteId,
    activePlan?.id ?? ""
  );
  const dietQuery = useCurrentAthleteDiet(athleteId);
  const notificationsQuery = useAthleteNotifications(athleteId);

  const jsDay = new Date().getDay();
  const todayDayOfWeek = jsDay === 0 ? 7 : jsDay;
  const sessions = sessionsQuery.data ?? [];
  const todaySessions = sessions.filter((s) => s.dayOfWeek === todayDayOfWeek);
  const primarySession = todaySessions[0] ?? sessions[0];

  const diet = dietQuery.data ?? null;
  const dietMacros = diet ? (diet.totals ?? diet.macros) : null;
  const nextMeal = diet ? pickNextMeal(diet.meals) : undefined;
  const unreadCount = (notificationsQuery.data ?? []).filter(
    (n) => !n.readAt
  ).length;

  return (
    <AthleteShell>
      <header className="coach-home__header">
        <h1>Minha área</h1>
        <p>Resumo de hoje e acesso rápido ao seu protocolo.</p>
      </header>

      {athleteId && (
        <section className="athlete-home__today" aria-label="Resumo de hoje">
          <Link to="/athlete/treino" className="athlete-home__hero">
            <span className="athlete-home__eyebrow">
              {todaySessions.length > 0
                ? `Treino de hoje · ${DAY_SHORT[todayDayOfWeek]}`
                : "Plano de treino"}
            </span>
            <strong className="athlete-home__hero-title">
              {plansQuery.isPending
                ? "Carregando treino…"
                : primarySession
                  ? primarySession.name
                  : "Nenhum treino ativo"}
            </strong>
            <span className="athlete-home__hero-meta">
              {primarySession
                ? `${primarySession.exercises.length} exercício(s) · v${activePlan?.version ?? 1}`
                : "Aguardando publicação do coach"}
            </span>
          </Link>

          <Link to="/athlete/dieta" className="athlete-home__hero">
            <span className="athlete-home__eyebrow">
              {nextMeal
                ? `Próxima refeição${nextMeal.time ? ` · ${nextMeal.time}` : ""}`
                : "Dieta aprovada"}
            </span>
            <strong className="athlete-home__hero-title">
              {dietQuery.isPending
                ? "Carregando dieta…"
                : nextMeal
                  ? nextMeal.name
                  : diet
                    ? `${OBJECTIVE_LABEL[diet.objective] ?? diet.objective} · v${diet.version}`
                    : "Sem dieta aprovada"}
            </strong>
            <span className="athlete-home__hero-meta">
              {dietMacros
                ? `${Math.round(dietMacros.calories)} kcal · P ${Math.round(dietMacros.protein)}g · C ${Math.round(dietMacros.carbohydrates)}g · G ${Math.round(dietMacros.fat)}g`
                : "Aguardando aprovação do coach"}
            </span>
          </Link>
        </section>
      )}

      <ul className="home-cards">
        {SECTIONS.map((section) => (
          <li key={section.to}>
            <Link to={section.to} className="home-cards__card">
              <span className="home-cards__title">
                {section.title}
                {section.to === "/athlete/avisos" && unreadCount > 0 && (
                  <span className="athlete-home__badge"> {unreadCount}</span>
                )}
              </span>
              <span className="home-cards__text">{section.text}</span>
            </Link>
          </li>
        ))}
      </ul>
    </AthleteShell>
  );
}
