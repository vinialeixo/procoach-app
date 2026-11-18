import { afterEach, describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./ProtectedRoute";
import { LoginPage } from "@/pages/LoginPage";
import { createQueryClient } from "@/lib/query-client";

function saveJwt(claims: Record<string, unknown>) {
  const payload = btoa(JSON.stringify(claims));
  localStorage.setItem("procoach.token", `header.${payload}.signature`);
}

afterEach(() => {
  localStorage.clear();
});

function renderRoutes(initialEntry = "/coach") {
  const client = createQueryClient();
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<ProtectedRoute allow={["COACH"]} />}>
            <Route path="/coach" element={<div>Área do Coach</div>} />
          </Route>
          <Route element={<ProtectedRoute allow={["ATHLETE"]} />}>
            <Route path="/athlete" element={<div>Área do Atleta</div>} />
          </Route>
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("ProtectedRoute", () => {
  it("redirects to /login when access token is expired and there is no refreshToken", () => {
    saveJwt({ role: "COACH", exp: Math.floor(Date.now() / 1000) - 120 });

    renderRoutes("/coach");

    expect(screen.getByRole("heading", { name: "Entrar" })).toBeInTheDocument();
    expect(screen.queryByText("Área do Coach")).not.toBeInTheDocument();
  });

  it("keeps user on protected route when access token is expired but refreshToken exists", () => {
    saveJwt({ role: "COACH", exp: Math.floor(Date.now() / 1000) - 120 });
    localStorage.setItem("procoach.refreshToken", "valid-refresh");

    renderRoutes("/coach");

    expect(screen.getByText("Área do Coach")).toBeInTheDocument();
  });

  it("renders expired session message on LoginPage when reason=session-expired", () => {
    renderRoutes("/login?reason=session-expired");

    expect(screen.getByText("Sessão expirada")).toBeInTheDocument();
    expect(
      screen.getByText("Sua sessão expirou. Faça login novamente para continuar.")
    ).toBeInTheDocument();
  });
});
