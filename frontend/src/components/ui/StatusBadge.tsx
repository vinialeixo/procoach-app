import { getAthleteAccessStatus, type Athlete } from "@/lib/athletes";
import type { CoachAccessStatus } from "@/lib/admin";
import { formatInviteExpiration } from "@/lib/invites";
import "./StatusBadge.css";

type Tone = "success" | "muted" | "neutral" | "dark";

// Rótulos/cores por estado. Atleta: Ativo/Pausado/Finalizado (US-005).
// Dieta (T-034): Rascunho/Em revisão/Aprovada/Arquivada (US-017) — o fluxo
// é travado em DRAFT -> IN_REVIEW -> APPROVED -> ARCHIVED (ver lib/diets.ts);
// o badge apenas reflete o estado, quem decide a transição é a tela.
// Acesso do atleta (T-082): Tem acesso / Convite pendente / Sem acesso.
// Coach / Admin (T-108): Ativo / Suspenso / Arquivado; Acesso ativo / Desabilitado.
const VARIANTS: Record<string, { label: string; tone: Tone }> = {
  ACTIVE: { label: "Ativo", tone: "success" },
  PAUSED: { label: "Pausado", tone: "muted" },
  FINISHED: { label: "Finalizado", tone: "neutral" },
  SUSPENDED: { label: "Suspenso", tone: "muted" },
  CANCELLED: { label: "Arquivado", tone: "neutral" },
  DRAFT: { label: "Rascunho", tone: "neutral" },
  IN_REVIEW: { label: "Em revisão", tone: "muted" },
  APPROVED: { label: "Aprovada", tone: "success" },
  ARCHIVED: { label: "Arquivada", tone: "dark" },
  HAS_ACCESS: { label: "Tem acesso", tone: "success" },
  ACCESS_ACTIVE: { label: "Acesso ativo", tone: "success" },
  INVITE_PENDING: { label: "Convite pendente", tone: "muted" },
  NO_ACCESS: { label: "Sem acesso", tone: "neutral" },
  DISABLED: { label: "Desabilitado", tone: "neutral" },
};

export function StatusBadge({ status, title }: { status: string; title?: string }) {
  const variant = VARIANTS[status] ?? { label: status, tone: "neutral" as const };
  return (
    <span className={`pc-badge pc-badge--${variant.tone}`} title={title}>
      {variant.label}
    </span>
  );
}

export function AthleteAccessBadge({
  athlete,
}: {
  athlete: Pick<Athlete, "hasCredential" | "pendingInvite">;
}) {
  const accessStatus = getAthleteAccessStatus(athlete);
  const title =
    accessStatus === "INVITE_PENDING" && athlete.pendingInvite
      ? `Convite para ${athlete.pendingInvite.email} — expira em ${formatInviteExpiration(athlete.pendingInvite.expiresAt)}`
      : undefined;
  return <StatusBadge status={accessStatus} title={title} />;
}

export function CoachAccessBadge({
  accessStatus,
  inviteExpiresAt,
}: {
  accessStatus: CoachAccessStatus;
  inviteExpiresAt?: string | null;
}) {
  const key = accessStatus === "ACTIVE" ? "ACCESS_ACTIVE" : accessStatus;
  const title =
    accessStatus === "INVITE_PENDING" && inviteExpiresAt
      ? `Convite pendente — expira em ${formatInviteExpiration(inviteExpiresAt)}`
      : undefined;
  return <StatusBadge status={key} title={title} />;
}
