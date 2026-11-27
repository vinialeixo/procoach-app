import { useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { Button } from "@/components/ui/Button";
import { ButtonLink } from "@/components/ui/ButtonLink";
import { DataTable, type Column } from "@/components/ui/DataTable";
import { Modal } from "@/components/ui/Modal";
import { StateMessage } from "@/components/ui/StateMessage";
import { CoachAccessBadge, StatusBadge } from "@/components/ui/StatusBadge";
import { ADMIN_NAV } from "@/lib/admin-nav";
import {
  coachInviteUrl,
  useAdminCoaches,
  useResendCoachInvite,
  type AdminCoach,
  type CoachAccessStatus,
  type CoachInvitePayload,
  type CoachStatus,
} from "@/lib/admin";
import { AdminCoachCreateModal } from "./AdminCoachCreateModal";
import "./AdminPages.css";

export function AdminCoachListPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [search, setSearch] = useState(searchParams.get("search") ?? "");
  const [statusFilter, setStatusFilter] = useState<"" | CoachStatus>(
    (searchParams.get("status") as CoachStatus) || ""
  );
  const [accessFilter, setAccessFilter] = useState<"" | CoachAccessStatus>(
    (searchParams.get("accessStatus") as CoachAccessStatus) || ""
  );
  const [includeArchived, setIncludeArchived] = useState(
    searchParams.get("includeArchived") === "true" || statusFilter === "CANCELLED"
  );

  const [createOpen, setCreateOpen] = useState(false);
  const [resendCoach, setResendCoach] = useState<AdminCoach | null>(null);
  const [resentInvite, setResentInvite] = useState<CoachInvitePayload | null>(null);
  const [copied, setCopied] = useState(false);

  const { data, isLoading, isError, refetch } = useAdminCoaches({
    status: statusFilter || undefined,
    accessStatus: accessFilter || undefined,
    search: search.trim() || undefined,
    includeArchived,
  });
  const resendInvite = useResendCoachInvite();

  const coaches = useMemo(() => {
    const items = data?.items ?? [];
    const term = search.trim().toLowerCase();
    return items.filter((coach) => {
      if (!includeArchived && coach.status === "CANCELLED") return false;
      if (statusFilter && coach.status !== statusFilter) return false;
      if (accessFilter && coach.accessStatus !== accessFilter) return false;
      if (
        term &&
        !coach.name.toLowerCase().includes(term) &&
        !coach.email.toLowerCase().includes(term)
      ) {
        return false;
      }
      return true;
    });
  }, [data, search, statusFilter, accessFilter, includeArchived]);

  const hasFilters =
    search.trim() !== "" || statusFilter !== "" || accessFilter !== "" || includeArchived;

  function clearFilters() {
    setSearch("");
    setStatusFilter("");
    setAccessFilter("");
    setIncludeArchived(false);
  }

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

  const columns: Column<AdminCoach>[] = useMemo(
    () => [
      {
        key: "coach",
        header: "Coach",
        render: (c) => (
          <div>
            <div className="admin-cell-primary">{c.name}</div>
            <div className="admin-cell-mono">{c.email}</div>
          </div>
        ),
      },
      {
        key: "status",
        header: "Status",
        render: (c) => <StatusBadge status={c.status} />,
      },
      {
        key: "access",
        header: "Acesso",
        render: (c) => (
          <CoachAccessBadge
            accessStatus={c.accessStatus}
            inviteExpiresAt={c.inviteExpiresAt}
          />
        ),
      },
      {
        key: "athletes",
        header: "Atletas",
        render: (c) => (
          <span className="admin-cell-mono">
            {c.activeAthleteCount} ativos · {c.athleteCount} total
          </span>
        ),
      },
      {
        key: "createdAt",
        header: "Criado em",
        render: (c) => (
          <span className="admin-cell-mono">
            {new Date(c.createdAt).toLocaleDateString("pt-BR")}
          </span>
        ),
      },
      {
        key: "actions",
        header: "Ações",
        render: (c) =>
          c.accessStatus === "INVITE_PENDING" ? (
            <Button
              type="button"
              variant="ghost"
              fullWidth={false}
              onClick={(event) => {
                event.stopPropagation();
                setResendCoach(c);
              }}
            >
              Reenviar convite
            </Button>
          ) : (
            <ButtonLink
              to={`/admin/coaches/${c.coachId}`}
              variant="ghost"
              onClick={(event) => event.stopPropagation()}
            >
              Abrir ficha
            </ButtonLink>
          ),
      },
    ],
    []
  );

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
          <h1>Coaches</h1>
          <p>Gerencie o acesso por convite, acompanhe o tamanho das carteiras e controle suspensões.</p>
        </div>
        <Button type="button" fullWidth={false} onClick={() => setCreateOpen(true)}>
          Novo coach
        </Button>
      </header>

      <div className="admin-toolbar">
        <input
          type="search"
          className="admin-toolbar__search"
          placeholder="Buscar por nome ou e-mail…"
          aria-label="Buscar coach por nome ou e-mail"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
        />
        <select
          className="admin-toolbar__select"
          aria-label="Filtrar por status"
          value={statusFilter}
          onChange={(event) => {
            const next = event.target.value as "" | CoachStatus;
            setStatusFilter(next);
            if (next === "CANCELLED") {
              setIncludeArchived(true);
            }
          }}
        >
          <option value="">Todos os status</option>
          <option value="ACTIVE">Ativo</option>
          <option value="SUSPENDED">Suspenso</option>
          <option value="CANCELLED">Arquivado</option>
        </select>
        <select
          className="admin-toolbar__select"
          aria-label="Filtrar por acesso"
          value={accessFilter}
          onChange={(event) =>
            setAccessFilter(event.target.value as "" | CoachAccessStatus)
          }
        >
          <option value="">Todos os acessos</option>
          <option value="ACTIVE">Acesso ativo</option>
          <option value="INVITE_PENDING">Convite pendente</option>
          <option value="DISABLED">Desabilitado</option>
        </select>
        <label className="admin-toolbar__checkbox">
          <input
            type="checkbox"
            checked={includeArchived}
            onChange={(event) => setIncludeArchived(event.target.checked)}
          />
          Mostrar arquivados
        </label>
      </div>

      {isLoading ? (
        <StateMessage
          kind="info"
          title="Carregando coaches…"
          description="Buscando a lista de coaches cadastrados."
        />
      ) : isError ? (
        <StateMessage
          kind="error"
          title="Erro ao carregar coaches"
          description="Não foi possível consultar a lista de coaches."
          onRetry={() => void refetch()}
        />
      ) : coaches.length === 0 ? (
        <StateMessage
          kind="info"
          title={hasFilters ? "Nenhum coach encontrado com os filtros atuais" : "Nenhum coach cadastrado"}
          description={
            hasFilters
              ? "Tente ajustar ou limpar os filtros para visualizar outros coaches."
              : "Adicione o primeiro coach por convite para liberar o acesso à plataforma."
          }
          actions={
            hasFilters ? (
              <Button type="button" variant="ghost" fullWidth={false} onClick={clearFilters}>
                Limpar filtros
              </Button>
            ) : (
              <Button type="button" fullWidth={false} onClick={() => setCreateOpen(true)}>
                Novo coach
              </Button>
            )
          }
        />
      ) : (
        <DataTable
          rows={coaches}
          columns={columns}
          rowKey={(c) => c.coachId}
          onRowClick={(c) => navigate(`/admin/coaches/${c.coachId}`)}
        />
      )}

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
