import { MemoryRouter } from "react-router-dom";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { NotFoundPage } from "./NotFoundPage";

describe("NotFoundPage", () => {
  it("exibe mensagem 404 e link para o início", () => {
    render(
      <MemoryRouter>
        <NotFoundPage />
      </MemoryRouter>
    );
    expect(screen.getByText("Página não encontrada")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Ir para o início" })).toHaveAttribute(
      "href",
      "/"
    );
  });
});
