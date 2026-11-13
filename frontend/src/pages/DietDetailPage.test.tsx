import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { DietDetailPage } from "./DietDetailPage";
import { apiClient } from "@/lib/api-client";

vi.mock("@/lib/api-client", () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

vi.mock("@/lib/auth", () => ({
  useAuth: () => ({
    user: { id: "coach-1", email: "coach@procoach.com", role: "COACH" },
    logout: vi.fn(),
  }),
}));

function renderPage(path = "/coach/athletes/ath-1/diets/diet-2") {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[path]}>
        <Routes>
          <Route path="/coach/athletes/:id/diets/:versionId" element={<DietDetailPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  );
}

const v1 = {
  id: "diet-1",
  coachId: "coach-1",
  athleteId: "ath-1",
  objective: "CUTTING",
  calories: 2200,
  protein: 160,
  carbohydrates: 240,
  fat: 65,
  status: "APPROVED",
  version: 1,
  previousVersionId: null,
  changeReason: null,
  createdAt: "2026-11-01T10:00:00Z",
  updatedAt: "2026-11-01T10:00:00Z",
  readOnly: true,
  deleted: false,
  createdBy: "11111111-1111-1111-1111-111111111111",
  updatedBy: "11111111-1111-1111-1111-111111111111",
  meals: [
    {
      id: "meal-1",
      name: "Café da manhã",
      order: 1,
      time: "08:00",
      notes: null,
      target: null,
      totals: { calories: 500, protein: 30, carbohydrates: 60, fat: 15 },
      items: [
        {
          id: "item-1",
          foodId: "f-1",
          name: "Aveia",
          source: "TACO",
          kind: "FOOD",
          state: "RAW",
          quantityG: 60,
          householdMeasure: "4 colheres de sopa",
          preparation: null,
          calories: 220,
          protein: 8,
          carbohydrates: 38,
          fat: 4,
        },
      ],
    },
  ],
};

const v2 = {
  ...v1,
  id: "diet-2",
  calories: 2000,
  protein: 160,
  carbohydrates: 200,
  fat: 60,
  status: "DRAFT",
  version: 2,
  previousVersionId: "diet-1",
  changeReason: "Redução de carboidratos",
  readOnly: false,
  meals: [
    {
      id: "meal-2",
      name: "Almoço",
      order: 2,
      time: "12:30",
      notes: "Pós-treino",
      target: null,
      totals: { calories: 700, protein: 50, carbohydrates: 80, fat: 20 },
      items: [
        {
          id: "item-2",
          foodId: null,
          name: "Whey Protein",
          source: "CUSTOM",
          kind: "SUPPLEMENT_CALORIC",
          state: "AS_SERVED",
          quantityG: 30,
          householdMeasure: "1 scoop",
          preparation: "Diluído em água",
          calories: 120,
          protein: 24,
          carbohydrates: 3,
          fat: 1,
        },
      ],
    },
    {
      id: "meal-1",
      name: "Café da manhã",
      order: 1,
      time: "08:00",
      notes: null,
      target: null,
      totals: { calories: 500, protein: 30, carbohydrates: 60, fat: 15 },
      items: [
        {
          id: "item-1",
          foodId: "f-1",
          name: "Aveia",
          source: "TACO",
          kind: "FOOD",
          state: "RAW",
          quantityG: 80,
          householdMeasure: "5 colheres de sopa",
          preparation: null,
          calories: 290,
          protein: 10,
          carbohydrates: 50,
          fat: 5,
        },
      ],
    },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("DietDetailPage", () => {
  it("renders read-only detail with sorted meals, items, derived macros, audit, and version diff", async () => {
    vi.mocked(apiClient.get).mockImplementation((url) => {
      if (url === "/athletes/ath-1") {
        return Promise.resolve({
          data: {
            id: "ath-1",
            name: "Maria Souza",
            email: "maria@procoach.com",
            sex: "FEMALE",
            birthDate: "1995-05-10",
            height: 165,
            weight: 60,
            goal: "CUTTING",
            status: "ACTIVE",
            createdAt: "2026-10-01T00:00:00Z",
            hasCredential: true,
            pendingInvite: null,
          },
        });
      }
      if (url === "/diets/diet-2") {
        return Promise.resolve({ data: v2 });
      }
      if (url === "/diets/history") {
        return Promise.resolve({ data: [v1, v2] });
      }
      if (url === "/assessments") {
        return Promise.resolve({
          data: [
            {
              id: "ass-1",
              athleteId: "ath-1",
              assessmentDate: "2026-11-01",
              weightKg: 60,
              heightCm: 165,
              bodyFatPercentage: null,
              muscleMassKg: null,
              waistCm: null,
              hipCm: null,
              createdAt: "2026-11-01T10:00:00Z",
              metrics: {
                imc: 22.0,
                tmbKcal: 1350,
                getKcal: 1900,
                weightChangeKg: null,
                weightChangePercent: null,
              },
            },
          ],
        });
      }
      if (url === "/diets/diet-2/energy-balance") {
        return Promise.resolve({
          data: {
            dietId: "diet-2",
            calories: 2000,
            tmbKcal: 1350,
            getKcal: 1900,
            balanceKcal: 100,
            balancePercent: 5.3,
            classification: "MAINTENANCE",
            basedOnAssessmentDate: "2026-11-01",
            weightKg: 60,
          },
        });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /Maria Souza — Dieta v2/i })).toBeInTheDocument();
    });

    // Manage meals link visible for DRAFT & !readOnly
    const manageMealsLink = screen.getByRole("link", { name: "Gerenciar refeições" });
    expect(manageMealsLink).toHaveAttribute("href", "/coach/athletes/ath-1/diets/diet-2/meals");

    // Meals sorted by order: 1. Café da manhã before 2. Almoço
    const mealHeadings = screen.getAllByRole("heading", { level: 3 });
    expect(mealHeadings[0]).toHaveTextContent("1. Café da manhã");
    expect(mealHeadings[1]).toHaveTextContent("2. Almoço");

    // Item metadata (FOOD vs SUPPLEMENT_CALORIC, RAW/AS_SERVED, householdMeasure, preparation)
    expect(screen.getByText("Alimento")).toBeInTheDocument();
    expect(screen.getByText("· Cru")).toBeInTheDocument();
    expect(screen.getByText("Suplemento calórico")).toBeInTheDocument();
    expect(screen.getByText("· Como servido")).toBeInTheDocument();
    expect(screen.getByText(/Diluído em água/)).toBeInTheDocument();
    expect(screen.getByText(/1 scoop/)).toBeInTheDocument();

    // Version diff section
    expect(await screen.findByText(/Comparação com v1 → v2/)).toBeInTheDocument();
    expect(screen.getByText("-200 kcal")).toBeInTheDocument();
    expect(screen.getAllByText("Adicionado").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("Alterado").length).toBeGreaterThanOrEqual(1);

    // Audit info
    expect(screen.getByText("Redução de carboidratos")).toBeInTheDocument();
    expect(screen.getAllByText("11111111-1111-1111-1111-111111111111").length).toBe(2);
  });
});
