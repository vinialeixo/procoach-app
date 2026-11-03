import { Navigate, Outlet } from "react-router-dom";
import { isAuthenticated, getRole } from "@/lib/auth";

type Role = "COACH" | "ATHLETE";

// RF-11 (Atleta só acessa próprios dados) e RN-01 são validados sempre no
// núcleo (backend) — isto é só conveniência de navegação no front, não
// substitui a checagem de autorização real.
export function ProtectedRoute({ allow }: { allow: Role[] }) {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  const role = getRole();

  if (!role || !allow.includes(role)) {
    // Logado, mas na área errada (ex.: atleta tentando abrir rota de coach) —
    // manda pra home do próprio papel em vez de deixar acessar.
    const home = role === "ATHLETE" ? "/athlete" : "/coach";
    return <Navigate to={home} replace />;
  }

  return <Outlet />;
}
