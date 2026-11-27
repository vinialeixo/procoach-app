import { afterEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Navigate, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./ProtectedRoute";
import { LoginPage } from "@/pages/LoginPage";
import { createQueryClient } from "@/lib/query-client";
import { homeForRole } from "@/lib/auth";
import {
  apiClient,
  resetRefreshStateForTests,
  setPasswordChangeRequiredHandler,
} from "@/lib/api-client";

function saveJwt(claims: Record<string, unknown>) {
  const payload = btoa(JSON.stringify(claims));
  localStorage.setItem("procoach.token", `header.${payload}.signature`);
}

afterEach(() => {
  localStorage.clear();
  resetRefreshStateForTests();
});

function renderRoutes(initialEntry = "/coach") {
  const client = createQueryClient();
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/registro"
            element={<Navigate to="/login?reason=invite-only" replace />}
          />
          <Route element={<ProtectedRoute allow={["ADMIN"]} />}>
            <Route path="/admin" element={<div>Área do Admin</div>} />
            <Route
              path="/admin/alterar-senha"
              element={<div>Alterar Senha Obrigatória</div>}
            />
          </Route>
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

describe("ProtectedRoute & admin routing", () => {
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

  it("allows ADMIN on /admin", () => {
    saveJwt({ role: "ADMIN", exp: Math.floor(Date.now() / 1000) + 300 });
    renderRoutes("/admin");
    expect(screen.getByText("Área do Admin")).toBeInTheDocument();
  });

  it("blocks COACH from /admin and redirects to /coach", () => {
    saveJwt({ role: "COACH", exp: Math.floor(Date.now() / 1000) + 300 });
    renderRoutes("/admin");
    expect(screen.getByText("Área do Coach")).toBeInTheDocument();
    expect(screen.queryByText("Área do Admin")).not.toBeInTheDocument();
  });

  it("redirects ADMIN with mustChangePassword=true to /admin/alterar-senha", () => {
    saveJwt({
      role: "ADMIN",
      mustChangePassword: true,
      exp: Math.floor(Date.now() / 1000) + 300,
    });

    renderRoutes("/admin");

    expect(screen.getByText("Alterar Senha Obrigatória")).toBeInTheDocument();
    expect(screen.queryByText("Área do Admin")).not.toBeInTheDocument();
  });

  it("redirects /registro to /login with invite-only notice and hides public register link", () => {
    renderRoutes("/registro");

    expect(screen.getByText("Cadastro por convite")).toBeInTheDocument();
    expect(screen.getByLabelText("E-mail ou usuário")).toBeInTheDocument();
    expect(screen.queryByText("Criar conta")).not.toBeInTheDocument();
  });

  it("resolves homeForRole for ADMIN, COACH, ATHLETE and null", () => {
    expect(homeForRole("ADMIN")).toBe("/admin");
    expect(homeForRole("COACH")).toBe("/coach");
    expect(homeForRole("ATHLETE")).toBe("/athlete");
    expect(homeForRole(null)).toBe("/login");
  });

  it("triggers passwordChangeRequiredHandler on 403 PASSWORD_CHANGE_REQUIRED", async () => {
    const handler = vi.fn();
    setPasswordChangeRequiredHandler(handler);

    apiClient.defaults.adapter = async (config) => {
      throw {
        isAxiosError: true,
        config,
        response: {
          status: 403,
          data: { code: "PASSWORD_CHANGE_REQUIRED" },
        },
      };
    };

    await expect(apiClient.get("/admin/coaches")).rejects.toBeDefined();
    expect(handler).toHaveBeenCalledTimes(1);
  });
});
