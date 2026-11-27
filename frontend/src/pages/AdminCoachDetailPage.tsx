import { useParams } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { ButtonLink } from "@/components/ui/ButtonLink";
import { StateMessage } from "@/components/ui/StateMessage";
import { ADMIN_NAV } from "@/lib/admin-nav";

export function AdminCoachDetailPage() {
  const { coachId } = useParams<{ coachId: string }>();

  return (
    <AppShell
      navItems={ADMIN_NAV}
      headerActions={
        <ButtonLink to="/admin/alterar-senha" variant="ghost">
          Alterar senha
        </ButtonLink>
      }
    >
      <h1>Detalhe do coach</h1>
      <div style={{ marginTop: "var(--space-5)" }}>
        <StateMessage
          kind="info"
          title={`Coach ${coachId ?? ""} (T-109)`}
          description="A carteira operacional de atletas e ações de suspensão/arquivamento entram na task T-109."
        />
      </div>
    </AppShell>
  );
}
