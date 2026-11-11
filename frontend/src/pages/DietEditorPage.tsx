import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useNavigate, useParams } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { COACH_NAV } from "@/lib/coach-nav";
import { Field } from "@/components/ui/Field";
import { SelectField } from "@/components/ui/SelectField";
import { TextareaField } from "@/components/ui/TextareaField";
import { Button } from "@/components/ui/Button";
import { ButtonLink } from "@/components/ui/ButtonLink";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { useAthlete } from "@/lib/athletes";
import {
  DIET_OBJECTIVES,
  OBJECTIVE_LABEL,
  STATUS_LABEL,
  findCurrentDietVersion,
  useCreateDiet,
  useCreateDietVersion,
  useDiets,
} from "@/lib/diets";
import "./AthleteForm.css";
import "./AthleteDiets.css";
import "./DietEditor.css";

// T-034 — Editor de dieta (épico E3: US-011, US-014, US-015, US-016, US-017).
// Persona: Coach.
//
// Este editor NUNCA salva por cima: cria a v1 ou, a partir de uma versão
// existente, a PRÓXIMA versão — o domínio congela a anterior (readOnly) e
// exige motivo. Por isso o formulário mostra os chips v{n} -> v{n+1} e o
// motivo obrigatório (auditoria, US-016); não existe botão "salvar
// alterações" aqui. Versões congeladas não podem ser base de nova versão —
// só a versão atual pode (mesma regra do domínio).
//
// Rotas: /coach/athletes/:id/diets/new (v1)
//        /coach/athletes/:id/diets/:versionId/new-version (v{n+1} a partir
//        da versão atual :versionId)

function formatNumber(value: number, suffix = ""): string {
  return `${value.toLocaleString("pt-BR", { maximumFractionDigits: 1 })}${suffix}`;
}

const macroField = z.coerce
  .number({ error: "Valor inválido" })
  .positive("Deve ser maior que zero");

const schema = z.object({
  objective: z.enum(DIET_OBJECTIVES, {
    error: "Selecione o objetivo da dieta",
  }),
  calories: macroField,
  protein: macroField,
  carbohydrates: macroField,
  fat: macroField,
  // Só é enviado ao criar nova versão (auditoria US-016); na v1 não existe.
  changeReason: z.string().optional(),
});
type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

export function DietEditorPage() {
  const { id: athleteId = "", versionId } = useParams();
  // Rotas distintas => modo fixo por montagem (nunca alterna no meio).
  const isNewVersion = Boolean(versionId);
  const navigate = useNavigate();

  const { data: athlete, isLoading: athleteLoading, isError: athleteFailed } =
    useAthlete(athleteId);
  const { data: versions, isLoading: dietsLoading, isError: dietsFailed } =
    useDiets(athleteId);
  const createDiet = useCreateDiet(athleteId);
  const createVersion = useCreateDietVersion(athleteId);

  const loading = athleteLoading || dietsLoading;
  const loadFailed = athleteFailed || dietsFailed;

  const active = (versions ?? [])
    .filter((v) => !v.deleted)
    .sort((a, b) => a.version - b.version);
  const current = findCurrentDietVersion(active);
  const previous = isNewVersion
    ? active.find((v) => v.id === versionId)
    : undefined;

  // Guards que espelham as regras do domínio (imutabilidade da v{n}, base
  // sempre na versão atual) — o backend rejeitaria no mesmo ponto.
  const blockedByExistingChain = !isNewVersion && active.length > 0;
  const versionNotFound = isNewVersion && !previous;
  const staleVersion =
    isNewVersion && !!previous && (!current || current.id !== previous.id);

  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<FormInput, unknown, FormValues>({
    resolver: zodResolver(schema),
    defaultValues: previous
      ? {
          objective: previous.objective,
          calories: previous.macros.calories,
          protein: previous.macros.protein,
          carbohydrates: previous.macros.carbohydrates,
          fat: previous.macros.fat,
        }
      : undefined,
  });

  async function onSubmit(values: FormValues) {
    if (isNewVersion && !values.changeReason?.trim()) {
      setError("changeReason", {
        type: "manual",
        message:
          "Informe o motivo da alteração — ele entra no histórico de auditoria.",
      });
      return;
    }
    const macros = {
      calories: values.calories,
      protein: values.protein,
      carbohydrates: values.carbohydrates,
      fat: values.fat,
    };
    try {
      const created =
        isNewVersion && previous
          ? await createVersion.mutateAsync({
              versionId: previous.id,
              payload: {
                objective: values.objective,
                macros,
                changeReason: values.changeReason!.trim(),
              },
            })
          : await createDiet.mutateAsync({
              objective: values.objective,
              macros,
            });
      navigate(`/coach/athletes/${athleteId}?tab=dietas`, {
        state: { dietVersionCreated: created.version },
      });
    } catch {
      setError("root", {
        message: "Não foi possível salvar a dieta. Tente novamente.",
      });
    }
  }

  const nextVersion = previous ? previous.version + 1 : 1;
  const submitLabel = isNewVersion ? `Criar v${nextVersion}` : "Criar dieta (v1)";
  const saving = createDiet.isPending || createVersion.isPending;

  return (
    <AppShell navItems={COACH_NAV}>
      <div className="athlete-form">
        <Link
          to={`/coach/athletes/${athleteId}?tab=dietas`}
          className="athlete-form__back"
        >
          &larr; Voltar para a ficha
        </Link>
        <h1 className="athlete-form__title">
          {isNewVersion
            ? `Nova versão da dieta`
            : "Nova dieta"}
          {athlete ? ` — ${athlete.name}` : ""}
        </h1>

        {loading ? (
          <p>Carregando...</p>
        ) : loadFailed ? (
          <div className="diet-editor__blocked" role="alert">
            <p>Não foi possível carregar os dados. Tente novamente.</p>
            <ButtonLink to={`/coach/athletes/${athleteId}?tab=dietas`} variant="ghost">Voltar para a ficha</ButtonLink>
          </div>
        ) : blockedByExistingChain ? (
          <div className="diet-editor__blocked" role="status">
            <p>
              Este atleta já tem dieta{" "}
              <strong>
                v{current?.version ?? "?"} {current ? `(${STATUS_LABEL[current.status]})` : ""}
              </strong>
              . Dietas nunca são sobrescritas: para alterar, crie uma nova
              versão a partir da atual.
            </p>
            {current && (
              <ButtonLink
                to={`/coach/athletes/${athleteId}/diets/${current.id}/new-version`} variant="ghost">Nova versão a partir da v{current.version}</ButtonLink>
            )}
          </div>
        ) : versionNotFound ? (
          <div className="diet-editor__blocked" role="alert">
            <p>Versão não encontrada para este atleta. O link pode estar desatualizado.</p>
            <ButtonLink to={`/coach/athletes/${athleteId}?tab=dietas`} variant="ghost">Voltar para a ficha</ButtonLink>
          </div>
        ) : staleVersion ? (
          <div className="diet-editor__blocked" role="alert">
            <p>
              A v{previous!.version} não é a versão atual
              {current ? ` (a atual é a v${current.version})` : ""} — versões
              anteriores são somente leitura. Nova versão só pode ser criada a
              partir da versão atual.
            </p>
            {current && (
              <ButtonLink
                to={`/coach/athletes/${athleteId}/diets/${current.id}/new-version`} variant="ghost">Criar a partir da v{current.version}</ButtonLink>
            )}
          </div>
        ) : (
          <form
            className="athlete-form__form"
            onSubmit={handleSubmit(onSubmit)}
            noValidate
          >
            {isNewVersion && previous ? (
              <>
                <p className="diet-editor__notice">
                  <span className="diets__ver">v{previous.version}</span>
                  <span className="diet-editor__arrow">&rarr;</span>
                  <span className="diets__ver diets__ver--current">
                    v{nextVersion} (nova)
                  </span>
                  <span>
                    Nada é sobrescrito: salvar cria a v{nextVersion} e congela
                    a v{previous.version} (somente leitura). O motivo abaixo é
                    obrigatório e entra no histórico de auditoria.
                  </span>
                </p>
                <dl className="diet-editor__base">
                  <div className="diet-editor__base-title">
                    Base — v{previous.version} (somente leitura)
                  </div>
                  <div>
                    <dt>Objetivo</dt>
                    <dd>{OBJECTIVE_LABEL[previous.objective]}</dd>
                  </div>
                  <div>
                    <dt>Status</dt>
                    <dd>
                      <StatusBadge status={previous.status} />
                    </dd>
                  </div>
                  <div>
                    <dt>Calorias</dt>
                    <dd className="diets__mono">
                      {formatNumber(previous.macros.calories, " kcal")}
                    </dd>
                  </div>
                  <div>
                    <dt>Proteínas</dt>
                    <dd className="diets__mono">
                      {formatNumber(previous.macros.protein, " g")}
                    </dd>
                  </div>
                  <div>
                    <dt>Carboidratos</dt>
                    <dd className="diets__mono">
                      {formatNumber(previous.macros.carbohydrates, " g")}
                    </dd>
                  </div>
                  <div>
                    <dt>Gorduras</dt>
                    <dd className="diets__mono">
                      {formatNumber(previous.macros.fat, " g")}
                    </dd>
                  </div>
                </dl>
              </>
            ) : (
              <p className="diet-editor__notice">
                <span className="diets__ver diets__ver--current">v1</span>
                <span>
                  A dieta começa na v1 (Rascunho). A partir daqui nenhuma
                  edição sobrescreve: toda alteração cria uma nova versão
                  (v2, v3…), sempre com motivo registrado.
                </span>
              </p>
            )}

            <SelectField
              label="Objetivo"
              placeholder="Selecione"
              error={errors.objective?.message}
              options={DIET_OBJECTIVES.map((o) => ({
                value: o,
                label: OBJECTIVE_LABEL[o],
              }))}
              {...register("objective")}
            />
            {isNewVersion && (
              <TextareaField
                label="Motivo da alteração (auditoria)"
                placeholder="Ex.: subir proteína após nova avaliação do atleta"
                error={errors.changeReason?.message}
                rows={3}
                {...register("changeReason")}
              />
            )}
            <div className="athlete-form__row">
              <Field
                label="Calorias (kcal)"
                type="number"
                inputMode="decimal"
                step="1"
                error={errors.calories?.message}
                {...register("calories")}
              />
              <Field
                label="Proteínas (g)"
                type="number"
                inputMode="decimal"
                step="0.1"
                error={errors.protein?.message}
                {...register("protein")}
              />
            </div>
            <div className="athlete-form__row">
              <Field
                label="Carboidratos (g)"
                type="number"
                inputMode="decimal"
                step="0.1"
                error={errors.carbohydrates?.message}
                {...register("carbohydrates")}
              />
              <Field
                label="Gorduras (g)"
                type="number"
                inputMode="decimal"
                step="0.1"
                error={errors.fat?.message}
                {...register("fat")}
              />
            </div>

            {errors.root && (
              <p className="athlete-form__error" role="alert">
                {errors.root.message}
              </p>
            )}

            <div className="athlete-form__actions">
              <ButtonLink
                to={`/coach/athletes/${athleteId}?tab=dietas`}
                className="athlete-form__cancel" variant="ghost">Cancelar</ButtonLink>
              <Button type="submit" loading={saving}>
                {submitLabel}
              </Button>
            </div>
          </form>
        )}
      </div>
    </AppShell>
  );
}
