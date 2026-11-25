import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AppErrorBoundary } from "./AppErrorBoundary";

function Boom(): never {
  throw new Error("boom");
}

describe("AppErrorBoundary", () => {
  it("renderiza os filhos normalmente quando não há erro", () => {
    render(
      <AppErrorBoundary>
        <p>Conteúdo OK</p>
      </AppErrorBoundary>
    );
    expect(screen.getByText("Conteúdo OK")).toBeInTheDocument();
  });

  it("exibe tela de recuperação quando ocorre erro de renderização", () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => {});
    render(
      <AppErrorBoundary>
        <Boom />
      </AppErrorBoundary>
    );
    expect(screen.getByRole("alert")).toBeInTheDocument();
    expect(
      screen.getByText("Não foi possível exibir esta tela")
    ).toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Recarregar página" })
    ).toBeInTheDocument();
    spy.mockRestore();
  });
});
