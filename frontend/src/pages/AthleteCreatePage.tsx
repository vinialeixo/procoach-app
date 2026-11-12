import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate, Link } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { COACH_NAV } from "@/lib/coach-nav";
import { Field } from "@/components/ui/Field";
import { SelectField } from "@/components/ui/SelectField";
import { Button } from "@/components/ui/Button";
import { ButtonLink } from "@/components/ui/ButtonLink";
import { GOALS, GOAL_LABEL, SEXES, SEX_LABEL, useCreateAthlete } from "@/lib/athletes";
import "./AthleteForm.css";

// T-019 — Cadastro de atleta (US-001). Persona: Coach.
// Todos os campos são obrigatórios (RF-01), com exceção do e-mail que é opcional
// para fins de deduplicação e convite direto (T-078 / T-083 / Issue #99).
// Sucesso -> redireciona para a ficha do atleta criado com banner
// "cadastrado com sucesso" (critério 2), e o registro passa a aparecer na
// listagem (critério 3).

const schema = z.object({
  name: z.string().min(1, "Informe o nome"),
  email: z
    .string()
    .trim()
    .email("E-mail inválido")
    .optional()
    .or(z.literal("")),
  sex: z.enum(SEXES, { error: "Selecione o sexo" }),
  birthDate: z.string().min(1, "Informe a data de nascimento"),
  heightCm: z.coerce
    .number({ error: "Altura inválida" })
    .positive("Altura deve ser positiva"),
  weightKg: z.coerce
    .number({ error: "Peso inválido" })
    .positive("Peso deve ser positivo"),
  goal: z.enum(GOALS, { error: "Selecione o objetivo" }),
});
type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

export function AthleteCreatePage() {
  const navigate = useNavigate();
  const createMutation = useCreateAthlete();
  const [conflictInfo, setConflictInfo] = useState<{
    message: string;
    athleteId?: string;
  } | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<FormInput, unknown, FormValues>({
    resolver: zodResolver(schema),
  });

  async function onSubmit(values: FormValues) {
    setConflictInfo(null);
    try {
      const athlete = await createMutation.mutateAsync(values);
      navigate(`/coach/athletes/${athlete.id}`, {
        replace: true,
        state: { created: true },
      });
    } catch (error) {
      const err = error as {
        response?: {
          status?: number;
          data?: { code?: string; message?: string; athleteId?: string };
        };
      };
      const status = err.response?.status;
      const code = err.response?.data?.code;
      const message = err.response?.data?.message ?? "";
      const athleteId = err.response?.data?.athleteId;

      if (
        status === 409 ||
        code === "EMAIL_ALREADY_REGISTERED" ||
        /já cadastrado/i.test(message)
      ) {
        setConflictInfo({
          message:
            "Esta pessoa já tem conta no sistema. Convide o atleta utilizando este e-mail pela ficha ou consulte a listagem.",
          athleteId,
        });
      } else {
        setError("root", { message: "Não foi possível cadastrar. Tente novamente." });
      }
    }
  }

  return (
    <AppShell navItems={COACH_NAV}>
      <div className="athlete-form">
        <Link to="/coach/athletes" className="athlete-form__back">
          &larr; Voltar para a listagem
        </Link>
        <h1 className="athlete-form__title">Novo atleta</h1>

        <form className="athlete-form__form" onSubmit={handleSubmit(onSubmit)} noValidate>
          <Field
            label="Nome"
            error={errors.name?.message}
            {...register("name")}
          />
          <Field
            label="E-mail (opcional)"
            type="email"
            placeholder="atleta@exemplo.com"
            error={errors.email?.message}
            {...register("email")}
          />
          <SelectField
            label="Sexo"
            placeholder="Selecione"
            error={errors.sex?.message}
            options={SEXES.map((s) => ({ value: s, label: SEX_LABEL[s] }))}
            {...register("sex")}
          />
          <Field
            label="Data de nascimento"
            type="date"
            error={errors.birthDate?.message}
            {...register("birthDate")}
          />
          <div className="athlete-form__row">
            <Field
              label="Altura (cm)"
              type="number"
              inputMode="decimal"
              step="0.1"
              error={errors.heightCm?.message}
              {...register("heightCm")}
            />
            <Field
              label="Peso (kg)"
              type="number"
              inputMode="decimal"
              step="0.1"
              error={errors.weightKg?.message}
              {...register("weightKg")}
            />
          </div>
          <SelectField
            label="Objetivo"
            placeholder="Selecione"
            options={GOALS.map((g) => ({ value: g, label: GOAL_LABEL[g] }))}
            error={errors.goal?.message}
            {...register("goal")}
          />

          {conflictInfo && (
            <div className="athlete-form__conflict-alert" role="alert">
              <p>{conflictInfo.message}</p>
              <div className="athlete-form__conflict-actions">
                {conflictInfo.athleteId ? (
                  <ButtonLink to={`/coach/athletes/${conflictInfo.athleteId}`}>
                    Ir para a ficha do atleta
                  </ButtonLink>
                ) : (
                  <ButtonLink to="/coach/athletes" variant="ghost">
                    Ver atletas cadastrados
                  </ButtonLink>
                )}
              </div>
            </div>
          )}

          {errors.root && (
            <p className="athlete-form__error" role="alert">
              {errors.root.message}
            </p>
          )}

          <div className="athlete-form__actions">
            <Button type="submit" loading={createMutation.isPending}>
              Cadastrar atleta
            </Button>
          </div>
        </form>
      </div>
    </AppShell>
  );
}
