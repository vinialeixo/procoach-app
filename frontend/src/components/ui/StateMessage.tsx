import { ReactNode } from "react";
import { Button } from "./Button";
import "./StateMessage.css";

type StateMessageKind = "info" | "warning" | "error" | "success";

type StateMessageProps = {
  kind: StateMessageKind;
  title?: string;
  description?: ReactNode;
  /** Ações customizadas (botões, etc.) — exibidas abaixo da descrição. */
  actions?: ReactNode;
  /** Só no erro: mostra o botão "Tentar novamente". */
  onRetry?: () => void;
};

// Estados de tela padronizados (info / warning / error / success).
// info/warning usam role="status", erro usa role="alert" para leitores de tela.
export function StateMessage({
  kind,
  title,
  description,
  actions,
  onRetry,
}: StateMessageProps) {
  const role = kind === "error" ? "alert" : "status";

  return (
    <div className={`pc-state pc-state--${kind}`} role={role}>
      {title && <strong className="pc-state__title">{title}</strong>}
      {description && <p className="pc-state__description">{description}</p>}
      {actions && <div className="pc-state__actions">{actions}</div>}
      {kind === "error" && onRetry && (
        <Button type="button" variant="ghost" onClick={onRetry} className="pc-state__retry">
          Tentar novamente
        </Button>
      )}
    </div>
  );
}
