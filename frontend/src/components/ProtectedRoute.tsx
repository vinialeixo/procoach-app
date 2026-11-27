import { Navigate, Outlet, useLocation } from "react-router-dom";
import {
  isAuthenticated,
  getRole,
  homeForRole,
  mustChangePassword,
  type Role,
} from "@/lib/auth";

// RF-11 (Atleta só acessa próprios dados), RN-01 e BR-ADM-01 são validados
// sempre no núcleo (backend) — isto é só conveniência de navegação no front,
// não substitui a checagem de autorização real.
export function ProtectedRoute({ allow }: { allow: Role[] }) {
  const location = useLocation();

  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  const role = getRole();

  if (!role || !allow.includes(role)) {
    // Logado, mas na área errada — manda pra home do próprio papel.
    return <Navigate to={homeForRole(role)} replace />;
  }

  if (
    role === "ADMIN" &&
    mustChangePassword() &&
    location.pathname !== "/admin/alterar-senha"
  ) {
    return <Navigate to="/admin/alterar-senha" replace />;
  }

  return <Outlet />;
}
