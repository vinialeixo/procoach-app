import { ReactNode } from "react";
import "./AuthLayout.css";

// Entradas ilustrativas — refletem o próprio mecanismo do produto
// (versionamento + auditoria imutável, ver AGENT.md) em vez de imagem
// genérica de academia. Nenhuma chamada de API acontece aqui; é decorativo.
const LEDGER_ENTRIES = [
  { v: "v14", action: "Dieta aprovada", who: "coach.silva" },
  { v: "v13", action: "Avaliação registrada", who: "coach.silva" },
  { v: "v12", action: "Treino versionado", who: "coach.oliveira" },
  { v: "v11", action: "Atleta adicionado", who: "coach.ramos" },
  { v: "v10", action: "Dieta em revisão", who: "coach.silva" },
  { v: "v09", action: "Check-in recebido", who: "atleta.costa" },
];

export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="auth-layout">
      <aside className="auth-layout__brand" aria-hidden="true">
        <div className="auth-layout__brand-content">
          <span className="auth-layout__wordmark">ProCoach</span>
          <p className="auth-layout__tagline">
            Cada decisão registrada. Cada versão rastreável.
          </p>

          <ul className="auth-layout__ledger">
            {LEDGER_ENTRIES.map((entry, i) => (
              <li
                key={entry.v}
                className="auth-layout__ledger-row"
                style={{ opacity: 1 - i * 0.14 }}
              >
                <span className="auth-layout__ledger-v">{entry.v}</span>
                <span className="auth-layout__ledger-action">{entry.action}</span>
                <span className="auth-layout__ledger-who">{entry.who}</span>
              </li>
            ))}
          </ul>
        </div>
      </aside>

      <main className="auth-layout__panel">
        <div className="auth-layout__panel-inner">{children}</div>
      </main>
    </div>
  );
}
