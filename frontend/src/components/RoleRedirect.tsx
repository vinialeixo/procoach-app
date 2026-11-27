import { Navigate } from "react-router-dom";
import { isAuthenticated, getRole, homeForRole, mustChangePassword } from "@/lib/auth";

export function RoleRedirect() {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  const role = getRole();
  if (role === "ADMIN" && mustChangePassword()) {
    return <Navigate to="/admin/alterar-senha" replace />;
  }
  return <Navigate to={homeForRole(role)} replace />;
}
