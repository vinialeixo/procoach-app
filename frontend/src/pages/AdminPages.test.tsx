import { afterAll, afterEach, beforeAll, describe, expect, it } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClientProvider } from "@tanstack/react-query";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { setupServer } from "msw/node";
import { handlers } from "@/mocks/handlers";
import { resetMockDb } from "@/mocks/db";
import { createQueryClient } from "@/lib/query-client";
import { AdminHomePage } from "./AdminHomePage";
import { AdminCoachListPage } from "./AdminCoachListPage";

const server = setupServer(...handlers);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  resetMockDb();
});
afterAll(() => server.close());

function renderAdmin(initialEntry = "/admin") {
  const client = createQueryClient();
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <Routes>
          <Route path="/admin" element={<AdminHomePage />} />
          <Route path="/admin/coaches" element={<AdminCoachListPage />} />
          <Route path="/admin/coaches/:coachId" element={<div>Detalhe</div>} />
          <Route path="/admin/admins" element={<div>Admins</div>} />
          <Route path="/admin/alterar-senha" element={<div>Alterar senha</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

describe("AdminHomePage & AdminCoachListPage (T-108)", () => {
  it("renders KPIs and operational attention items on AdminHomePage", async () => {
    renderAdmin("/admin");

    expect(
      await screen.findByText("Maria Fernanda de Albuquerque Vasconcelos")
    ).toBeInTheDocument();
    expect(screen.getByText("Coaches ativos")).toBeInTheDocument();
    expect(screen.getByText("Convites pendentes")).toBeInTheDocument();
    expect(screen.getByText("Bruno Henrique Cavalcanti")).toBeInTheDocument();
  });

  it("filters coaches by status, access and archived toggle on AdminCoachListPage", async () => {
    const user = userEvent.setup();
    renderAdmin("/admin/coaches");

    expect(await screen.findByText("Vinícius Coach")).toBeInTheDocument();
    expect(screen.queryByText("Eduardo Silveira Rocha")).not.toBeInTheDocument();

    await user.click(screen.getByLabelText("Mostrar arquivados"));
    expect(await screen.findByText("Eduardo Silveira Rocha")).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText("Filtrar por acesso"), "INVITE_PENDING");
    expect(
      screen.getByText("Maria Fernanda de Albuquerque Vasconcelos")
    ).toBeInTheDocument();
    expect(screen.queryByText("Vinícius Coach")).not.toBeInTheDocument();
  });

  it("creates a coach via invite modal, shows one-time link and handles 409 duplicate email", async () => {
    const user = userEvent.setup();
    renderAdmin("/admin/coaches");

    await screen.findByText("Vinícius Coach");
    await user.click(screen.getByRole("button", { name: "Novo coach" }));

    await user.type(screen.getByLabelText("Nome do coach"), "Marina Costa");
    await user.type(screen.getByLabelText("E-mail"), "coach@procoach.app");
    await user.click(screen.getByRole("button", { name: "Criar e gerar convite" }));

    expect(
      await screen.findByText(/Este e-mail já está cadastrado na plataforma/i)
    ).toBeInTheDocument();

    await user.clear(screen.getByLabelText("E-mail"));
    await user.type(screen.getByLabelText("E-mail"), "marina@procoach.app");
    await user.click(screen.getByRole("button", { name: "Criar e gerar convite" }));

    expect(await screen.findByText("Copie este link agora")).toBeInTheDocument();
    const linkInput = screen.getByLabelText("Link de ativação do coach") as HTMLInputElement;
    expect(linkInput.value).toContain("/convite-coach/tok_");

    await user.click(screen.getByRole("button", { name: "Fechar" }));
    await waitFor(() => {
      expect(screen.getByText("Marina Costa")).toBeInTheDocument();
    });
  });

  it("resends coach invite from list and shows new link", async () => {
    const user = userEvent.setup();
    renderAdmin("/admin/coaches");

    await screen.findByText("Maria Fernanda de Albuquerque Vasconcelos");
    await user.click(screen.getByRole("button", { name: "Reenviar convite" }));
    await user.click(screen.getByRole("button", { name: "Confirmar reenvio" }));

    expect(await screen.findByText("Novo link de convite gerado")).toBeInTheDocument();
    const linkInput = screen.getByLabelText("Novo link de convite") as HTMLInputElement;
    expect(linkInput.value).toContain("/convite-coach/tok_");
  });
});
