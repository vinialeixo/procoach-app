import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { Button } from "@/components/ui/Button";
import { ButtonLink } from "@/components/ui/ButtonLink";
import { Modal } from "@/components/ui/Modal";
import { StateMessage } from "@/components/ui/StateMessage";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { ADMIN_NAV } from "@/lib/admin-nav";
import {
  coachInviteUrl,
  useAdminCoaches,
  useAdminSummary,
  useResendCoachInvite,
  type AdminCoach,
  type CoachInvitePayload,
} from "@/lib/admin";
import { AdminCoachCreateModal } from "./AdminCoachCreateModal";
import "./AdminPages.css";

type AttentionItem = {
  coach: AdminCoach;
  badgeStatus: string;
  label: string;
  kind: "INVITE" | "DETAIL";
};

function buildAdminAttention(coaches: AdminCoach[]): AttentionItem[] {
  const now = Date.now();
  const oneDayMs = 24 * 60 * 60 * 1000;
  const items: AttentionItem[] = [];

  for (const coach of coaches) {
    if (coach.accessStatus === "INVITE_PENDING") {
      const exp = coach.inviteExpiresAt ? Date.parse(coach.inviteExpiresAt) : NaN;
      const remaining = Number.isNaN(exp) ? Infinity : exp - now;
      if (remaining <= 0) {
        items.push({
          coach,
          badgeStatus: "INVITE_PENDING",
          label: "Convite expirado",
          kind: "INVITE",
        });
      } else if (remaining <= oneDayMs) {
        const hours = Math.max(1, Math.round(remaining / (60 * 60 * 1000)));
        items.push({
          coach,
          badgeStatus: "INVITE_PENDING",
          label: `Convite expira em ${hours}h`,
          kind: "INVITE",
        });
      } else {
        items.push({
          coach,
          badgeStatus: "INVITE_PENDING",
          label: "Convite pendente",
          kind: "INVITE",
        });
      }
    } else if (coach.status === "ACTIVE" && coach.athleteCount === 0) {
      items.push({
        coach,
        badgeStatus: "NO_ACCESS",
        label: "Ativo · 0 atletas na carteira",
        kind: "DETAIL",
      });
    } else if (coach.status === "SUSPENDED") {
      items.push({
        coach,
        badgeStatus: "SUSPENDED",
        label: "Coach suspenso",
        kind: "DETAIL",
      });
    }
  }

  return items.slice(0, 6);
}

export function AdminHomePage() {
  const { data: summary, isLoading: loadingSummary, isError: errorSummary, refetch } =
    useAdminSummary();
  const { data: coachesData } = useAdminCoaches({ includeArchived: false });
  const resendInvite = useResendCoachInvite();

  const [createOpen, setCreateOpen] = useState(false);
  const [resendCoach, setResendCoach] = useState<AdminCoach | null>(null);
  const [resentInvite, setResentInvite] = useState<CoachInvitePayload | null>(null);
  const [copied, setCopied] = useState(false);

  const attentionItems = useMemo(
    () => buildAdminAttention(coachesData?.items ?? []),
    [coachesData]
  );

  async function handleConfirmResend() {
    if (!resendCoach) return;
    const result = await resendInvite.mutateAsync(resendCoach.coachId);
    setResentInvite(result);
    setCopied(false);
  }

  function closeResendModal() {
    setResendCoach(null);
    setResentInvite(null);
    setCopied(false);
    resendInvite.reset();
  }

  async function copyResentLink() {
    if (!resentInvite) return;
    try {
      await navigator.clipboard.writeText(coachInviteUrl(resentInvite.token));
      setCopied(true);
    } catch {
      setCopied(false);
    }
  }

  return (
    <AppShell
      navItems={ADMIN_NAV}
      headerActions={
        <ButtonLink to="/admin/alterar-senha" variant="ghost">
          Alterar senha
        </ButtonLink>
      }
    >
      <header className="admin-page__header">
        <div>
          <h1>Painel administrativo</h1>
          <p>Visão operacional dos coaches, convites de acesso e volume de atletas na plataforma.</p>
        </div>
        <Button type="button" fullWidth={false} onClick={() => setCreateOpen(true)}>
          Novo coach
        </Button>
      </header>

      {errorSummary && (
        <div style={{ marginTop: "var(--space-5)" }}>
          <StateMessage
            kind="error"
            title="Não foi possível carregar os indicadores"
            description="Verifique sua conexão e tente novamente."
            onRetry={() => void refetch()}
          />
        </div>
      )}

      <section className="admin-kpi-grid" aria-label="Indicadores da plataforma">
        <Link className="admin-kpi-card" to="/admin/coaches?status=ACTIVE">
          <span className="admin-kpi-card__label">Coaches ativos</span>
          <strong className="admin-kpi-card__value">
            {loadingSummary ? "—" : summary?.activeCoaches ?? 0}
          </strong>
          <span className="admin-kpi-card__meta">
            {loadingSummary ? "Carregando…" : `${summary?.totalCoaches ?? 0} cadastrados no total`}
          </span>
        </Link>
        <Link className="admin-kpi-card" to="/admin/coaches?accessStatus=INVITE_PENDING">
          <span className="admin-kpi-card__label">Convites pendentes</span>
          <strong className="admin-kpi-card__value">
            {loadingSummary ? "—" : summary?.pendingInvites ?? 0}
          </strong>
          <span className="admin-kpi-card__meta">Aguardando ativação</span>
        </Link>
        <Link className="admin-kpi-card" to="/admin/coaches?status=SUSPENDED">
          <span className="admin-kpi-card__label">Coaches suspensos</span>
          <strong className="admin-kpi-card__value">
            {loadingSummary ? "—" : summary?.suspendedCoaches ?? 0}
          </strong>
          <span className="admin-kpi-card__meta">Acesso bloqueado</span>
        </Link>
        <Link className="admin-kpi-card" to="/admin/coaches">
          <span className="admin-kpi-card__label">Atletas ativos</span>
          <strong className="admin-kpi-card__value">
            {loadingSummary ? "—" : summary?.activeAthletes ?? 0}
          </strong>
          <span className="admin-kpi-card__meta">Distribuídos nas carteiras</span>
        </Link>
        <Link className="admin-kpi-card" to="/admin/admins">
          <span className="admin-kpi-card__label">Administradores</span>
          <strong className="admin-kpi-card__value">
            {loadingSummary ? "—" : summary?.adminCount ?? 0}
          </strong>
          <span className="admin-kpi-card__meta">Contas ativas</span>
        </Link>
      </section>

      <section className="admin-panel" aria-label="Atenção operacional">
        <div className="admin-panel__head">
          <h2>Atenção operacional ({attentionItems.length})</h2>
          <Link className="admin-panel__link" to="/admin/coaches">
            Ver todos os coaches
          </Link>
        </div>

        {attentionItems.length === 0 ? (
          <StateMessage
            kind="info"
            title="Nenhuma pendência no momento"
            description="Todos os coaches ativos já aceitaram o acesso e não há convites pendentes."
          />
        ) : (
          <ul className="admin-attention-list">
            {attentionItems.map(({ coach, label, kind }) => (
              <li key={coach.coachId} className="admin-attention-row">
                <div className="admin-attention-row__main">
                  <div className="admin-attention-row__name">{coach.name}</div>
                  <div className="admin-attention-row__sub">{coach.email}</div>
                </div>
                <div>
                  <StatusBadge status={kind === "INVITE" ? "INVITE_PENDING" : coach.status} />{" "}
                  <span style={{ fontSize: "var(--text-xs)", color: "var(--color-ink-muted)" }}>
                    {label}
                  </span>
                </div>
                {kind === "INVITE" ? (
                  <Button
                    type="button"
                    variant="ghost"
                    fullWidth={false}
                    onClick={() => setResendCoach(coach)}
                  >
                    Reenviar convite
                  </Button>
                ) : (
                  <ButtonLink to={`/admin/coaches/${coach.coachId}`} variant="ghost">
                    Abrir ficha
                  </ButtonLink>
                )}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="admin-shortcut-grid" aria-label="Atalhos">
        <Link className="admin-shortcut-card" to="/admin/coaches">
          <h2>Gerenciar coaches</h2>
          <p>Busque por nome ou e-mail, acompanhe carteiras de atletas e emita novos convites.</p>
        </Link>
        <Link className="admin-shortcut-card" to="/admin/admins">
          <h2>Contas administrativas</h2>
          <p>Liste administradores da plataforma, adicione novas contas e redefina senhas.</p>
        </Link>
      </section>

      <AdminCoachCreateModal open={createOpen} onClose={() => setCreateOpen(false)} />

      <Modal
        open={Boolean(resendCoach)}
        title={resentInvite ? "Novo link de convite gerado" : "Reenviar convite de coach"}
        onClose={closeResendModal}
      >
        {resentInvite && resendCoach ? (
          <div className="pc-modal__body">
            <StateMessage
              kind="warning"
              title="Copie este link agora"
              description="O convite anterior foi revogado. Este link é exibido uma única vez."
            />
            <div className="admin-invite-box">
              <input
                type="text"
                readOnly
                aria-label="Novo link de convite"
                value={coachInviteUrl(resentInvite.token)}
              />
              <Button
                type="button"
                fullWidth={false}
                className="admin-copy-btn"
                onClick={() => void copyResentLink()}
              >
                {copied ? "Link copiado" : "Copiar link"}
              </Button>
            </div>
            <div className="pc-modal__actions">
              <Button type="button" variant="ghost" fullWidth={false} onClick={closeResendModal}>
                Fechar
              </Button>
            </div>
          </div>
        ) : (
          <div className="pc-modal__body">
            <p style={{ margin: 0, fontSize: "var(--text-sm)" }}>
              Reenviar o convite para <strong>{resendCoach?.name}</strong> revogará o link anterior e
              gerará um novo token válido por 7 dias.
            </p>
            <div className="pc-modal__actions">
              <Button type="button" variant="ghost" fullWidth={false} onClick={closeResendModal}>
                Cancelar
              </Button>
              <Button
                type="button"
                fullWidth={false}
                loading={resendInvite.isPending}
                onClick={() => void handleConfirmResend()}
              >
                Confirmar reenvio
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </AppShell>
  );
}
