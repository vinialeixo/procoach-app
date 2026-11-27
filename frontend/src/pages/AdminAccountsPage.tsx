import { AppShell } from "@/layouts/AppShell";
import { ButtonLink } from "@/components/ui/ButtonLink";
import { StateMessage } from "@/components/ui/StateMessage";
import { ADMIN_NAV } from "@/lib/admin-nav";

export function AdminAccountsPage() {
  return (
    <AppShell
      navItems={ADMIN_NAV}
      headerActions={
        <ButtonLink to="/admin/alterar-senha" variant="ghost">
          Alterar senha
        </ButtonLink>
      }
    >
      <h1>Administradores</h1>
      <div style={{ marginTop: "var(--space-5)" }}>
        <StateMessage
          kind="info"
          title="Gestão de administradores (T-110)"
          description="A listagem, criação e reset de senha de administradores entram na task T-110."
        />
      </div>
    </AppShell>
  );
}
