import { Navigate } from "react-router-dom";
import { isAuthenticated, getRole } from "@/lib/auth";

export function RoleRedirect() {
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }
  const role = getRole();
  return <Navigate to={role === "ATHLETE" ? "/athlete" : "/coach"} replace />;
}
