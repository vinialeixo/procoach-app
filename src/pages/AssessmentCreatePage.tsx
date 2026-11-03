import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate, useParams, Link } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { Field } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { useAthlete } from "@/lib/athletes";
import { useCreateAssessment } from "@/lib/assessments";
import "./AthleteForm.css";

// T-027 — Registro de avaliação física (US-007). Persona: Coach.
// assessmentDate é obrigatória e não dá pra salvar sem ela (critérios 1 e 2).
// Este form só CRIA registros: avaliações nunca são sobrescritas (US-008).
// Campos opcionais vazios vão como undefined, alinhado ao BodyMeasurements
// do domínio (T-049). Altura vem pré-preenchida da ficha do atleta.

const optionalMeasure = z.preprocess(
  (v) => (v === "" || v === null || v === undefined ? undefined : v),
  z.coerce
    .number({ invalid_type_error: "Valor inválido" })
    .positive("Deve ser positivo")
    .optional()
);

const schema = z.object({
  assessmentDate: z.string().min(1, "Informe a data da avaliação"),
  weightKg: z.coerce
    .number({ invalid_type_error: "Peso inválido" })
    .positive("Peso deve ser positivo"),
  heightCm: z.coerce
    .number({ invalid_type_error: "Altura inválida" })
    .positive("Altura deve ser positiva"),
  bodyFatPercentage: optionalMeasure,
  muscleMassKg: optionalMeasure,
  waistCm: optionalMeasure,
  hipCm: optionalMeasure,
});
type FormValues = z.infer<typeof schema>;

function todayLocal(): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(now.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

export function AssessmentCreatePage() {
  const { id: athleteId = "" } = useParams();
  const navigate = useNavigate();
  const { data: athlete, isLoading } = useAthlete(athleteId);
  const createMutation = useCreateAssessment(athleteId);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  async function onSubmit(values: FormValues) {
    try {
      await createMutation.mutateAsync({
        assessmentDate: values.assessmentDate,
        measurements: {
          weightKg: values.weightKg,
          heightCm: values.heightCm,
          bodyFatPercentage: values.bodyFatPercentage,
          muscleMassKg: values.muscleMassKg,
          waistCm: values.waistCm,
          hipCm: values.hipCm,
        },
      });
      navigate(`/coach/athletes/${athleteId}`, {
        state: { assessmentCreated: true },
      });
    } catch {
      setError("root", {
        message: "Não foi possível registrar a avaliação. Tente novamente.",
      });
    }
  }

  return (
    <AppShell navItems={[{ label: "Atletas", href: "/coach/athletes" }]}>
      <div className="athlete-form">
        <Link to={`/coach/athletes/${athleteId}`} className="athlete-form__back">
          &larr; Voltar para a ficha
        </Link>
        <h1 className="athlete-form__title">
          Nova avaliação{athlete ? ` — ${athlete.name}` : ""}
        </h1>

        {isLoading ? (
          <p>Carregando atleta...</p>
        ) : (
          <form
            className="athlete-form__form"
            onSubmit={handleSubmit(onSubmit)}
            noValidate
            key={athlete?.id ?? "new"}
          >
            <Field
              label="Data da avaliação"
              type="date"
              error={errors.assessmentDate?.message}
              {...register("assessmentDate", { value: todayLocal() })}
            />
            <div className="athlete-form__row">
              <Field
                label="Peso (kg)"
                type="number"
                inputMode="decimal"
                step="0.1"
                error={errors.weightKg?.message}
                {...register("weightKg")}
              />
              <Field
                label="Altura (cm)"
                type="number"
                inputMode="decimal"
                step="0.1"
                error={errors.heightCm?.message}
                {...register("heightCm", { value: athlete?.heightCm })}
              />
            </div>
            <div className="athlete-form__row">
              <Field
                label="% de gordura (opcional)"
                type="number"
                inputMode="decimal"
                step="0.1"
                error={errors.bodyFatPercentage?.message}
                {...register("bodyFatPercentage")}
              />
              <Field
                label="Massa muscular kg (opcional)"
                type="number"
                inputMode="decimal"
                step="0.1"
                error={errors.muscleMassKg?.message}
                {...register("muscleMassKg")}
              />
            </div>
            <div className="athlete-form__row">
              <Field
                label="Cintura cm (opcional)"
                type="number"
                inputMode="decimal"
                step="0.1"
                error={errors.waistCm?.message}
                {...register("waistCm")}
              />
              <Field
                label="Quadril cm (opcional)"
                type="number"
                inputMode="decimal"
                step="0.1"
                error={errors.hipCm?.message}
                {...register("hipCm")}
              />
            </div>

            {errors.root && (
              <p className="athlete-form__error" role="alert">
                {errors.root.message}
              </p>
            )}

            <div className="athlete-form__actions">
              <Button type="submit" loading={createMutation.isPending}>
                Registrar avaliação
              </Button>
            </div>
          </form>
        )}
      </div>
    </AppShell>
  );
}
