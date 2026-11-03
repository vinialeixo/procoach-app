import { logout } from "@/lib/auth";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/Button";
import "./DashboardPage.css";

export function DashboardPage() {
  const navigate = useNavigate();

  return (
    <div className="dashboard-page">
      <header className="dashboard-page__header">
        <span className="dashboard-page__wordmark">ProCoach</span>
        <Button
          variant="ghost"
          onClick={() => {
            logout();
            navigate("/login", { replace: true });
          }}
        >
          Sair
        </Button>
      </header>
      <main className="dashboard-page__content">
        <h1>Painel do coach</h1>
        <p>
          Login concluído. As telas de gestão de atletas, dietas e treinos
          entram aqui conforme as próximas tasks do backlog (T-019, T-022,
          T-027, T-034...).
        </p>
      </main>
    </div>
  );
}
