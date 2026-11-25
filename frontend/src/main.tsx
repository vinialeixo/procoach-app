import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClientProvider } from "@tanstack/react-query";
import { App } from "./App";
import { AppErrorBoundary } from "./components/AppErrorBoundary";
import { queryClient } from "./lib/query-client";
import "./styles/global.css";

async function prepareMocks() {
  if (import.meta.env.VITE_USE_MOCKS === "true") {
    const { worker } = await import("./mocks/browser");
    await worker.start({ onUnhandledFrame: "bypass" });
  }
}

prepareMocks().then(() => {
  if (
    "serviceWorker" in navigator &&
    import.meta.env.PROD &&
    import.meta.env.VITE_USE_MOCKS !== "true"
  ) {
    window.addEventListener("load", () => {
      navigator.serviceWorker.register("/sw.js").catch(() => {
        // Falha silenciosa: PWA é aprimoramento progressivo e não bloqueia a SPA.
      });
    });
  }

  ReactDOM.createRoot(document.getElementById("root")!).render(
    <React.StrictMode>
      <AppErrorBoundary>
        <QueryClientProvider client={queryClient}>
          <App />
        </QueryClientProvider>
      </AppErrorBoundary>
    </React.StrictMode>
  );
});
