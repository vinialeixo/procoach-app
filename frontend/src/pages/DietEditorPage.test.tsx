import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { DietEditorPage } from "./DietEditorPage";
import { apiClient } from "@/lib/api-client";

vi.mock("@/lib/api-client", () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

function renderEditor(athleteId = "ath-1") {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={[`/coach/athletes/${athleteId}/diets/new`]}>
        <Routes>
          <Route path="/coach/athletes/:id/diets/new" element={<DietEditorPage />} />
          <Route
            path="/coach/athletes/:id/diets/:versionId/meals"
            element={<p>Tela de refeições</p>}
          />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

const mockAthlete = {
  id: "ath-1",
  name: "Carlos Silva",
  email: "carlos@example.com",
};

const mockAssessments = [
  {
    id: "ass-1",
    athleteId: "ath-1",
    assessmentDate: "2026-11-01",
    weightKg: 80,
    heightCm: 178,
    bodyFatPercentage: 14,
    muscleMassKg: null,
    waistCm: null,
    hipCm: null,
    createdAt: "2026-11-01T10:00:00Z",
    metrics: {
      imc: 25.2,
      tmbKcal: 1750,
      getKcal: 2400,
      weightChangeKg: null,
      weightChangePercent: null,
    },
  },
];

beforeEach(() => {
  vi.clearAllMocks();
});

describe("DietEditorPage (T-094)", () => {
  it("exibe faixas de referência e avisos imediatos durante edição (4/4/9, macro zerado, kcal < TMB, déficit agressivo, proteína/gordura)", async () => {
    const user = userEvent.setup();
    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === "/athletes/ath-1") return Promise.resolve({ data: mockAthlete });
      if (url === "/diets/history") return Promise.resolve({ data: [] });
      if (url === "/assessments") return Promise.resolve({ data: mockAssessments });
      return Promise.resolve({ data: null });
    });

    renderEditor();

    expect(
      await screen.findByLabelText("Avisos nutricionais e faixas de referência")
    ).toBeInTheDocument();
    expect(screen.getByText(/Proteína \(ref\. 1,6–2,2 g\/kg\)/)).toBeInTheDocument();
    expect(screen.getByText(/Gordura \(ref\. 0,5–1,0 g\/kg\)/)).toBeInTheDocument();
    expect(screen.getByText(/Fibras \(ref\. 25–38 g\/dia\)/)).toBeInTheDocument();

    await user.selectOptions(screen.getByLabelText("Objetivo"), "CUTTING");
    await user.type(screen.getByLabelText("Calorias (kcal)"), "1500");
    await user.type(screen.getByLabelText("Proteínas (g)"), "100");
    await user.type(screen.getByLabelText("Carboidratos (g)"), "0");
    await user.type(screen.getByLabelText("Gorduras (g)"), "20");

    expect(await screen.findByText(/Carboidrato zerado/)).toBeInTheDocument();
    expect(screen.getByText(/divergem de 4\/4\/9/)).toBeInTheDocument();
    expect(screen.getByText(/abaixo da TMB/)).toBeInTheDocument();
    expect(screen.getByText(/Déficit agressivo/)).toBeInTheDocument();
    expect(screen.getByText(/Proteína \(1\.3 g\/kg\) abaixo/)).toBeInTheDocument();
    expect(screen.getByText(/Gordura \(0\.3 g\/kg\) abaixo/)).toBeInTheDocument();
    expect(
      screen.getByLabelText(/Estou ciente dos avisos nutricionais/i)
    ).toBeInTheDocument();
  });

  it("permite salvar mesmo com aviso CRITICAL/WARNING sem bloquear nem aprovar automaticamente", async () => {
    const user = userEvent.setup();
    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === "/athletes/ath-1") return Promise.resolve({ data: mockAthlete });
      if (url === "/diets/history") return Promise.resolve({ data: [] });
      if (url === "/assessments") return Promise.resolve({ data: mockAssessments });
      return Promise.resolve({ data: null });
    });
    vi.mocked(apiClient.post).mockResolvedValueOnce({
      data: {
        id: "diet-new",
        coachId: "coach-1",
        athleteId: "ath-1",
        objective: "CUTTING",
        calories: 1600,
        protein: 0,
        carbohydrates: 250,
        fat: 66,
        status: "DRAFT",
        version: 1,
        previousVersionId: null,
        changeReason: null,
        createdAt: "2026-11-10T10:00:00Z",
        updatedAt: "2026-11-10T10:00:00Z",
        readOnly: false,
        deleted: false,
        createdBy: "coach-1",
        updatedBy: "coach-1",
      },
    });

    renderEditor();

    await screen.findByLabelText("Objetivo");
    await user.selectOptions(screen.getByLabelText("Objetivo"), "CUTTING");
    await user.type(screen.getByLabelText("Calorias (kcal)"), "1600");
    await user.type(screen.getByLabelText("Proteínas (g)"), "0");
    await user.type(screen.getByLabelText("Carboidratos (g)"), "250");
    await user.type(screen.getByLabelText("Gorduras (g)"), "66");

    expect(await screen.findByText(/Proteína zerada é incompatível/i)).toBeInTheDocument();
    expect(screen.getByText("Crítico")).toBeInTheDocument();

    const submitButton = screen.getByRole("button", { name: "Criar dieta (v1)" });
    expect(submitButton).toBeEnabled();

    // Não exige marcar ciência para prosseguir (não bloqueia fluxo do coach)
    await user.click(submitButton);

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith("/diets", {
        athleteId: "ath-1",
        objective: "CUTTING",
        calories: 1600,
        protein: 0,
        carbohydrates: 250,
        fat: 66,
      });
    });

    expect(await screen.findByText("Tela de refeições")).toBeInTheDocument();
  });
});
