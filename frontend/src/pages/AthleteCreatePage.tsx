import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate, Link } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { Field } from "@/components/ui/Field";
import { SelectField } from "@/components/ui/SelectField";
import { Button } from "@/components/ui/Button";
import { useCreateAthlete } from "@/lib/athletes";
import "./AthleteForm.css";

// T-019 — Cadastro de atleta (US-001). Persona: Coach.
// Todos os campos são obrigatórios (RF-01); validação com Zod antes de salvar.
// Sucesso -> redireciona para a ficha do atleta criado com banner
// "cadastrado com sucesso" (critério 2), e o registro passa a aparecer na
// listagem (critério 3).

const schema = z.object({
  name: z.string().min(1, "Informe o nome"),
  sex: z.enum(["M", "F", "OTHER"], { required_error: "Selecione o sexo" }),
  birthDate: z.string().min(1, "Informe a data de nascimento"),
  heightCm: z.coerce
    .number({ invalid_type_error: "Altura inválida" })
    .positive("Altura deve ser positiva"),
  weightKg: z.coerce
    .number({ invalid_type_error: "Peso inválido" })
    .positive("Peso deve ser positivo"),
  goal: z.string().min(1, "Informe o objetivo"),
});
type FormValues = z.infer<typeof schema>;

export function AthleteCreatePage() {
  const navigate = useNavigate();
  const createMutation = useCreateAthlete();
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  async function onSubmit(values: FormValues) {
    try {
      const athlete = await createMutation.mutateAsync(values);
      navigate(`/coach/athletes/${athlete.id}`, {
        replace: true,
        state: { created: true },
      });
    } catch {
      setError("root", { message: "Não foi possível cadastrar. Tente novamente." });
    }
  }

  return (
    <AppShell navItems={[{ label: "Atletas", href: "/coach/athletes" }]}>
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
          <SelectField
            label="Sexo"
            placeholder="Selecione"
            error={errors.sex?.message}
            options={[
              { value: "M", label: "Masculino" },
              { value: "F", label: "Feminino" },
              { value: "OTHER", label: "Outro" },
            ]}
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
          <Field
            label="Objetivo"
            placeholder="Ex.: hipertrofia, cutting, manutenção"
            error={errors.goal?.message}
            {...register("goal")}
          />

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
