import { getAthleteAccessStatus, type Athlete } from "@/lib/athletes";
import "./StatusBadge.css";

type Tone = "success" | "muted" | "neutral" | "dark";

// Rótulos/cores por estado. Atleta: Ativo/Pausado/Finalizado (US-005).
// Dieta (T-034): Rascunho/Em revisão/Aprovada/Arquivada (US-017) — o fluxo
// é travado em DRAFT -> IN_REVIEW -> APPROVED -> ARCHIVED (ver lib/diets.ts);
// o badge apenas reflete o estado, quem decide a transição é a tela.
// Acesso do atleta (T-082): Tem acesso / Convite pendente / Sem acesso.
const VARIANTS: Record<string, { label: string; tone: Tone }> = {
  ACTIVE: { label: "Ativo", tone: "success" },
  PAUSED: { label: "Pausado", tone: "muted" },
  FINISHED: { label: "Finalizado", tone: "neutral" },
  DRAFT: { label: "Rascunho", tone: "neutral" },
  IN_REVIEW: { label: "Em revisão", tone: "muted" },
  APPROVED: { label: "Aprovada", tone: "success" },
  ARCHIVED: { label: "Arquivada", tone: "dark" },
  HAS_ACCESS: { label: "Tem acesso", tone: "success" },
  INVITE_PENDING: { label: "Convite pendente", tone: "muted" },
  NO_ACCESS: { label: "Sem acesso", tone: "neutral" },
};

export function StatusBadge({ status }: { status: string }) {
  const variant = VARIANTS[status] ?? { label: status, tone: "neutral" as const };
  return (
    <span className={`pc-badge pc-badge--${variant.tone}`}>{variant.label}</span>
  );
}

export function AthleteAccessBadge({
  athlete,
}: {
  athlete: Pick<Athlete, "hasCredential" | "pendingInvite">;
}) {
  const accessStatus = getAthleteAccessStatus(athlete);
  return <StatusBadge status={accessStatus} />;
}
