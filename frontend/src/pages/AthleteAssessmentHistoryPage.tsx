import { AthleteShell } from "@/layouts/AthleteShell";
import { StateMessage } from "@/components/ui/StateMessage";
import { getAthleteId } from "@/lib/auth";
import { AthleteAssessmentsPanel } from "@/pages/AthleteAssessmentsPanel";
import "./AthleteContent.css";

export function AthleteAssessmentHistoryPage() {
  const athleteId = getAthleteId();

  return (
    <AthleteShell>
      <section className="athlete-content">
        <h1>Histórico de avaliações</h1>
        {athleteId ? (
          <AthleteAssessmentsPanel athleteId={athleteId} allowCreate={false} />
        ) : (
          <StateMessage
            kind="error"
            description="Não foi possível identificar a conta de atleta nesta sessão. Entre novamente para consultar suas avaliações."
          />
        )}
      </section>
    </AthleteShell>
  );
}
