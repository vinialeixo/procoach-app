import { AppShell } from "@/layouts/AppShell";
import { ButtonLink } from "@/components/ui/ButtonLink";
import { StateMessage } from "@/components/ui/StateMessage";
import { ADMIN_NAV } from "@/lib/admin-nav";

export function AdminCoachListPage() {
  return (
    <AppShell
      navItems={ADMIN_NAV}
      headerActions={
        <ButtonLink to="/admin/alterar-senha" variant="ghost">
          Alterar senha
        </ButtonLink>
      }
    >
      <h1>Coaches</h1>
      <div style={{ marginTop: "var(--space-5)" }}>
        <StateMessage
          kind="info"
          title="Listagem de coaches (T-108)"
          description="A busca, filtros de status/acesso e criação por convite entram na task T-108."
        />
      </div>
    </AppShell>
  );
}
