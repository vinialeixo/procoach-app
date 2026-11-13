import { http, HttpResponse } from "msw";
import { mockDb, nextMockId, type MockApiAssessment, type MockApiAthlete } from "../db";

type CreateAssessmentBody = {
  athleteId: string;
  assessmentDate: string;
  weightKg: number;
  heightCm: number;
  bodyFatPercentage?: number | null;
  muscleMassKg?: number | null;
  waistCm?: number | null;
  hipCm?: number | null;
};

function calculateAge(birthDate: string, referenceDate: string): number {
  const birth = new Date(birthDate);
  const ref = new Date(referenceDate);
  let age = ref.getFullYear() - birth.getFullYear();
  const monthDiff = ref.getMonth() - birth.getMonth();
  if (monthDiff < 0 || (monthDiff === 0 && ref.getDate() < birth.getDate())) {
    age -= 1;
  }
  return Math.max(age, 15);
}

function computeMetrics(
  athlete: MockApiAthlete,
  weightKg: number,
  heightCm: number,
  assessmentDate: string,
  previousWeight: number | null
) {
  const heightM = heightCm / 100;
  const imc = Number((weightKg / (heightM * heightM)).toFixed(2));
  const age = calculateAge(athlete.birthDate, assessmentDate);
  const sexOffset = athlete.sex === "MALE" ? 5 : -161;
  const tmbKcal = Math.round(10 * weightKg + 6.25 * heightCm - 5 * age + sexOffset);
  const getKcal = Math.round(tmbKcal * 1.2);

  if (previousWeight === null || previousWeight === 0) {
    return {
      imc,
      tmbKcal,
      getKcal,
      weightChangeKg: null,
      weightChangePercent: null,
    };
  }

  const weightChangeKg = Number((weightKg - previousWeight).toFixed(2));
  const weightChangePercent = Number(((weightChangeKg / previousWeight) * 100).toFixed(2));

  return {
    imc,
    tmbKcal,
    getKcal,
    weightChangeKg,
    weightChangePercent,
  };
}

export const assessmentsHandlers = [
  http.get("/api/v1/assessments", ({ request }) => {
    const url = new URL(request.url);
    const athleteId = url.searchParams.get("athleteId");
    const list = mockDb.assessments
      .filter((item) => !athleteId || item.athleteId === athleteId)
      .slice()
      .sort((a, b) => b.assessmentDate.localeCompare(a.assessmentDate));
    return HttpResponse.json(list);
  }),

  http.get("/api/v1/assessments/compare", ({ request }) => {
    const url = new URL(request.url);
    const athleteId = url.searchParams.get("athleteId");
    const list = mockDb.assessments
      .filter((item) => !athleteId || item.athleteId === athleteId)
      .slice()
      .sort((a, b) => b.assessmentDate.localeCompare(a.assessmentDate));

    if (list.length < 2) {
      return HttpResponse.json(
        { message: "São necessárias pelo menos duas avaliações para comparação." },
        { status: 404 }
      );
    }

    const [current, previous] = list;
    return HttpResponse.json({
      previous,
      current,
      deltas: {
        weightKg: Number((current.weightKg - previous.weightKg).toFixed(2)),
        bodyFatPercentage:
          current.bodyFatPercentage !== null && previous.bodyFatPercentage !== null
            ? Number((current.bodyFatPercentage - previous.bodyFatPercentage).toFixed(2))
            : null,
        muscleMassKg:
          current.muscleMassKg !== null && previous.muscleMassKg !== null
            ? Number((current.muscleMassKg - previous.muscleMassKg).toFixed(2))
            : null,
      },
    });
  }),

  http.post("/api/v1/assessments", async ({ request }) => {
    const body = (await request.json()) as CreateAssessmentBody;
    const athlete = mockDb.athletes.find((item) => item.id === body.athleteId);

    if (!athlete) {
      return HttpResponse.json({ message: "Atleta não encontrado." }, { status: 404 });
    }

    const existing = mockDb.assessments
      .filter((item) => item.athleteId === body.athleteId)
      .slice()
      .sort((a, b) => b.assessmentDate.localeCompare(a.assessmentDate));
    const previousWeight = existing[0]?.weightKg ?? null;

    const created: MockApiAssessment = {
      id: nextMockId("assess"),
      athleteId: body.athleteId,
      assessmentDate: body.assessmentDate,
      weightKg: body.weightKg,
      heightCm: body.heightCm,
      bodyFatPercentage: body.bodyFatPercentage ?? null,
      muscleMassKg: body.muscleMassKg ?? null,
      waistCm: body.waistCm ?? null,
      hipCm: body.hipCm ?? null,
      createdAt: new Date().toISOString(),
      metrics: computeMetrics(
        athlete,
        body.weightKg,
        body.heightCm,
        body.assessmentDate,
        previousWeight
      ),
    };

    mockDb.assessments.push(created);
    athlete.weight = body.weightKg;
    athlete.height = body.heightCm;

    return HttpResponse.json(created, { status: 201 });
  }),
];
