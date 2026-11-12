import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { TrainingPage } from "./TrainingPage";
import { apiClient } from "@/lib/api-client";

vi.mock("@/lib/api-client", () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

function createClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
}

const mockAthletes = [
  { id: "ath-1", name: "Carlos Silva" },
  { id: "ath-2", name: "Ana Souza" },
];

const mockPeriodizations = [
  {
    id: "per-1",
    athleteId: "ath-1",
    name: "Macrociclo 2026",
    startDate: "2026-01-01",
    endDate: "2026-06-30",
    deleted: false,
  },
];

const mockPlans = [
  {
    id: "plan-1",
    athleteId: "ath-1",
    periodizationId: "per-1",
    version: 1,
    name: "Hipertrofia Fase 1",
    description: "Fase de volume",
    status: "ACTIVE",
    deleted: false,
  },
];

const mockSessions = [
  {
    id: "ses-1",
    planId: "plan-1",
    periodizationId: "per-1",
    name: "Treino A - Peito",
    weekNumber: 1,
    dayOfWeek: 1,
    status: "ACTIVE",
    exercises: [],
    deleted: false,
  },
];

beforeEach(() => {
  vi.clearAllMocks();
});

function setupMocks() {
  vi.mocked(apiClient.get).mockImplementation((url: string, config?: unknown) => {
    const requestConfig = config as { params?: { planId?: string } } | undefined;
    if (url === "/athletes") {
      return Promise.resolve({ data: mockAthletes });
    }
    if (url.includes("/api/training/periodizations/athlete/ath-1")) {
      return Promise.resolve({ data: mockPeriodizations });
    }
    if (url.includes("/api/training/plans/athlete/ath-1")) {
      return Promise.resolve({ data: mockPlans });
    }
    if (url.includes("/api/training/trainings/athlete/ath-1")) {
      if (!requestConfig?.params?.planId || requestConfig.params.planId === "plan-1") {
        return Promise.resolve({ data: mockSessions });
      }
      return Promise.resolve({ data: [] });
    }
    if (url.includes("/api/training/trainings/periodization/per-1")) {
      return Promise.resolve({ data: mockSessions });
    }
    if (url.includes("/api/library/exercises")) {
      return Promise.resolve({ data: [] });
    }
    return Promise.resolve({ data: [] });
  });
}

describe("TrainingPage (Issues #39, #40, #41)", () => {
  it("carrega hierarquia completa a partir dos query params da URL (Issue #40)", async () => {
    setupMocks();

    render(
      <QueryClientProvider client={createClient()}>
        <MemoryRouter
          initialEntries={[
            "/coach/training?athlete=ath-1&periodization=per-1&plan=plan-1&session=ses-1",
          ]}
        >
          <Routes>
            <Route path="/coach/training" element={<TrainingPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      // Atleta selecionado
      expect(screen.getByDisplayValue("Carlos Silva")).toBeInTheDocument();
      // Periodização selecionada
      expect(screen.getByDisplayValue(/Macrociclo 2026/)).toBeInTheDocument();
      // Plano selecionado
      expect(screen.getByDisplayValue(/Hipertrofia Fase 1/)).toBeInTheDocument();
    });

    // Sessão selecionada no editor
    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Treino A - Peito" })).toBeInTheDocument();
    });
  });

  it("exibe alerta quando ID de atleta inválido é passado na URL", async () => {
    setupMocks();

    render(
      <QueryClientProvider client={createClient()}>
        <MemoryRouter initialEntries={["/coach/training?athlete=ath-inexistente"]}>
          <Routes>
            <Route path="/coach/training" element={<TrainingPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(
        screen.getByText(
          /Atleta informado na URL não foi encontrado ou você não tem permissão para acessá-lo/
        )
      ).toBeInTheDocument();
    });
  });

  it("exibe alerta quando ID de plano inválido é passado na URL", async () => {
    setupMocks();

    render(
      <QueryClientProvider client={createClient()}>
        <MemoryRouter
          initialEntries={[
            "/coach/training?athlete=ath-1&periodization=per-1&plan=plan-inexistente",
          ]}
        >
          <Routes>
            <Route path="/coach/training" element={<TrainingPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(
        screen.getByText(/Plano informado na URL não foi encontrado nesta periodização/)
      ).toBeInTheDocument();
    });
  });

  it("atualiza a seleção e limpa níveis inferiores ao trocar de atleta", async () => {
    const user = userEvent.setup();
    setupMocks();

    render(
      <QueryClientProvider client={createClient()}>
        <MemoryRouter
          initialEntries={[
            "/coach/training?athlete=ath-1&periodization=per-1&plan=plan-1&session=ses-1",
          ]}
        >
          <Routes>
            <Route path="/coach/training" element={<TrainingPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByDisplayValue("Carlos Silva")).toBeInTheDocument();
    });

    // Troca para Ana Souza
    const athleteSelect = screen.getByLabelText("Atleta");
    await user.selectOptions(athleteSelect, "ath-2");

    await waitFor(() => {
      expect(screen.getByDisplayValue("Ana Souza")).toBeInTheDocument();
    });
  });

  it("seleciona sessão e foca campo de prescrição ao clicar no atalho de adicionar exercício (Issue #42)", async () => {
    const user = userEvent.setup();
    setupMocks();

    render(
      <QueryClientProvider client={createClient()}>
        <MemoryRouter
          initialEntries={[
            "/coach/training?athlete=ath-1&periodization=per-1&plan=plan-1",
          ]}
        >
          <Routes>
            <Route path="/coach/training" element={<TrainingPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText("Treino A - Peito")).toBeInTheDocument();
    });

    // Clique 1: atalho "+ Adicionar exercício" na grade
    const addExerciseShortcut = screen.getByRole("button", { name: "+ Adicionar exercício" });
    await user.click(addExerciseShortcut);

    // O editor da sessão abre e o campo de busca de exercício ganha foco
    await waitFor(() => {
      expect(screen.getByRole("heading", { name: "Treino A - Peito" })).toBeInTheDocument();
      expect(screen.getByLabelText("Buscar exercício")).toHaveFocus();
    });
  });

  it("permite criar nova versão do plano com estratégia de duplicação por padrão (Issue #43)", async () => {
    const user = userEvent.setup();
    setupMocks();

    vi.mocked(apiClient.post).mockResolvedValue({
      data: {
        id: "plan-2",
        athleteId: "ath-1",
        periodizationId: "per-1",
        version: 2,
        name: "Hipertrofia Fase 1 (v2)",
        description: "Ajuste de volume",
        status: "DRAFT",
        previousVersionId: "plan-1",
        deleted: false,
      },
    });

    render(
      <QueryClientProvider client={createClient()}>
        <MemoryRouter
          initialEntries={[
            "/coach/training?athlete=ath-1&periodization=per-1&plan=plan-1",
          ]}
        >
          <Routes>
            <Route path="/coach/training" element={<TrainingPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Criar nova versão" })).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: "Criar nova versão" }));

    // Modal aberto com opção de duplicação pré-selecionada
    expect(
      screen.getByRole("heading", { name: "Criar nova versão do plano" })
    ).toBeInTheDocument();

    const duplicateRadio = screen.getByLabelText(
      /Duplicar sessões e prescrições da versão atual \(Recomendado\)/
    ) as HTMLInputElement;
    expect(duplicateRadio.checked).toBe(true);

    await user.click(screen.getByRole("button", { name: "Criar versão" }));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith(
        "/api/training/plans/plan-1/versions",
        expect.objectContaining({
          copySessions: true,
        }),
        expect.any(Object)
      );
    });
  });

  it("busca sessões filtrando por planId diretamente no backend (Issue #45)", async () => {
    setupMocks();

    render(
      <QueryClientProvider client={createClient()}>
        <MemoryRouter
          initialEntries={[
            "/coach/training?athlete=ath-1&periodization=per-1&plan=plan-1",
          ]}
        >
          <Routes>
            <Route path="/coach/training" element={<TrainingPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText("Treino A - Peito")).toBeInTheDocument();
    });

    // Garante que a requisição de treinos foi feita diretamente por atleta e planId
    expect(apiClient.get).toHaveBeenCalledWith(
      "/api/training/trainings/athlete/ath-1",
      expect.objectContaining({
        params: { planId: "plan-1" },
      })
    );

    // Garante que não busca mais todas as sessões da periodização
    const calledUrls = vi.mocked(apiClient.get).mock.calls.map((call) => call[0]);
    expect(
      calledUrls.some((url) =>
        url.includes("/api/training/trainings/periodization/per-1")
      )
    ).toBe(false);
  });
});
