import { MuscleGroup, PlanContentChange, TrainingStatus } from "@/lib/training";

export const MUSCLE_LABELS: Record<MuscleGroup, string> = {
  PEITO: "Peito",
  COSTAS: "Costas",
  OMBROS: "Ombros",
  BICEPS: "Bíceps",
  TRICEPS: "Tríceps",
  ANTEBRACO: "Antebraço",
  QUADRICEPS: "Quadríceps",
  POSTERIOR_DE_COXA: "Posterior de coxa",
  GLUTEOS: "Glúteos",
  ADUTORES: "Adutores",
  ABDUTORES: "Abdutores",
  PANTURRILHA: "Panturrilha",
  ABDOMEN: "Abdômen",
  LOMBAR: "Lombar",
  TRAPECIO: "Trapézio",
  FOREARM: "Antebraço",
};

export const EQUIPMENT_LABELS: Record<string, string> = {
  BARRA_LIVRE: "Barra livre",
  HALTERES: "Halteres",
  MAQUINA: "Máquina",
  CROSSOVER: "Crossover",
  POLIA: "Polia",
  BANCO: "Banco",
  PESO_CORPORAL: "Peso corporal",
  KETTLEBELL: "Kettlebell",
  BOLA_SUICA: "Bola suíça",
  ELASTICO: "Elástico",
  SMITH: "Smith",
  OUTRO: "Outro",
};

export const INTENSITY_OPTIONS = [
  { value: "RPE", label: "RPE" },
  { value: "RIR", label: "RIR" },
  { value: "PERCENT_1RM", label: "% de 1RM" },
  { value: "ABSOLUTE_LOAD", label: "Carga absoluta" },
];

export const DAY_OPTIONS = [
  { value: "1", label: "Segunda-feira" },
  { value: "2", label: "Terça-feira" },
  { value: "3", label: "Quarta-feira" },
  { value: "4", label: "Quinta-feira" },
  { value: "5", label: "Sexta-feira" },
  { value: "6", label: "Sábado" },
  { value: "7", label: "Domingo" },
];

export const STATUS_NEXT: Partial<Record<TrainingStatus, TrainingStatus>> = {
  DRAFT: "IN_REVIEW",
  IN_REVIEW: "ACTIVE",
  ACTIVE: "FINISHED",
};

export const STATUS_ACTION: Record<TrainingStatus, string> = {
  DRAFT: "Enviar para revisão",
  IN_REVIEW: "Ativar",
  ACTIVE: "Finalizar",
  FINISHED: "Finalizado",
};

export const PLAN_STATUS_ACTION: Record<TrainingStatus, string> = {
  DRAFT: "Enviar para revisão",
  IN_REVIEW: "Publicar versão",
  ACTIVE: "Finalizar versão",
  FINISHED: "Finalizada",
};

export const CHANGE_LABEL: Record<PlanContentChange, string> = {
  ADDED: "Nova",
  REMOVED: "Removida",
  CHANGED: "Alterada",
  UNCHANGED: "Sem mudanças",
};

export const DAY_LABELS = Object.fromEntries(DAY_OPTIONS.map((day) => [day.value, day.label]));

export function formatDate(date: string | null) {
  if (!date) return "Data não informada";
  return new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium", timeZone: "UTC" }).format(
    new Date(`${date}T00:00:00Z`)
  );
}

export function movePrescription(ids: string[], index: number, offset: number) {
  const next = [...ids];
  const targetIndex = index + offset;
  if (targetIndex < 0 || targetIndex >= next.length) return next;
  [next[index], next[targetIndex]] = [next[targetIndex], next[index]];
  return next;
}

export function errorMessage(error: unknown, fallback: string) {
  if (error && typeof error === "object" && "response" in error) {
    const status = (error as { response?: { status?: number } }).response?.status;
    if (status === 401) return "Sua sessão expirou. Entre novamente para continuar.";
    if (status === 403) return "Você não tem acesso a este conteúdo.";
    if (status === 409) return "O estado mudou no servidor. Atualize os dados e tente novamente.";
  }
  return fallback;
}
