import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { DietMealsPage } from "./DietMealsPage";
import { apiClient } from "@/lib/api-client";
import type { ApiDiet } from "@/lib/diets";

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

function renderPage(athleteId = "ath-1", versionId = "diet-1") {
  return render(
    <QueryClientProvider client={createClient()}>
      <MemoryRouter initialEntries={[`/coach/athletes/${athleteId}/diets/${versionId}/meals`]}>
        <Routes>
          <Route
            path="/coach/athletes/:id/diets/:versionId/meals"
            element={<DietMealsPage />}
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

function makeDiet(overrides: Partial<ApiDiet> = {}): ApiDiet {
  return {
    id: "diet-1",
    coachId: "coach-1",
    athleteId: "ath-1",
    objective: "HYPERTROPHY",
    calories: 2500,
    protein: 180,
    carbohydrates: 280,
    fat: 70,
    status: "DRAFT",
    version: 1,
    previousVersionId: null,
    changeReason: null,
    createdAt: "2026-04-01T10:00:00Z",
    updatedAt: "2026-04-01T10:00:00Z",
    readOnly: false,
    deleted: false,
    createdBy: "coach-1",
    updatedBy: "coach-1",
    meals: [],
    totals: { calories: 0, protein: 0, carbohydrates: 0, fat: 0 },
    ...overrides,
  };
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe("DietMealsPage (T-093)", () => {
  it("exibe cabeçalho, metas prescritas, totais e checagem 4/4/9", async () => {
    const diet = makeDiet({
      totals: { calories: 1000, protein: 100, carbohydrates: 100, fat: 20 },
    });

    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === "/athletes/ath-1") return Promise.resolve({ data: mockAthlete });
      if (url === "/diets/diet-1") return Promise.resolve({ data: diet });
      return Promise.resolve({ data: null });
    });

    renderPage();

    expect(await screen.findByText(/Refeições da dieta — Carlos Silva/)).toBeInTheDocument();
    expect(screen.getByText("v1")).toBeInTheDocument();
    expect(screen.getByText("Hipertrofia")).toBeInTheDocument();
    expect(screen.getByText("Rascunho")).toBeInTheDocument();

    expect(screen.getByRole("link", { name: /Voltar para a ficha/i })).toHaveAttribute(
      "href",
      "/coach/athletes/ath-1?tab=dietas"
    );
    expect(screen.getByRole("link", { name: /Resumo da dieta/i })).toHaveAttribute(
      "href",
      "/coach/athletes/ath-1/diets/diet-1"
    );

    expect(screen.getByText(/Checagem 4\/4\/9: 980 kcal/)).toBeInTheDocument();
  });

  it("permite criar refeição com ordem padrão e metas opcionais", async () => {
    const user = userEvent.setup();
    const diet = makeDiet();
    const updatedDiet = makeDiet({
      meals: [
        {
          id: "meal-1",
          name: "Café da manhã",
          order: 1,
          time: "07:30",
          notes: "Pré-treino",
          target: { calories: 600, protein: 40, carbohydrates: 70, fat: 15 },
          items: [],
          totals: { calories: 0, protein: 0, carbohydrates: 0, fat: 0 },
        },
      ],
    });

    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === "/athletes/ath-1") return Promise.resolve({ data: mockAthlete });
      if (url === "/diets/diet-1") return Promise.resolve({ data: diet });
      return Promise.resolve({ data: null });
    });
    vi.mocked(apiClient.post).mockResolvedValueOnce({ data: updatedDiet });

    renderPage();

    await screen.findByLabelText("Nome da refeição");
    expect(screen.getByLabelText("Ordem")).toHaveValue(1);

    await user.type(screen.getByLabelText("Nome da refeição"), "Café da manhã");
    await user.type(screen.getByLabelText("Horário (HH:mm)"), "07:30");
    await user.type(screen.getByLabelText("Observações"), "Pré-treino");
    await user.click(screen.getByLabelText(/Definir metas de macros/i));
    await user.type(screen.getByLabelText("Meta calorias (kcal)"), "600");
    await user.type(screen.getByLabelText("Meta proteínas (g)"), "40");
    await user.type(screen.getByLabelText("Meta carboidratos (g)"), "70");
    await user.type(screen.getByLabelText("Meta gorduras (g)"), "15");

    await user.click(screen.getByRole("button", { name: "Adicionar refeição" }));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith("/diets/diet-1/meals", {
        name: "Café da manhã",
        order: 1,
        time: "07:30",
        notes: "Pré-treino",
        target: { calories: 600, protein: 40, carbohydrates: 70, fat: 15 },
      });
    });
  });

  it("separa visualmente FOOD e SUPPLEMENT_CALORIC com badges RAW/AS_SERVED e permite adicionar/remover itens e refeições", async () => {
    const user = userEvent.setup();
    const diet = makeDiet({
      meals: [
        {
          id: "meal-2",
          name: "Almoço",
          order: 2,
          time: "12:00",
          notes: null,
          target: null,
          items: [
            {
              id: "item-1",
              foodId: null,
              name: "Arroz branco",
              source: "TACO",
              kind: "FOOD",
              state: "RAW",
              quantityG: 150,
              householdMeasure: "3 colheres de servir",
              preparation: "cozido sem óleo",
              calories: 192,
              protein: 3.8,
              carbohydrates: 42,
              fat: 0.4,
            },
            {
              id: "item-2",
              foodId: null,
              name: "Whey Concentrado",
              source: "CUSTOM",
              kind: "SUPPLEMENT_CALORIC",
              state: "AS_SERVED",
              quantityG: 30,
              householdMeasure: "1 scoop",
              preparation: "diluído em água",
              calories: 120,
              protein: 24,
              carbohydrates: 3,
              fat: 1.5,
            },
          ],
          totals: { calories: 312, protein: 27.8, carbohydrates: 45, fat: 1.9 },
        },
        {
          id: "meal-1",
          name: "Desjejum",
          order: 1,
          time: "07:00",
          notes: null,
          target: null,
          items: [],
          totals: { calories: 0, protein: 0, carbohydrates: 0, fat: 0 },
        },
      ],
    });

    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === "/athletes/ath-1") return Promise.resolve({ data: mockAthlete });
      if (url === "/diets/diet-1") return Promise.resolve({ data: diet });
      return Promise.resolve({ data: null });
    });
    vi.mocked(apiClient.post).mockResolvedValue({ data: diet });
    vi.mocked(apiClient.delete).mockResolvedValue({ data: diet });

    renderPage();

    const mealCards = await screen.findAllByRole("article");
    expect(mealCards).toHaveLength(2);
    // Ordenação por order: Desjejum (#1) antes de Almoço (#2)
    expect(within(mealCards[0]).getByText("Desjejum")).toBeInTheDocument();
    expect(within(mealCards[1]).getByText("Almoço")).toBeInTheDocument();

    const almoco = mealCards[1];
    expect(within(almoco).getByText("Alimentos")).toBeInTheDocument();
    expect(within(almoco).getByText("Suplementos calóricos")).toBeInTheDocument();
    expect(within(almoco).getByText("Arroz branco")).toBeInTheDocument();
    expect(within(almoco).getByText("Whey Concentrado")).toBeInTheDocument();
    expect(within(almoco).getAllByText("Peso cru").length).toBeGreaterThanOrEqual(1);
    expect(within(almoco).getAllByText("Como servido").length).toBeGreaterThanOrEqual(1);
    expect(within(almoco).getByText(/3 colheres de servir · cozido sem óleo/)).toBeInTheDocument();
    expect(within(almoco).getByText(/1 scoop · diluído em água/)).toBeInTheDocument();

    // Adicionar item no Desjejum (meal-1)
    const desjejum = mealCards[0];
    expect(
      within(desjejum).getByText(/busca automática no catálogo TACO será integrada após backend T-086/i)
    ).toBeInTheDocument();

    await user.type(within(desjejum).getByLabelText("Alimento"), "Hipercalórico");
    await user.selectOptions(within(desjejum).getByLabelText("Origem"), "CUSTOM");
    await user.selectOptions(within(desjejum).getByLabelText("Tipo"), "SUPPLEMENT_CALORIC");
    await user.selectOptions(within(desjejum).getByLabelText("Estado"), "AS_SERVED");
    await user.type(within(desjejum).getByLabelText("Quantidade (g)"), "60");
    await user.type(within(desjejum).getByLabelText("Calorias (kcal)"), "240");
    await user.type(within(desjejum).getByLabelText("Proteínas (g)"), "15");
    await user.type(within(desjejum).getByLabelText("Carboidratos (g)"), "40");
    await user.type(within(desjejum).getByLabelText("Gorduras (g)"), "2");
    await user.click(within(desjejum).getByRole("button", { name: "Adicionar item" }));

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith("/diets/diet-1/meals/meal-1/items", {
        name: "Hipercalórico",
        source: "CUSTOM",
        kind: "SUPPLEMENT_CALORIC",
        state: "AS_SERVED",
        quantityG: 60,
        householdMeasure: null,
        preparation: null,
        calories: 240,
        protein: 15,
        carbohydrates: 40,
        fat: 2,
      });
    });

    // Remover item
    await user.click(screen.getByRole("button", { name: "Remover item Arroz branco" }));
    await waitFor(() => {
      expect(apiClient.delete).toHaveBeenCalledWith(
        "/diets/diet-1/meals/meal-2/items/item-1"
      );
    });

    // Remover refeição
    await user.click(screen.getByRole("button", { name: "Remover refeição Desjejum" }));
    await waitFor(() => {
      expect(apiClient.delete).toHaveBeenCalledWith("/diets/diet-1/meals/meal-1");
    });
  });

  it("bloqueia edição e oculta formulários quando a dieta não está em DRAFT ou está readOnly", async () => {
    const approvedDiet = makeDiet({
      status: "APPROVED",
      readOnly: true,
      meals: [
        {
          id: "meal-1",
          name: "Almoço",
          order: 1,
          time: "12:00",
          notes: null,
          target: null,
          items: [
            {
              id: "item-1",
              foodId: null,
              name: "Frango",
              source: "TACO",
              kind: "FOOD",
              state: "RAW",
              quantityG: 150,
              householdMeasure: null,
              preparation: null,
              calories: 240,
              protein: 45,
              carbohydrates: 0,
              fat: 5,
            },
          ],
          totals: { calories: 240, protein: 45, carbohydrates: 0, fat: 5 },
        },
      ],
    });

    vi.mocked(apiClient.get).mockImplementation((url: string) => {
      if (url === "/athletes/ath-1") return Promise.resolve({ data: mockAthlete });
      if (url === "/diets/diet-1") return Promise.resolve({ data: approvedDiet });
      return Promise.resolve({ data: null });
    });

    renderPage();

    const alert = await screen.findByRole("alert");
    expect(alert).toHaveTextContent(/Modo somente leitura/i);
    expect(screen.queryByRole("button", { name: "Adicionar refeição" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Adicionar item" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Remover refeição/i })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Remover item/i })).not.toBeInTheDocument();
  });
});

