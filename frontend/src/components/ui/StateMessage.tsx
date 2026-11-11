import { ReactNode } from "react";
import { Button } from "./Button";
import "./StateMessage.css";

type StateMessageProps = {
  variant: "loading" | "empty" | "error";
  children: ReactNode;
  /** Só no erro: mostra o botão "Tentar novamente". */
  onRetry?: () => void;
};

// Estados de tela padronizados (carregando / vazio / erro). Loading usa
// role="status" e erro usa role="alert" para leitores de tela.
export function StateMessage({ variant, children, onRetry }: StateMessageProps) {
  return (
    <div
      className={`pc-state pc-state--${variant}`}
      role={variant === "error" ? "alert" : variant === "loading" ? "status" : undefined}
      aria-busy={variant === "loading" || undefined}
    >
      <p>{children}</p>
      {variant === "error" && onRetry && (
        <Button type="button" variant="ghost" onClick={onRetry}>
          Tentar novamente
        </Button>
      )}
    </div>
  );
}
