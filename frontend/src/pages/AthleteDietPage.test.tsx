import { ReactNode } from "react";
import { render, screen, waitFor, within } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "@/lib/api-client";
import { getAthleteId } from "@/lib/auth";
import { ApiDiet } from "@/lib/diets";
import { AthleteDietPage } from "./AthleteDietPage";

vi.mock("@/lib/api-client", () => ({
  apiClient: {
    get: vi.fn(),
  },
}));

vi.mock("@/lib/auth", () => ({
  getAthleteId: vi.fn(),
}));

vi.mock("@/layouts/AthleteShell", () => ({
  AthleteShell: ({ children }: { children: ReactNode }) => <div>{children}</div>,
}));

function renderPage() {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>
      <AthleteDietPage />
    </QueryClientProvider>
  );
}

function makeDietResponse(overrides: Partial<ApiDiet> = {}): ApiDiet {
  return {
    id: "diet-1",
    coachId: "coach-1",
    athleteId: "ath-1",
    objective: "HYPERTROPHY",
    calories: 1800,
    protein: 180,
    carbohydrates: 180,
    fat: 40,
    status: "APPROVED",
    version: 2,
    previousVersionId: "diet-0",
    changeReason: "Ajuste semanal",
    createdAt: "2026-11-10T10:00:00Z",
    updatedAt: "2026-11-10T10:00:00Z",
    readOnly: false,
    deleted: false,
    createdBy: "coach-1",
    updatedBy: "coach-1",
    meals: [],
    ...overrides,
  };
}

describe("AthleteDietPage (T-095)", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getAthleteId).mockReturnValue("ath-1");
  });

  it("shows an alert when athleteId is missing", () => {
    vi.mocked(getAthleteId).mockReturnValue(null);
    renderPage();

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Não foi possível identificar sua conta. Entre novamente."
    );
  });

  it("renders empty state when backend returns 404 (no approved diet)", async () => {
    vi.mocked(apiClient.get).mockRejectedValueOnce({
      response: { status: 404 },
    });

    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText("Ainda não há uma dieta aprovada disponível.")
      ).toBeInTheDocument();
    });
  });

  it("renders 401 session expired error message", async () => {
    vi.mocked(apiClient.get).mockRejectedValueOnce({
      response: { status: 401 },
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(
        "Sua sessão expirou. Entre novamente para consultar sua dieta."
      );
    });
  });

  it("renders 403 forbidden error message", async () => {
    vi.mocked(apiClient.get).mockRejectedValueOnce({
      response: { status: 403 },
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByRole("alert")).toHaveTextContent(
        "Você não tem permissão para consultar este conteúdo."
      );
    });
  });

  it("renders overview macros and %kcal distribution via computeDerivedMacros", async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: makeDietResponse(),
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText("Hipertrofia")).toBeInTheDocument();
    });

    expect(screen.getByText("Plano alimentar · versão 2")).toBeInTheDocument();
    expect(screen.getByText("1.800 kcal")).toBeInTheDocument();
    expect(screen.getAllByText(/180 g/)).toHaveLength(2);
    expect(screen.getAllByText("(40% kcal)")).toHaveLength(2);
    expect(screen.getByText("(20% kcal)")).toBeInTheDocument();
  });

  it("renders meals sorted by order with time, RAW/AS_SERVED labels, householdMeasure, and preparation", async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: makeDietResponse({
        meals: [
          {
            id: "meal-2",
            name: "Almoço",
            order: 2,
            time: "12:30",
            notes: "Beber água após",
            target: null,
            totals: { calories: 650, protein: 50, carbohydrates: 70, fat: 15 },
            items: [
              {
                id: "item-2",
                foodId: "f-2",
                name: "Arroz branco",
                source: "TACO",
                kind: "FOOD",
                state: "AS_SERVED",
                quantityG: 150,
                householdMeasure: "4 colheres de sopa",
                preparation: "Cozido sem óleo",
                calories: 192,
                protein: 4,
                carbohydrates: 42,
                fat: 0.4,
              },
            ],
          },
          {
            id: "meal-1",
            name: "Café da manhã",
            order: 1,
            time: "07:30",
            notes: null,
            target: null,
            totals: { calories: 400, protein: 30, carbohydrates: 45, fat: 10 },
            items: [
              {
                id: "item-1",
                foodId: "f-1",
                name: "Aveia em flocos",
                source: "TACO",
                kind: "FOOD",
                state: "RAW",
                quantityG: 40,
                householdMeasure: "3 colheres de sopa",
                preparation: null,
                calories: 157,
                protein: 5.5,
                carbohydrates: 26,
                fat: 3.4,
              },
            ],
          },
        ],
      }),
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText("Refeições")).toBeInTheDocument();
    });

    const mealHeadings = screen.getAllByRole("heading", { level: 3 });
    expect(mealHeadings.map((h) => h.textContent)).toEqual([
      "Café da manhã",
      "Almoço",
    ]);

    const mealsSection = screen.getByRole("region", { name: "Refeições" });
    expect(within(mealsSection).getByText("07:30")).toBeInTheDocument();
    expect(within(mealsSection).getByText("12:30")).toBeInTheDocument();
    expect(within(mealsSection).getByText("Beber água após")).toBeInTheDocument();
    expect(within(mealsSection).getByText("3 colheres de sopa")).toBeInTheDocument();
    expect(
      within(mealsSection).getByText("4 colheres de sopa · Cozido sem óleo")
    ).toBeInTheDocument();

    expect(within(mealsSection).getByText("Cru")).toBeInTheDocument();
    expect(within(mealsSection).getByText("Como servido")).toBeInTheDocument();
  });

  it("renders dedicated Suplementos section for SUPPLEMENT_CALORIC items and aggregates Lista de compras", async () => {
    vi.mocked(apiClient.get).mockResolvedValueOnce({
      data: makeDietResponse({
        meals: [
          {
            id: "meal-1",
            name: "Café da manhã",
            order: 1,
            time: "07:30",
            notes: null,
            target: null,
            totals: null,
            items: [
              {
                id: "item-1",
                foodId: "f-1",
                name: "Frango grelhado",
                source: "TACO",
                kind: "FOOD",
                state: "RAW",
                quantityG: 120,
                householdMeasure: "1 filé médio",
                preparation: "Grelhado",
                calories: 190,
                protein: 36,
                carbohydrates: 0,
                fat: 4,
              },
              {
                id: "sup-1",
                foodId: null,
                name: "Whey Protein",
                source: "CUSTOM",
                kind: "SUPPLEMENT_CALORIC",
                state: "AS_SERVED",
                quantityG: 30,
                householdMeasure: "1 scoop",
                preparation: null,
                calories: 120,
                protein: 24,
                carbohydrates: 2,
                fat: 1.5,
              },
            ],
          },
          {
            id: "meal-2",
            name: "Jantar",
            order: 2,
            time: "20:00",
            notes: null,
            target: null,
            totals: null,
            items: [
              {
                id: "item-2",
                foodId: "f-1",
                name: "Frango grelhado",
                source: "TACO",
                kind: "FOOD",
                state: "RAW",
                quantityG: 180,
                householdMeasure: "1 filé grande",
                preparation: "Grelhado",
                calories: 285,
                protein: 54,
                carbohydrates: 0,
                fat: 6,
              },
            ],
          },
        ],
      }),
    });

    renderPage();

    await waitFor(() => {
      expect(
        screen.getByRole("region", { name: "Suplementos" })
      ).toBeInTheDocument();
    });

    const mealsSection = screen.getByRole("region", { name: "Refeições" });
    expect(within(mealsSection).queryByText("Whey Protein")).toBeNull();
    expect(within(mealsSection).getAllByText("Frango grelhado")).toHaveLength(2);

    const supplementsSection = screen.getByRole("region", {
      name: "Suplementos",
    });
    expect(within(supplementsSection).getByText("Whey Protein")).toBeInTheDocument();
    expect(within(supplementsSection).getByText("30 g")).toBeInTheDocument();
    expect(within(supplementsSection).getByText("Como servido")).toBeInTheDocument();
    expect(within(supplementsSection).getByText("1 scoop")).toBeInTheDocument();

    const shoppingSection = screen.getByRole("region", {
      name: "Lista de compras",
    });
    expect(within(shoppingSection).getByText("Frango grelhado")).toBeInTheDocument();
    expect(within(shoppingSection).getByText("300 g")).toBeInTheDocument();
    expect(within(shoppingSection).getByText("Cru")).toBeInTheDocument();
    expect(
      within(shoppingSection).getByText("1 filé médio · 1 filé grande")
    ).toBeInTheDocument();
  });
});
