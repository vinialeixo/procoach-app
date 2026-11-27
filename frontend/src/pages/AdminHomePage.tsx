import { AppShell } from "@/layouts/AppShell";
import { ButtonLink } from "@/components/ui/ButtonLink";
import { StateMessage } from "@/components/ui/StateMessage";
import { ADMIN_NAV } from "@/lib/admin-nav";

export function AdminHomePage() {
  return (
    <AppShell
      navItems={ADMIN_NAV}
      headerActions={
        <ButtonLink to="/admin/alterar-senha" variant="ghost">
          Alterar senha
        </ButtonLink>
      }
    >
      <header style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: "var(--space-4)", flexWrap: "wrap" }}>
        <div>
          <h1>Painel administrativo</h1>
          <p>Visão operacional dos coaches, convites de acesso e volume de atletas.</p>
        </div>
        <ButtonLink to="/admin/coaches">Gerenciar coaches</ButtonLink>
      </header>
      <div style={{ marginTop: "var(--space-5)" }}>
        <StateMessage
          kind="info"
          title="Painel em construção (T-108)"
          description="Os indicadores, a caixa de atenção operacional e os atalhos completos serão conectados na próxima entrega."
        />
      </div>
    </AppShell>
  );
}
