import { ReactNode } from "react";
import { NavLink, useNavigate } from "react-router-dom";
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
      <a className="skip-link" href="#conteudo">
        Pular para o conteúdo
      </a>
      <header className="app-shell__header">
        <span className="app-shell__wordmark">ProCoach</span>
        {navItems.length > 0 && (
          <nav className="app-shell__nav" aria-label="Principal">
            {navItems.map((item) => (
              <NavLink
                key={item.href}
                to={item.href}
                className={({ isActive }) =>
                  isActive
                    ? "app-shell__nav-link app-shell__nav-link--active"
                    : "app-shell__nav-link"
                }
              >
                {item.label}
              </NavLink>
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
      <main id="conteudo" className="app-shell__content">
        {children}
      </main>
    </div>
  );
}
