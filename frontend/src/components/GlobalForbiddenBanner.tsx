import { useEffect, useSyncExternalStore } from "react";
import { useLocation } from "react-router-dom";
import { Button } from "@/components/ui/Button";
import { StateMessage } from "@/components/ui/StateMessage";
import {
  clearForbiddenError,
  getForbiddenError,
  subscribeForbiddenError,
} from "@/lib/query-client";

export function GlobalForbiddenBanner() {
  const { pathname } = useLocation();
  const message = useSyncExternalStore(
    subscribeForbiddenError,
    getForbiddenError,
    getForbiddenError
  );

  useEffect(() => {
    clearForbiddenError();
  }, [pathname]);

  if (!message) {
    return null;
  }

  return (
    <div
      style={{
        position: "fixed",
        top: "var(--space-4)",
        right: "var(--space-4)",
        left: "var(--space-4)",
        maxWidth: "560px",
        margin: "0 auto",
        zIndex: 1000,
      }}
    >
      <StateMessage
        kind="error"
        title="Acesso negado"
        description={message}
        actions={
          <Button type="button" variant="ghost" onClick={clearForbiddenError}>
            Fechar
          </Button>
        }
      />
    </div>
  );
}
