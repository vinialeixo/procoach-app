import { AppShell } from "@/layouts/AppShell";
import { ATHLETE_NAV } from "@/lib/athlete-nav";
import { getAthleteId } from "@/lib/auth";
import { AthleteAssessmentsPanel } from "@/pages/AthleteAssessmentsPanel";

export function AthleteAssessmentHistoryPage() {
  const athleteId = getAthleteId();

  return (
    <AppShell navItems={ATHLETE_NAV}>
      <h1>Histórico de avaliações</h1>
      {athleteId ? (
        <AthleteAssessmentsPanel athleteId={athleteId} allowCreate={false} />
      ) : (
        <p role="alert">
          Não foi possível identificar a conta de atleta nesta sessão. Entre
          novamente para consultar suas avaliações.
        </p>
      )}
    </AppShell>
  );
}
