import { ReactNode } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/Button";
import { logout } from "@/lib/auth";
import "./AppShell.css";

type NavItem = { label: string; href: string };

export function AppShell({
  navItems,
  headerActions,
  children,
}: {
  navItems: NavItem[];
  headerActions?: ReactNode;
  children: ReactNode;
}) {
  const navigate = useNavigate();

  return (
    <div className="app-shell">
      <header className="app-shell__header">
        <span className="app-shell__wordmark">ProCoach</span>
        {navItems.length > 0 && (
          <nav className="app-shell__nav">
            {navItems.map((item) => (
              <a key={item.href} href={item.href} className="app-shell__nav-link">
                {item.label}
              </a>
            ))}
          </nav>
        )}
        <div className="app-shell__actions">
          {headerActions}
          <Button
            variant="ghost"
            onClick={() => {
              logout();
              navigate("/login", { replace: true });
            }}
          >
            Sair
          </Button>
        </div>
      </header>
      <main className="app-shell__content">{children}</main>
    </div>
  );
}
