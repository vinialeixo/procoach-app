import { Link } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
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
  return (
    <AppShell navItems={COACH_NAV}>
      <h1>Painel do coach</h1>
      <p>Escolha por onde começar.</p>
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
