import { AppShell } from "@/layouts/AppShell";

const COACH_NAV = [
  { label: "Atletas", href: "/coach/athletes" },
  { label: "Treinos e biblioteca", href: "/coach/training" },
];

export function CoachHomePage() {
  return (
    <AppShell navItems={COACH_NAV}>
      <h1>Painel do coach</h1>
      <p>
        Login concluído. As telas de gestão de atletas, dietas e treinos
        entram aqui conforme as próximas tasks do backlog (T-019, T-022,
        T-027, T-034...).
      </p>
    </AppShell>
  );
}
