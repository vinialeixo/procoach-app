import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { DataTable, Column } from "@/components/ui/DataTable";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { Button } from "@/components/ui/Button";
import { useAthletes, Athlete, AthleteStatus } from "@/lib/athletes";
import "./AthleteList.css";

// T-022 — Listagem de atletas (US-003). Persona: Coach.
// Exibe nome, status e objetivo; filtro por status; só aparece o que a API
// retornar (isolamento multi-tenant é do backend — RN-01, o front não filtra
// por coach).

const STATUS_OPTIONS: { value: "" | AthleteStatus; label: string }[] = [
  { value: "", label: "Todos os status" },
  { value: "ACTIVE", label: "Ativo" },
  { value: "PAUSED", label: "Pausado" },
  { value: "FINISHED", label: "Finalizado" },
];

const columns: Column<Athlete>[] = [
  { key: "name", header: "Nome", render: (a) => a.name },
  { key: "status", header: "Status", render: (a) => <StatusBadge status={a.status} /> },
  { key: "goal", header: "Objetivo", render: (a) => a.goal },
];

export function AthleteListPage() {
  const navigate = useNavigate();
  const [statusFilter, setStatusFilter] = useState<"" | AthleteStatus>("");
  const [search, setSearch] = useState("");
  const { data: athletes, isLoading, isError } = useAthletes(
    statusFilter || undefined
  );

  const filtered = (athletes ?? []).filter((a) =>
    a.name.toLowerCase().includes(search.trim().toLowerCase())
  );

  return (
    <AppShell navItems={[{ label: "Atletas", href: "/coach/athletes" }]}>
      <div className="athlete-list">
        <div className="athlete-list__header">
          <h1 className="athlete-list__title">Atletas</h1>
          <Link to="/coach/athletes/new" className="athlete-list__new">
            <Button>Novo atleta</Button>
          </Link>
        </div>

        <div className="athlete-list__toolbar">
          <input
            type="search"
            className="athlete-list__search"
            placeholder="Buscar por nome"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Buscar por nome"
          />
          <select
            className="athlete-list__filter"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as "" | AthleteStatus)}
            aria-label="Filtrar por status"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {isError && (
          <p className="athlete-list__error" role="alert">
            Não foi possível carregar os atletas. Tente novamente.
          </p>
        )}
        {isLoading ? (
          <p className="athlete-list__loading">Carregando atletas…</p>
        ) : (
          <DataTable
            columns={columns}
            rows={filtered}
            rowKey={(a) => a.id}
            emptyMessage="Nenhum atleta encontrado."
            onRowClick={(a) => navigate(`/coach/athletes/${a.id}`)}
          />
        )}
      </div>
    </AppShell>
  );
}
