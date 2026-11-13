import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { MemoryRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { AthleteDietsPanel } from "./AthleteDietsPanel";
import { apiClient } from "@/lib/api-client";

vi.mock("@/lib/api-client", () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

function renderPanel(athleteId = "ath-1") {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter>
        <AthleteDietsPanel athleteId={athleteId} />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

const baseDiet = {
  id: "diet-2",
  coachId: "coach-1",
  athleteId: "ath-1",
  objective: "CUTTING",
  calories: 2000,
  protein: 160,
  carbohydrates: 200,
  fat: 60,
  status: "DRAFT",
  version: 2,
  previousVersionId: "diet-1",
  changeReason: "Ajuste de corte",
  createdAt: "2026-11-10T10:00:00Z",
  updatedAt: "2026-11-10T10:00:00Z",
  readOnly: false,
  deleted: false,
  createdBy: "11111111-1111-1111-1111-111111111111",
  updatedBy: "11111111-1111-1111-1111-111111111111",
  meals: [],
};

const frozenV1 = {
  ...baseDiet,
  id: "diet-1",
  version: 1,
  status: "APPROVED",
  previousVersionId: null,
  changeReason: null,
  readOnly: true,
};

beforeEach(() => {
  vi.clearAllMocks();
});

describe("AthleteDietsPanel", () => {
  it("displays full macros, derived metrics with weightKg, energy balance, and action links", async () => {
    vi.mocked(apiClient.get).mockImplementation((url) => {
      if (url === "/diets/history") {
        return Promise.resolve({ data: [frozenV1, baseDiet] });
      }
      if (url === "/assessments") {
        return Promise.resolve({
          data: [
            {
              id: "ass-1",
              athleteId: "ath-1",
              assessmentDate: "2026-11-01",
              weightKg: 80,
              heightCm: 175,
              bodyFatPercentage: 15,
              muscleMassKg: null,
              waistCm: null,
              hipCm: null,
              createdAt: "2026-11-01T10:00:00Z",
              metrics: {
                imc: 26.1,
                tmbKcal: 1750,
                getKcal: 2400,
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
            tmbKcal: 1750,
            getKcal: 2400,
            balanceKcal: -400,
            balancePercent: -16.7,
            classification: "DEFICIT",
            basedOnAssessmentDate: "2026-11-01",
            weightKg: 80,
          },
        });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    renderPanel();

    await waitFor(() => {
      expect(screen.getByLabelText("Macronutrientes da versão atual")).toBeInTheDocument();
    });

    // Derived metrics: 2000 / 80 = 25 kcal/kg, 160 / 80 = 2 g/kg
    expect(await screen.findByText(/25 kcal\/kg/)).toBeInTheDocument();
    expect(screen.getByText(/2 g\/kg/)).toBeInTheDocument();

    // Energy balance (loaded in follow-up query after current.id resolves)
    expect(await screen.findByText("Déficit")).toBeInTheDocument();
    expect(screen.getByText(/2\.400 kcal/)).toBeInTheDocument();
    expect(screen.getByText(/-400 kcal/)).toBeInTheDocument();

    // Links: Ver detalhes & Gerenciar refeições (only v2 is DRAFT & !readOnly)
    const detailLinks = screen.getAllByRole("link", { name: "Ver detalhes" });
    expect(detailLinks.length).toBeGreaterThanOrEqual(2);
    expect(detailLinks[0]).toHaveAttribute("href", "/coach/athletes/ath-1/diets/diet-2");

    const mealLinks = screen.getAllByRole("link", { name: "Gerenciar refeições" });
    expect(mealLinks.length).toBe(2); // current card + v2 row (v1 is readOnly APPROVED)
    expect(mealLinks[0]).toHaveAttribute("href", "/coach/athletes/ath-1/diets/diet-2/meals");
  });

  it("shows non-blocking nutritional warnings and 409 assessment CTA", async () => {
    const inconsistentDiet = {
      ...baseDiet,
      calories: 3000, // 4/4/9 sum of 100P, 100C, 0F = 800 kcal -> huge inconsistency + zero fat
      protein: 100,
      carbohydrates: 100,
      fat: 0,
    };

    vi.mocked(apiClient.get).mockImplementation((url) => {
      if (url === "/diets/history") {
        return Promise.resolve({ data: [inconsistentDiet] });
      }
      if (url === "/assessments") {
        return Promise.resolve({ data: [] });
      }
      if (url === "/diets/diet-2/energy-balance") {
        return Promise.reject({ response: { status: 409 } });
      }
      return Promise.reject(new Error(`Unexpected URL: ${url}`));
    });

    renderPanel();

    await waitFor(() => {
      expect(screen.getByLabelText("Avisos nutricionais")).toBeInTheDocument();
    });

    expect(screen.getByText(/diverge das calorias informadas/i)).toBeInTheDocument();
    expect(screen.getByText(/macronutriente\(s\) zerado\(s\): Gorduras/i)).toBeInTheDocument();

    const cta = await screen.findByRole("link", { name: "Registrar avaliação" });
    expect(cta).toHaveAttribute("href", "/coach/athletes/ath-1/assessments/new");
  });
});
