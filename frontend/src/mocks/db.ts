import type {
  AthleteStatus,
  Goal,
  PendingInviteSummary,
  Sex,
} from "@/lib/athletes";
import type { AthleteInvite } from "@/lib/invites";
import type { AssessmentMetrics } from "@/lib/assessments";
import type { DietObjective, DietStatus } from "@/lib/diets";
import type {
  Exercise,
  Periodization,
  TrainingPlan,
  TrainingSession,
} from "@/lib/training";
import type {
  AthleteNotification,
  DevicePlatform,
  NotificationPreferences,
} from "@/lib/notifications";

export type MockCoach = {
  id: string;
  name: string;
  email: string;
};

export type MockApiAthlete = {
  id: string;
  name: string;
  email: string | null;
  sex: Sex;
  birthDate: string;
  height: number;
  weight: number;
  goal: Goal;
  status: AthleteStatus;
  createdAt: string;
  hasCredential: boolean;
  pendingInvite: PendingInviteSummary | null;
};

export type MockInviteRecord = AthleteInvite & {
  token: string;
};

export type MockApiAssessment = {
  id: string;
  athleteId: string;
  assessmentDate: string;
  weightKg: number;
  heightCm: number;
  bodyFatPercentage: number | null;
  muscleMassKg: number | null;
  waistCm: number | null;
  hipCm: number | null;
  createdAt: string;
  metrics: AssessmentMetrics;
};

export type MockApiDiet = {
  id: string;
  coachId: string;
  athleteId: string;
  objective: DietObjective;
  calories: number;
  protein: number;
  carbohydrates: number;
  fat: number;
  status: DietStatus;
  version: number;
  previousVersionId: string | null;
  changeReason: string | null;
  createdAt: string;
  updatedAt: string;
  readOnly: boolean;
  deleted: boolean;
  createdBy: string;
  updatedBy: string;
};

export type MockMealItem = {
  id: string;
  name: string;
  quantity: string;
  calories: number;
  protein: number;
  carbohydrates: number;
  fat: number;
};

export type MockMeal = {
  id: string;
  dietId: string;
  name: string;
  time: string;
  orderIndex: number;
  items: MockMealItem[];
};

export type MockDeviceRecord = {
  athleteId: string;
  token: string;
  platform: DevicePlatform;
};

export type MockDbState = {
  coach: MockCoach;
  athletes: MockApiAthlete[];
  invites: MockInviteRecord[];
  assessments: MockApiAssessment[];
  diets: MockApiDiet[];
  meals: MockMeal[];
  exercises: Exercise[];
  periodizations: Periodization[];
  plans: TrainingPlan[];
  sessions: TrainingSession[];
  notifications: Record<string, AthleteNotification[]>;
  notificationPreferences: Record<string, NotificationPreferences>;
  devices: MockDeviceRecord[];
};

export const MOCK_COACH_CREDENTIALS = {
  id: "11111111-1111-4111-8111-111111111111",
  name: "Vinícius Coach",
  email: "coach@procoach.app",
};

export const MOCK_ATHLETE_CREDENTIALS = {
  athleteId: "22222222-2222-4222-8222-222222222201",
  name: "Lucas Silva",
  email: "lucas@procoach.app",
};

let idCounter = 100;

export function nextMockId(prefix = "mock"): string {
  idCounter += 1;
  return `${prefix}-${idCounter.toString().padStart(4, "0")}`;
}

function createInitialState(): MockDbState {
  const coachId = MOCK_COACH_CREDENTIALS.id;
  const lucasId = MOCK_ATHLETE_CREDENTIALS.athleteId;
  const marianaId = "22222222-2222-4222-8222-222222222202";
  const rafaelId = "22222222-2222-4222-8222-222222222203";

  const futureExpiry = new Date(Date.now() + 5 * 24 * 60 * 60 * 1000).toISOString();

  return {
    coach: { ...MOCK_COACH_CREDENTIALS },
    athletes: [
      {
        id: lucasId,
        name: "Lucas Silva",
        email: "lucas@procoach.app",
        sex: "MALE",
        birthDate: "1995-04-12",
        height: 178,
        weight: 82.5,
        goal: "HYPERTROPHY",
        status: "ACTIVE",
        createdAt: "2026-08-01T10:00:00.000Z",
        hasCredential: true,
        pendingInvite: null,
      },
      {
        id: marianaId,
        name: "Mariana Costa",
        email: "mariana@procoach.app",
        sex: "FEMALE",
        birthDate: "1998-09-23",
        height: 165,
        weight: 63.0,
        goal: "CUTTING",
        status: "ACTIVE",
        createdAt: "2026-09-10T14:30:00.000Z",
        hasCredential: false,
        pendingInvite: {
          id: "inv-mariana-1",
          email: "mariana@procoach.app",
          expiresAt: futureExpiry,
        },
      },
      {
        id: rafaelId,
        name: "Rafael Mendes",
        email: "rafael@procoach.app",
        sex: "MALE",
        birthDate: "1991-01-15",
        height: 182,
        weight: 90.0,
        goal: "RECOMPOSITION",
        status: "PAUSED",
        createdAt: "2026-07-15T09:00:00.000Z",
        hasCredential: false,
        pendingInvite: null,
      },
    ],
    invites: [
      {
        id: "inv-mariana-1",
        athleteId: marianaId,
        email: "mariana@procoach.app",
        expiresAt: futureExpiry,
        createdAt: "2026-11-10T12:00:00.000Z",
        status: "PENDING",
        token: "mock-invite-mariana",
      },
    ],
    assessments: [
      {
        id: "assess-lucas-1",
        athleteId: lucasId,
        assessmentDate: "2026-09-01",
        weightKg: 80.0,
        heightCm: 178,
        bodyFatPercentage: 15.2,
        muscleMassKg: 38.4,
        waistCm: 83.0,
        hipCm: 99.0,
        createdAt: "2026-09-01T11:00:00.000Z",
        metrics: {
          imc: 25.25,
          tmbKcal: 1768,
          getKcal: 2122,
          weightChangeKg: null,
          weightChangePercent: null,
        },
      },
      {
        id: "assess-lucas-2",
        athleteId: lucasId,
        assessmentDate: "2026-10-15",
        weightKg: 82.5,
        heightCm: 178,
        bodyFatPercentage: 14.5,
        muscleMassKg: 40.1,
        waistCm: 82.0,
        hipCm: 100.0,
        createdAt: "2026-10-15T11:00:00.000Z",
        metrics: {
          imc: 26.04,
          tmbKcal: 1793,
          getKcal: 2152,
          weightChangeKg: 2.5,
          weightChangePercent: 3.13,
        },
      },
    ],
    diets: [
      {
        id: "diet-lucas-v1",
        coachId,
        athleteId: lucasId,
        objective: "HYPERTROPHY",
        calories: 2700,
        protein: 185,
        carbohydrates: 330,
        fat: 70,
        status: "ARCHIVED",
        version: 1,
        previousVersionId: null,
        changeReason: null,
        createdAt: "2026-09-02T10:00:00.000Z",
        updatedAt: "2026-10-16T09:00:00.000Z",
        readOnly: true,
        deleted: false,
        createdBy: coachId,
        updatedBy: coachId,
      },
      {
        id: "diet-lucas-v2",
        coachId,
        athleteId: lucasId,
        objective: "HYPERTROPHY",
        calories: 2950,
        protein: 200,
        carbohydrates: 375,
        fat: 72,
        status: "APPROVED",
        version: 2,
        previousVersionId: "diet-lucas-v1",
        changeReason: "Ajuste calórico após ganho de massa magra na avaliação de outubro",
        createdAt: "2026-10-16T09:15:00.000Z",
        updatedAt: "2026-10-16T09:30:00.000Z",
        readOnly: false,
        deleted: false,
        createdBy: coachId,
        updatedBy: coachId,
      },
      {
        id: "diet-mariana-v1",
        coachId,
        athleteId: marianaId,
        objective: "CUTTING",
        calories: 1750,
        protein: 140,
        carbohydrates: 170,
        fat: 55,
        status: "DRAFT",
        version: 1,
        previousVersionId: null,
        changeReason: null,
        createdAt: "2026-11-01T15:00:00.000Z",
        updatedAt: "2026-11-01T15:00:00.000Z",
        readOnly: false,
        deleted: false,
        createdBy: coachId,
        updatedBy: coachId,
      },
    ],
    meals: [
      {
        id: "meal-1",
        dietId: "diet-lucas-v2",
        name: "Café da manhã",
        time: "07:30",
        orderIndex: 1,
        items: [
          {
            id: "item-1",
            name: "Ovos mexidos",
            quantity: "4 unidades",
            calories: 280,
            protein: 24,
            carbohydrates: 2,
            fat: 20,
          },
          {
            id: "item-2",
            name: "Aveia em flocos",
            quantity: "60 g",
            calories: 230,
            protein: 8,
            carbohydrates: 40,
            fat: 4,
          },
        ],
      },
      {
        id: "meal-2",
        dietId: "diet-lucas-v2",
        name: "Almoço",
        time: "12:30",
        orderIndex: 2,
        items: [
          {
            id: "item-3",
            name: "Peito de frango grelhado",
            quantity: "180 g",
            calories: 295,
            protein: 55,
            carbohydrates: 0,
            fat: 6,
          },
          {
            id: "item-4",
            name: "Arroz branco cozido",
            quantity: "250 g",
            calories: 320,
            protein: 6,
            carbohydrates: 70,
            fat: 1,
          },
        ],
      },
    ],
    exercises: [
      {
        id: "ex-bench-v1",
        name: "Supino Reto com Barra (v1)",
        muscleGroups: ["PEITO", "TRICEPS", "OMBROS"],
        equipment: "Barra e Banco Reto",
        execution: "Desça a barra controladamente até tocar a linha média do peitoral.",
        commonErrors: ["Abrir excessivamente os cotovelos"],
        progressions: ["Supino com pausa"],
        regressions: ["Supino com halteres"],
        contraindications: ["Dor aguda no ombro"],
        references: [
          {
            authors: "Schoenfeld BJ",
            title: "Biomechanics of Chest Training",
            source: "Strength & Conditioning Journal",
            year: 2020,
          },
        ],
        version: 1,
        superseded: true,
      },
      {
        id: "ex-bench-v2",
        name: "Supino Reto com Barra",
        muscleGroups: ["PEITO", "TRICEPS", "OMBROS"],
        equipment: "Barra e Banco Reto",
        execution: "Mantenha escápulas retraídas e desça a barra até o esterno.",
        commonErrors: ["Tirar o quadril do banco", "Abrir cotovelos a 90 graus"],
        progressions: ["Supino com pausa de 2s"],
        regressions: ["Flexão de braços"],
        contraindications: ["Lesão aguda de manguito rotador"],
        references: [
          {
            authors: "Schoenfeld BJ",
            title: "Resistance Training Mechanics",
            source: "JSCR",
            year: 2022,
          },
        ],
        version: 2,
        superseded: false,
      },
      {
        id: "ex-squat",
        name: "Agachamento Livre",
        muscleGroups: ["QUADRICEPS", "GLUTEOS", "LOMBAR"],
        equipment: "Barra e Rack",
        execution: "Agache mantendo o tronco firme e joelhos alinhados aos pés.",
        commonErrors: ["Valgo dinâmico", "Perda de curvatura lombar"],
        progressions: ["Agachamento com pausa"],
        regressions: ["Goblet Squat"],
        contraindications: ["Dor lombar aguda"],
        references: [],
        version: 1,
        superseded: false,
      },
      {
        id: "ex-pulldown",
        name: "Puxada Frontal Aberta",
        muscleGroups: ["COSTAS", "BICEPS"],
        equipment: "Polia Alta",
        execution: "Puxe a barra em direção à parte superior do peito mantendo peito aberto.",
        commonErrors: ["Balançar o tronco para trás"],
        progressions: ["Barra fixa pronada"],
        regressions: ["Puxada com elástico"],
        contraindications: [],
        references: [],
        version: 1,
        superseded: false,
      },
      {
        id: "ex-rdl",
        name: "Levantamento Terra Romeno",
        muscleGroups: ["POSTERIOR_DE_COXA", "GLUTEOS", "LOMBAR"],
        equipment: "Barra",
        execution: "Empurre o quadril para trás mantendo a barra rente às pernas.",
        commonErrors: ["Flexionar a coluna lombar"],
        progressions: ["RDL unilateral"],
        regressions: ["Ponte de glúteos"],
        contraindications: ["Hérnia discal sintomática"],
        references: [],
        version: 1,
        superseded: false,
      },
      {
        id: "ex-ohp",
        name: "Desenvolvimento com Halteres",
        muscleGroups: ["OMBROS", "TRICEPS"],
        equipment: "Halteres e Banco",
        execution: "Empurre os halteres acima da cabeça sem hiperestender a lombar.",
        commonErrors: ["Arquear excessivamente as costas"],
        progressions: ["Desenvolvimento em pé com barra"],
        regressions: ["Elevação lateral"],
        contraindications: ["Impacto subacromial agudo"],
        references: [],
        version: 1,
        superseded: false,
      },
    ],
    periodizations: [
      {
        id: "per-lucas-1",
        athleteId: lucasId,
        name: "Mesociclo Hipertrofia Q4",
        description: "Bloco de 8 semanas com foco em progressão de carga.",
        startDate: "2026-10-01",
        endDate: "2026-11-26",
        status: "ACTIVE",
        deleted: false,
      },
    ],
    plans: [
      {
        id: "plan-lucas-v1",
        athleteId: lucasId,
        periodizationId: "per-lucas-1",
        name: "Upper / Lower Base",
        description: "Versão inicial de adaptação.",
        version: 1,
        previousVersionId: null,
        status: "FINISHED",
        createdAt: "2026-10-01T09:00:00.000Z",
        updatedAt: "2026-10-20T09:00:00.000Z",
        deleted: false,
      },
      {
        id: "plan-lucas-v2",
        athleteId: lucasId,
        periodizationId: "per-lucas-1",
        name: "Upper / Lower Progressão",
        description: "Aumento de volume em peito e posteriores.",
        version: 2,
        previousVersionId: "plan-lucas-v1",
        status: "ACTIVE",
        createdAt: "2026-10-21T10:00:00.000Z",
        updatedAt: "2026-10-21T10:30:00.000Z",
        deleted: false,
      },
    ],
    sessions: [
      {
        id: "sess-lucas-a",
        periodizationId: "per-lucas-1",
        planId: "plan-lucas-v2",
        athleteId: lucasId,
        name: "Treino A — Superiores (Empurrar)",
        description: "Foco em peitoral e ombros",
        weekNumber: 1,
        dayOfWeek: 1,
        status: "ACTIVE",
        deleted: false,
        exercises: [
          {
            id: "presc-1",
            exerciseId: "ex-bench-v1",
            exerciseName: "Supino Reto com Barra (v1)",
            customExercise: false,
            orderIndex: 1,
            sets: 4,
            repetitions: 8,
            load: 80,
            intensityType: "RPE",
            intensityValue: 8,
            restSeconds: 120,
            notes: "Controlar excêntrica em 2s",
            exerciseVersion: 1,
            newerVersionAvailable: true,
            currentExerciseId: "ex-bench-v2",
          },
          {
            id: "presc-2",
            exerciseId: "ex-ohp",
            exerciseName: "Desenvolvimento com Halteres",
            customExercise: false,
            orderIndex: 2,
            sets: 3,
            repetitions: 10,
            load: 22,
            intensityType: "RIR",
            intensityValue: 2,
            restSeconds: 90,
            notes: null,
            exerciseVersion: 1,
            newerVersionAvailable: false,
            currentExerciseId: null,
          },
        ],
      },
      {
        id: "sess-lucas-b",
        periodizationId: "per-lucas-1",
        planId: "plan-lucas-v2",
        athleteId: lucasId,
        name: "Treino B — Inferiores",
        description: "Foco em quadríceps e posteriores",
        weekNumber: 1,
        dayOfWeek: 3,
        status: "ACTIVE",
        deleted: false,
        exercises: [
          {
            id: "presc-3",
            exerciseId: "ex-squat",
            exerciseName: "Agachamento Livre",
            customExercise: false,
            orderIndex: 1,
            sets: 4,
            repetitions: 6,
            load: 110,
            intensityType: "RPE",
            intensityValue: 8.5,
            restSeconds: 150,
            notes: null,
            exerciseVersion: 1,
            newerVersionAvailable: false,
            currentExerciseId: null,
          },
          {
            id: "presc-4",
            exerciseId: "ex-rdl",
            exerciseName: "Levantamento Terra Romeno",
            customExercise: false,
            orderIndex: 2,
            sets: 3,
            repetitions: 10,
            load: 90,
            intensityType: "RIR",
            intensityValue: 2,
            restSeconds: 120,
            notes: null,
            exerciseVersion: 1,
            newerVersionAvailable: false,
            currentExerciseId: null,
          },
        ],
      },
    ],
    notifications: {
      [lucasId]: [
        {
          id: "notif-1",
          type: "DIET_APPROVED",
          message: "Sua dieta v2 (Hipertrofia · 2.950 kcal) foi aprovada pelo coach.",
          referenceType: "DIET",
          referenceId: "diet-lucas-v2",
          createdAt: "2026-10-16T09:35:00.000Z",
          read: false,
          readAt: null,
        },
        {
          id: "notif-2",
          type: "TRAINING_PLAN_ACTIVE",
          message: "Novo plano de treino 'Upper / Lower Progressão' liberado.",
          referenceType: "TRAINING_PLAN",
          referenceId: "plan-lucas-v2",
          createdAt: "2026-10-21T10:35:00.000Z",
          read: false,
          readAt: null,
        },
        {
          id: "notif-3",
          type: "ASSESSMENT_CREATED",
          message: "Nova avaliação física registrada em 15/10/2026.",
          referenceType: "ASSESSMENT",
          referenceId: "assess-lucas-2",
          createdAt: "2026-10-15T11:05:00.000Z",
          read: true,
          readAt: "2026-10-15T12:00:00.000Z",
        },
      ],
    },
    notificationPreferences: {
      [lucasId]: { emailEnabled: true, pushEnabled: false },
    },
    devices: [],
  };
}

export let mockDb: MockDbState = createInitialState();

export function resetMockDb(): MockDbState {
  idCounter = 100;
  mockDb = createInitialState();
  return mockDb;
}
