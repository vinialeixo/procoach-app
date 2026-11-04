import { AthleteShell } from "@/layouts/AthleteShell";
import { getAthleteId } from "@/lib/auth";
import { AthleteAssessmentsPanel } from "@/pages/AthleteAssessmentsPanel";

export function AthleteAssessmentHistoryPage() {
  const athleteId = getAthleteId();

  return (
    <AthleteShell>
      <h1>Histórico de avaliações</h1>
      {athleteId ? (
        <AthleteAssessmentsPanel athleteId={athleteId} allowCreate={false} />
      ) : (
        <p role="alert">
          Não foi possível identificar a conta de atleta nesta sessão. Entre
          novamente para consultar suas avaliações.
        </p>
      )}
    </AthleteShell>
  );
}
