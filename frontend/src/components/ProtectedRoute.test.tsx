import { render, screen } from "@testing-library/react";
import { describe, expect, it, beforeEach } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { ProtectedRoute } from "./ProtectedRoute";

function setSession(role: "COACH" | "ATHLETE") {
  const tokenBody = btoa(JSON.stringify({ role, athleteId: role === "ATHLETE" ? "athlete-1" : undefined }));
  localStorage.setItem("procoach.token", `header.${tokenBody}.signature`);
}

function renderProtectedCoachRoute() {
  return render(
    <MemoryRouter initialEntries={["/coach"]}>
      <Routes>
        <Route element={<ProtectedRoute allow={["COACH"]} />}>
          <Route path="/coach" element={<h1>Área do coach</h1>} />
        </Route>
        <Route path="/athlete" element={<h1>Área do atleta</h1>} />
        <Route path="/login" element={<h1>Entrar</h1>} />
      </Routes>
    </MemoryRouter>
  );
}

beforeEach(() => {
  localStorage.clear();
});

describe("ProtectedRoute", () => {
  it("redirects an unauthenticated visitor to login", () => {
    renderProtectedCoachRoute();

    expect(screen.getByRole("heading", { name: "Entrar" })).toBeInTheDocument();
  });

  it("redirects an athlete away from coach routes to their own area", () => {
    setSession("ATHLETE");
    renderProtectedCoachRoute();

    expect(screen.getByRole("heading", { name: "Área do atleta" })).toBeInTheDocument();
  });

  it("renders coach content for a coach session", () => {
    setSession("COACH");
    renderProtectedCoachRoute();

    expect(screen.getByRole("heading", { name: "Área do coach" })).toBeInTheDocument();
  });
});
