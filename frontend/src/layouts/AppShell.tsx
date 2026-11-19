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

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="app-shell">
      <a className="skip-link" href="#conteudo">
        Pular para o conteúdo
      </a>

      <aside className="app-shell__rail">
        <div className="app-shell__brand-row">
          <span className="app-shell__wordmark">ProCoach</span>
          <span className="app-shell__edition">DOSSIÊ</span>
        </div>

        {navItems.length > 0 && (
          <nav className="app-shell__nav" aria-label="Principal">
            {navItems.map((item) => (
              <NavLink
                key={item.href}
                to={item.href}
                end={item.href === "/coach" || item.href === "/athlete"}
                className={({ isActive }) =>
                  isActive
                    ? "app-shell__nav-link app-shell__nav-link--active"
                    : "app-shell__nav-link"
                }
              >
                <span className="app-shell__nav-label">{item.label}</span>
                <span className="app-shell__nav-dot" aria-hidden="true" />
              </NavLink>
            ))}
          </nav>
        )}

        <div className="app-shell__actions">
          {headerActions}
          <Button
            variant="ghost"
            fullWidth={false}
            className="app-shell__logout"
            onClick={handleLogout}
          >
            Sair
          </Button>
        </div>
      </aside>

      <main id="conteudo" className="app-shell__content">
        {children}
      </main>
    </div>
  );
}
