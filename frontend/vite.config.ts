import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";

// ProCoach frontend — SPA (Web-first, ver AGENT.md ADR-005)
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { "@": path.resolve(import.meta.dirname, "./src") },
  },
  server: {
    port: 5173,
    proxy: {
      // Núcleo Java roda em 8080 (ver backend/src/main/resources/application.yml)
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
