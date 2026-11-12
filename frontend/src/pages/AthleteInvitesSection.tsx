import { useState } from "react";
import { type Athlete } from "@/lib/athletes";
import { AthleteAccessBadge } from "@/components/ui/StatusBadge";
import { Button } from "@/components/ui/Button";
import {
  getCachedInvitePath,
  inviteUrl,
  useAthleteInvites,
  useRevokeAthleteInvite,
  type AthleteInvite,
} from "@/lib/invites";
import "./AthleteInvitesSection.css";

type Props = {
  athlete: Athlete;
  onOpenInviteModal: () => void;
};

export function AthleteInvitesSection({ athlete, onOpenInviteModal }: Props) {
  const { data: serverInvites, isLoading } = useAthleteInvites(athlete.id);
  const revokeMutation = useRevokeAthleteInvite(athlete.id);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [revokeError, setRevokeError] = useState<string | null>(null);

  // A listagem do backend (T-077) já inclui os convites pendentes com status
  // PENDING; sem fallback defensivo — o contrato é a fonte de verdade.
  const pendingInvites: AthleteInvite[] = (serverInvites ?? []).filter(
    (inv) => inv.status === "PENDING"
  );

  const cachedPath = getCachedInvitePath(athlete.id);

  async function handleCopy() {
    // O token nunca volta na listagem; o link só existe no momento da criação
    // (resposta do POST /auth/invites, guardada em sessionStorage).
    if (!cachedPath) return;
    try {
      await navigator.clipboard.writeText(inviteUrl({ invitePath: cachedPath }));
      setCopiedId(cachedPath);
      setTimeout(() => setCopiedId(null), 3000);
    } catch {
      setCopiedId(null);
    }
  }

  async function handleRevoke(inviteId: string) {
    if (!window.confirm("Deseja realmente revogar este convite?")) {
      return;
    }
    setRevokeError(null);
    try {
      await revokeMutation.mutateAsync(inviteId);
    } catch {
      setRevokeError("Não foi possível revogar o convite. Tente novamente.");
    }
  }

  function formatExpiration(iso: string) {
    try {
      const d = new Date(iso);
      if (isNaN(d.getTime())) return iso;
      return d.toLocaleDateString("pt-BR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return iso;
    }
  }

  return (
    <section className="athlete-invites" aria-labelledby="athlete-invites-title">
      <div className="athlete-invites__header">
        <div className="athlete-invites__title-wrap">
          <h2 id="athlete-invites-title" className="athlete-invites__title">
            Acesso ao aplicativo
          </h2>
          <AthleteAccessBadge athlete={athlete} />
        </div>
        {!athlete.hasCredential && (
          <Button type="button" variant="ghost" onClick={onOpenInviteModal}>
            {pendingInvites.length > 0 ? "Gerar novo convite" : "Convidar para o app"}
          </Button>
        )}
      </div>

      <div className="athlete-invites__content">
        {athlete.hasCredential ? (
          <p className="athlete-invites__success">
            Atleta possui credencial ativa e acesso liberado ao aplicativo.
          </p>
        ) : isLoading ? (
          <p>Carregando estado de convites…</p>
        ) : pendingInvites.length > 0 ? (
          <div className="athlete-invites__list">
            {pendingInvites.map((inv) => {
              const canCopy = Boolean(cachedPath);
              return (
                <div key={inv.id} className="athlete-invites__active-card">
                  <div className="athlete-invites__item-info">
                    <span className="athlete-invites__item-email">
                      Convite pendente: {inv.email}
                    </span>
                    <span className="athlete-invites__item-meta">
                      Expira em: {formatExpiration(inv.expiresAt)}
                    </span>
                  </div>
                  <div className="athlete-invites__item-actions">
                    {canCopy && (
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={handleCopy}
                      >
                        {copiedId ? "Link copiado!" : "Copiar link"}
                      </Button>
                    )}
                    <Button
                      type="button"
                      variant="ghost"
                      onClick={() => handleRevoke(inv.id)}
                      loading={revokeMutation.isPending}
                    >
                      Revogar convite
                    </Button>
                  </div>
                </div>
              );
            })}
            {revokeError && (
              <p className="athlete-invites__error" role="alert">
                {revokeError}
              </p>
            )}
          </div>
        ) : (
          <div className="athlete-invites__empty">
            <p>
              O atleta ainda não tem acesso nem convite pendente. Gere um convite para permitir
              que ele visualize treinos, dietas e avaliações.
            </p>
          </div>
        )}
      </div>
    </section>
  );
}
