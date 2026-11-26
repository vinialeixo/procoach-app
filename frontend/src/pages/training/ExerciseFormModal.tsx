import { useEffect } from "react";
import { useFieldArray, useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Modal } from "@/components/ui/Modal";
import { SelectField } from "@/components/ui/SelectField";
import { TextareaField } from "@/components/ui/TextareaField";
import {
  EQUIPMENT_TYPES,
  Exercise,
  MUSCLE_GROUPS,
  MuscleGroup,
  SaveExercisePayload,
} from "@/lib/training";
import { EQUIPMENT_LABELS, MUSCLE_LABELS, errorMessage } from "./training-constants";

const exerciseSchema = z.object({
  name: z.string().trim().min(1, "Informe o nome do exercício"),
  muscleGroups: z
    .array(z.enum(MUSCLE_GROUPS))
    .min(1, "Selecione pelo menos um grupo muscular"),
  equipment: z.string().min(1, "Selecione o equipamento"),
  execution: z.string().trim().min(1, "Descreva a execução correta"),
  commonErrorsText: z.string().optional(),
  progressionsText: z.string().optional(),
  regressionsText: z.string().optional(),
  contraindicationsText: z.string().optional(),
  references: z
    .array(
      z.object({
        authors: z.string().trim().min(1, "Informe os autores"),
        title: z.string().trim().min(1, "Informe o título"),
        source: z.string().trim().min(1, "Informe a fonte"),
        year: z.string().optional(),
      })
    )
    .optional(),
});

type ExerciseFormInput = z.input<typeof exerciseSchema>;
type ExerciseFormValues = z.output<typeof exerciseSchema>;

function linesToArray(text?: string): string[] {
  if (!text) return [];
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
}

interface ExerciseFormModalProps {
  open: boolean;
  exercise: Exercise | null;
  pending: boolean;
  error: unknown;
  onClose: () => void;
  onSave: (payload: SaveExercisePayload, existingId?: string) => Promise<void>;
}

export function ExerciseFormModal({
  open,
  exercise,
  pending,
  error,
  onClose,
  onSave,
}: ExerciseFormModalProps) {
  const {
    register,
    control,
    handleSubmit,
    reset,
    watch,
    setValue,
    formState: { errors },
  } = useForm<ExerciseFormInput, unknown, ExerciseFormValues>({
    resolver: zodResolver(exerciseSchema),
    defaultValues: {
      name: "",
      muscleGroups: [],
      equipment: "",
      execution: "",
      commonErrorsText: "",
      progressionsText: "",
      regressionsText: "",
      contraindicationsText: "",
      references: [],
    },
  });

  const { fields: refFields, append: appendRef, remove: removeRef } = useFieldArray({
    control,
    name: "references",
  });

  useEffect(() => {
    if (!open) return;
    if (exercise) {
      reset({
        name: exercise.name,
        muscleGroups: exercise.muscleGroups,
        equipment: exercise.equipment,
        execution: exercise.execution,
        commonErrorsText: exercise.commonErrors.join("\n"),
        progressionsText: exercise.progressions.join("\n"),
        regressionsText: exercise.regressions.join("\n"),
        contraindicationsText: exercise.contraindications.join("\n"),
        references: exercise.references.map((ref) => ({
          authors: ref.authors,
          title: ref.title,
          source: ref.source,
          year: ref.year != null ? String(ref.year) : "",
        })),
      });
    } else {
      reset({
        name: "",
        muscleGroups: [],
        equipment: "",
        execution: "",
        commonErrorsText: "",
        progressionsText: "",
        regressionsText: "",
        contraindicationsText: "",
        references: [],
      });
    }
  }, [open, exercise, reset]);

  const selectedMuscles = watch("muscleGroups") ?? [];

  function toggleMuscle(group: MuscleGroup) {
    const next = selectedMuscles.includes(group)
      ? selectedMuscles.filter((item) => item !== group)
      : [...selectedMuscles, group];
    setValue("muscleGroups", next, { shouldValidate: true });
  }

  async function submitForm(values: ExerciseFormValues) {
    const payload: SaveExercisePayload = {
      name: values.name,
      muscleGroups: values.muscleGroups,
      equipment: values.equipment,
      execution: values.execution,
      commonErrors: linesToArray(values.commonErrorsText),
      progressions: linesToArray(values.progressionsText),
      regressions: linesToArray(values.regressionsText),
      contraindications: linesToArray(values.contraindicationsText),
      references: (values.references ?? []).map((ref) => {
        const parsedYear = ref.year?.trim() ? Number(ref.year.trim()) : null;
        return {
          authors: ref.authors,
          title: ref.title,
          source: ref.source,
          year: parsedYear && Number.isFinite(parsedYear) ? parsedYear : null,
        };
      }),
    };
    await onSave(payload, exercise?.id);
  }

  const equipmentOptions = EQUIPMENT_TYPES.map((eq) => ({
    value: eq,
    label: EQUIPMENT_LABELS[eq] ?? eq,
  }));
  if (
    exercise &&
    !EQUIPMENT_TYPES.includes(exercise.equipment as (typeof EQUIPMENT_TYPES)[number])
  ) {
    equipmentOptions.unshift({
      value: exercise.equipment,
      label: EQUIPMENT_LABELS[exercise.equipment] ?? exercise.equipment,
    });
  }

  return (
    <Modal
      open={open}
      title={exercise ? `Editar exercício (v${exercise.version})` : "Novo exercício na biblioteca"}
      onClose={onClose}
    >
      <form className="training-page__form" onSubmit={handleSubmit(submitForm)} noValidate>
        {exercise && (
          <p className="training-page__notice" role="note">
            Ao salvar, será criada uma <strong>nova versão (v{exercise.version + 1})</strong>. A
            versão anterior (v{exercise.version}) permanece intacta nas prescrições existentes.
          </p>
        )}

        <Field
          id="exercise-form-name"
          label="Nome do exercício *"
          placeholder="Ex.: Supino inclinado com halteres"
          error={errors.name?.message}
          {...register("name")}
        />

        <fieldset className="training-page__muscle-fieldset">
          <legend>Grupos musculares *</legend>
          <div className="training-page__muscle-grid">
            {MUSCLE_GROUPS.map((group) => (
              <label key={group} className="training-page__checkbox-label">
                <input
                  type="checkbox"
                  checked={selectedMuscles.includes(group)}
                  onChange={() => toggleMuscle(group)}
                />
                <span>{MUSCLE_LABELS[group]}</span>
              </label>
            ))}
          </div>
          {errors.muscleGroups?.message && (
            <p className="training-page__alert" role="alert">
              {errors.muscleGroups.message}
            </p>
          )}
        </fieldset>

        <SelectField
          id="exercise-form-equipment"
          label="Equipamento *"
          options={equipmentOptions}
          placeholder="Selecione o equipamento"
          error={errors.equipment?.message}
          {...register("equipment")}
        />

        <TextareaField
          id="exercise-form-execution"
          label="Execução correta *"
          rows={3}
          placeholder="Descreva posicionamento, amplitude e controle do movimento"
          error={errors.execution?.message}
          {...register("execution")}
        />

        <div className="training-page__form-grid">
          <TextareaField
            id="exercise-form-errors"
            label="Erros comuns (1 por linha)"
            rows={2}
            {...register("commonErrorsText")}
          />
          <TextareaField
            id="exercise-form-contraindications"
            label="Contraindicações (1 por linha)"
            rows={2}
            {...register("contraindicationsText")}
          />
          <TextareaField
            id="exercise-form-progressions"
            label="Progressões (1 por linha)"
            rows={2}
            {...register("progressionsText")}
          />
          <TextareaField
            id="exercise-form-regressions"
            label="Regressões (1 por linha)"
            rows={2}
            {...register("regressionsText")}
          />
        </div>

        <div className="training-page__references-box">
          <div className="training-page__section-header">
            <span className="training-page__muted">Referências científicas (opcional)</span>
            <Button
              type="button"
              variant="ghost"
              fullWidth={false}
              onClick={() => appendRef({ authors: "", title: "", source: "", year: "" })}
            >
              + Referência
            </Button>
          </div>
          {refFields.map((field, index) => (
            <div key={field.id} className="training-page__reference-row">
              <Field
                label="Autores *"
                error={errors.references?.[index]?.authors?.message}
                {...register(`references.${index}.authors`)}
              />
              <Field
                label="Título *"
                error={errors.references?.[index]?.title?.message}
                {...register(`references.${index}.title`)}
              />
              <Field
                label="Fonte *"
                error={errors.references?.[index]?.source?.message}
                {...register(`references.${index}.source`)}
              />
              <Field
                label="Ano"
                type="number"
                {...register(`references.${index}.year`)}
              />
              <Button
                type="button"
                variant="ghost"
                fullWidth={false}
                onClick={() => removeRef(index)}
              >
                Remover
              </Button>
            </div>
          ))}
        </div>

        {Boolean(error) && (
          <p className="training-page__alert" role="alert">
            {errorMessage(error, "Não foi possível salvar o exercício na biblioteca.")}
          </p>
        )}

        <div className="training-page__modal-actions">
          <Button type="button" variant="ghost" fullWidth={false} onClick={onClose}>
            Cancelar
          </Button>
          <Button type="submit" fullWidth={false} loading={pending}>
            {exercise ? `Salvar nova versão (v${exercise.version + 1})` : "Criar exercício"}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
