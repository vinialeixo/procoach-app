import { FormEvent, useEffect, useRef, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Modal } from "@/components/ui/Modal";
import { SelectField } from "@/components/ui/SelectField";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { TextareaField } from "@/components/ui/TextareaField";
import {
  AddExercisePayload,
  Exercise,
  ExercisePrescription,
  IntensityType,
  MUSCLE_GROUPS,
  MuscleGroup,
  TrainingSession,
  UpdateExercisePrescriptionPayload,
} from "@/lib/training";
import {
  DAY_LABELS,
  EQUIPMENT_LABELS,
  INTENSITY_OPTIONS,
  MUSCLE_LABELS,
  STATUS_ACTION,
  STATUS_NEXT,
  errorMessage,
  movePrescription,
} from "./training-constants";

interface SessionEditorViewProps {
  selectedSession: TrainingSession;
  exercises: Exercise[];
  exercisesLoading: boolean;
  exercisesError: boolean;
  autoFocusAddExercise?: boolean;
  transitionSessionPending: boolean;
  transitionSessionError: unknown;
  reorderExercisesPending: boolean;
  reorderExercisesError: unknown;
  applyCurrentVersionPending: boolean;
  applyCurrentVersionError: unknown;
  addExercisePending: boolean;
  addExerciseError: unknown;
  updatePrescriptionPending: boolean;
  updatePrescriptionError: unknown;
  removePrescriptionPending: boolean;
  removePrescriptionError: unknown;
  onSearchExercises: (name: string, muscle: MuscleGroup | "") => void;
  onTransitionSession: (targetStatus: TrainingSession["status"]) => void;
  onReorderExercises: (prescriptionIds: string[]) => void;
  onApplyCurrentVersion: (exercise: ExercisePrescription) => void | Promise<void>;
  onAddExercise: (payload: AddExercisePayload) => Promise<void>;
  onUpdatePrescription: (prescriptionId: string, payload: UpdateExercisePrescriptionPayload) => Promise<void>;
  onRemovePrescription: (prescriptionId: string) => Promise<void>;
}

export function SessionEditorView({
  selectedSession,
  exercises,
  exercisesLoading,
  exercisesError,
  autoFocusAddExercise,
  transitionSessionPending,
  transitionSessionError,
  reorderExercisesPending,
  reorderExercisesError,
  applyCurrentVersionPending,
  applyCurrentVersionError,
  addExercisePending,
  addExerciseError,
  updatePrescriptionPending,
  updatePrescriptionError,
  removePrescriptionPending,
  removePrescriptionError,
  onSearchExercises,
  onTransitionSession,
  onReorderExercises,
  onApplyCurrentVersion,
  onAddExercise,
  onUpdatePrescription,
  onRemovePrescription,
}: SessionEditorViewProps) {
  const [libraryName, setLibraryName] = useState("");
  const [libraryMuscle, setLibraryMuscle] = useState<MuscleGroup | "">("");
  const [selectedExerciseId, setSelectedExerciseId] = useState("");
  const [editingPrescription, setEditingPrescription] = useState<ExercisePrescription | null>(null);
  const [removingPrescription, setRemovingPrescription] = useState<ExercisePrescription | null>(null);
  const [updatingExerciseId, setUpdatingExerciseId] = useState<string | null>(null);
  const [applySuccessFeedback, setApplySuccessFeedback] = useState<string | null>(null);

  const searchInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (autoFocusAddExercise) {
      const el = searchInputRef.current ?? document.getElementById("library-search-input");
      if (el) {
        el.focus();
        el.scrollIntoView?.({ behavior: "smooth", block: "start" });
      }
    }
  }, [autoFocusAddExercise, selectedSession.id]);

  async function handleApplyCurrentVersion(exercise: ExercisePrescription) {
    setUpdatingExerciseId(exercise.id);
    setApplySuccessFeedback(null);
    try {
      await onApplyCurrentVersion(exercise);
      setApplySuccessFeedback(`Exercício "${exercise.exerciseName}" atualizado para a versão atual.`);
    } finally {
      setUpdatingExerciseId(null);
    }
  }

  const selectedExercise = exercises.find((exercise) => exercise.id === selectedExerciseId);

  async function handleAddExercise(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedExercise) return;
    const formElement = event.currentTarget;
    const form = new FormData(formElement);
    const payload: AddExercisePayload = {
      exerciseId: selectedExercise.id,
      exerciseName: selectedExercise.name,
      orderIndex:
        Math.max(-1, ...selectedSession.exercises.map((exercise) => exercise.orderIndex)) + 1,
      sets: Number(form.get("sets")),
      repetitions: Number(form.get("repetitions")),
      load: Number(form.get("load")),
      intensityType: String(form.get("intensityType")) as IntensityType,
      intensityValue: form.get("intensityValue") ? Number(form.get("intensityValue")) : null,
      restSeconds: Number(form.get("restSeconds")),
    };
    await onAddExercise(payload);
    setSelectedExerciseId("");
    formElement.reset();
  }

  async function handleUpdatePrescription(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editingPrescription) return;
    const form = new FormData(event.currentTarget);
    const payload: UpdateExercisePrescriptionPayload = {
      exerciseId: editingPrescription.exerciseId,
      exerciseName: String(form.get("exerciseName")),
      sets: Number(form.get("sets")),
      repetitions: Number(form.get("repetitions")),
      load: Number(form.get("load")),
      intensityType: String(form.get("intensityType")) as IntensityType,
      intensityValue: Number(form.get("intensityValue")),
      restSeconds: Number(form.get("restSeconds")),
      notes: String(form.get("notes") ?? "").trim() || null,
    };
    await onUpdatePrescription(editingPrescription.id, payload);
    setEditingPrescription(null);
  }

  async function handleRemovePrescription() {
    if (!removingPrescription) return;
    await onRemovePrescription(removingPrescription.id);
    setRemovingPrescription(null);
  }

  return (
    <section className="training-page__section" aria-labelledby="session-detail-title">
      <div className="training-page__section-heading training-page__section-heading--split">
        <div>
          <p className="training-page__eyebrow">03 / Prescrição</p>
          <h2 id="session-detail-title">{selectedSession.name}</h2>
          <p className="training-page__muted">
            Semana {selectedSession.weekNumber} · {DAY_LABELS[String(selectedSession.dayOfWeek)]}
          </p>
        </div>
        <div className="training-page__plan-actions">
          <StatusBadge status={selectedSession.status} />
          {STATUS_NEXT[selectedSession.status] && (
            <Button
              variant="ghost"
              loading={transitionSessionPending}
              onClick={() => onTransitionSession(STATUS_NEXT[selectedSession.status]!)}
            >
              {STATUS_ACTION[STATUS_NEXT[selectedSession.status]!]}
            </Button>
          )}
        </div>
      </div>
      {selectedSession.description && <p className="training-page__muted">{selectedSession.description}</p>}
      {Boolean(transitionSessionError) && (
        <p className="training-page__alert" role="alert">
          {errorMessage(transitionSessionError, "Não foi possível alterar o status da sessão.")}
        </p>
      )}
      {selectedSession.status === "ACTIVE" && (
        <p className="training-page__notice" role="status">
          Esta sessão está ativa. Ao adicionar ou reordenar exercícios, o atleta receberá um aviso.
        </p>
      )}
      {selectedSession.status === "FINISHED" && (
        <p className="training-page__notice" role="status">
          Sessões finalizadas ficam somente para consulta.
        </p>
      )}

      <ol className="training-page__prescriptions">
        {selectedSession.exercises
          .slice()
          .sort((a, b) => a.orderIndex - b.orderIndex)
          .map((exercise, index, ordered) => (
            <li key={exercise.id} className="training-page__prescription">
              <span className="training-page__prescription-order">{index + 1}</span>
              <div className="training-page__prescription-main">
                <div className="training-page__prescription-heading">
                  <strong>{exercise.exerciseName}</strong>
                  {exercise.newerVersionAvailable && exercise.currentExerciseId && (
                    <span
                      className="training-page__badge-outdated pc-badge pc-badge--muted"
                      data-testid={`badge-outdated-${exercise.id}`}
                    >
                      Versão desatualizada (v{exercise.exerciseVersion})
                    </span>
                  )}
                </div>
                <span>
                  {exercise.sets} séries × {exercise.repetitions} reps · {exercise.load} kg ·{" "}
                  {exercise.intensityType}
                  {exercise.intensityValue !== null ? ` ${exercise.intensityValue}` : ""} · descanso{" "}
                  {exercise.restSeconds}s
                </span>
                {exercise.notes && <small>Observações: {exercise.notes}</small>}
                {exercise.newerVersionAvailable && exercise.currentExerciseId && (
                  <div className="training-page__version-warning">
                    <span>
                      Há uma versão mais nova deste exercício na biblioteca
                      {exercise.exerciseVersion !== null ? ` (prescrita: v${exercise.exerciseVersion})` : ""}.
                    </span>
                    <Button
                      type="button"
                      variant="ghost"
                      loading={applyCurrentVersionPending && updatingExerciseId === exercise.id}
                      disabled={
                        selectedSession.status === "FINISHED" ||
                        applyCurrentVersionPending ||
                        exercise.intensityValue === null
                      }
                      onClick={() => handleApplyCurrentVersion(exercise)}
                    >
                      Trocar pela versão atual
                    </Button>
                  </div>
                )}
              </div>
              <div className="training-page__prescription-actions">
                <Button
                  type="button"
                  variant="ghost"
                  disabled={selectedSession.status === "FINISHED"}
                  onClick={() => setEditingPrescription(exercise)}
                >
                  Editar
                </Button>
                <Button
                  type="button"
                  variant="ghost"
                  disabled={selectedSession.status === "FINISHED"}
                  onClick={() => setRemovingPrescription(exercise)}
                >
                  Remover
                </Button>
                <div className="training-page__reorder" aria-label={`Reordenar ${exercise.exerciseName}`}>
                  <Button
                    type="button"
                    variant="ghost"
                    disabled={index === 0 || reorderExercisesPending || selectedSession.status === "FINISHED"}
                    aria-label={`Mover ${exercise.exerciseName} para cima`}
                    onClick={() =>
                      onReorderExercises(movePrescription(ordered.map((item) => item.id), index, -1))
                    }
                  >
                    ↑
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    disabled={
                      index === ordered.length - 1 ||
                      reorderExercisesPending ||
                      selectedSession.status === "FINISHED"
                    }
                    aria-label={`Mover ${exercise.exerciseName} para baixo`}
                    onClick={() =>
                      onReorderExercises(movePrescription(ordered.map((item) => item.id), index, 1))
                    }
                  >
                    ↓
                  </Button>
                </div>
              </div>
            </li>
          ))}
      </ol>
      {Boolean(reorderExercisesError) && (
        <p className="training-page__alert" role="alert">
          {errorMessage(reorderExercisesError, "Não foi possível reordenar os exercícios.")}
        </p>
      )}
      {Boolean(applyCurrentVersionError) && (
        <p className="training-page__alert" role="alert">
          {errorMessage(applyCurrentVersionError, "Não foi possível trocar pela versão atual do exercício.")}
        </p>
      )}
      {applySuccessFeedback && (
        <p className="training-page__notice training-page__notice--success" role="status">
          {applySuccessFeedback}
        </p>
      )}

      {selectedSession.status !== "FINISHED" && (
        <div className="training-page__add-prescription" id="session-add-prescription">
          <div>
            <p className="training-page__eyebrow">Adicionar exercício</p>
            <h3>Prescrição da biblioteca</h3>
          </div>
          <form
            className="training-page__search"
            onSubmit={(event) => {
              event.preventDefault();
              onSearchExercises(libraryName.trim(), libraryMuscle);
              setSelectedExerciseId("");
            }}
          >
            <Field
              ref={searchInputRef}
              id="library-search-input"
              label="Buscar exercício"
              value={libraryName}
              onChange={(event) => setLibraryName(event.target.value)}
              placeholder="Nome do exercício"
            />
            <SelectField
              label="Grupo muscular"
              value={libraryMuscle}
              onChange={(event) => setLibraryMuscle(event.target.value as MuscleGroup | "")}
              options={MUSCLE_GROUPS.map((group) => ({ value: group, label: MUSCLE_LABELS[group] }))}
              placeholder="Todos os grupos"
            />
            <Button type="submit">Buscar</Button>
          </form>

          {exercisesError && (
            <p className="training-page__alert" role="alert">
              Não foi possível carregar a biblioteca.
            </p>
          )}
          {exercisesLoading ? (
            <p className="training-page__muted">Carregando exercícios…</p>
          ) : exercises.length ? (
            <div className="training-page__exercise-list">
              {exercises.map((exercise) => (
                <article
                  key={exercise.id}
                  className={
                    exercise.id === selectedExerciseId
                      ? "training-page__exercise training-page__exercise--selected"
                      : "training-page__exercise"
                  }
                >
                  <button
                    type="button"
                    className="training-page__exercise-select"
                    onClick={() => setSelectedExerciseId(exercise.id)}
                    aria-pressed={exercise.id === selectedExerciseId}
                  >
                    <span>
                      <strong>{exercise.name}</strong>
                      <small>
                        {exercise.muscleGroups.map((group) => MUSCLE_LABELS[group]).join(", ")} ·{" "}
                        {EQUIPMENT_LABELS[exercise.equipment] ?? exercise.equipment}
                      </small>
                    </span>
                    <span className="training-page__mono">v{exercise.version}</span>
                  </button>
                  <details className="training-page__exercise-details">
                    <summary>Detalhes técnicos e referências</summary>
                    <p>{exercise.execution}</p>
                    {!!exercise.commonErrors.length && (
                      <p>
                        <strong>Erros comuns:</strong> {exercise.commonErrors.join("; ")}
                      </p>
                    )}
                    {!!exercise.contraindications.length && (
                      <p>
                        <strong>Contraindicações:</strong> {exercise.contraindications.join("; ")}
                      </p>
                    )}
                    {!!exercise.progressions.length && (
                      <p>
                        <strong>Progressões:</strong> {exercise.progressions.join("; ")}
                      </p>
                    )}
                    {!!exercise.regressions.length && (
                      <p>
                        <strong>Regressões:</strong> {exercise.regressions.join("; ")}
                      </p>
                    )}
                    {!!exercise.references.length && (
                      <ul>
                        {exercise.references.map((reference, index) => (
                          <li key={`${reference.title}-${index}`}>
                            {reference.authors}. <cite>{reference.title}</cite>. {reference.source}
                            {reference.year ? `, ${reference.year}` : ""}.
                          </li>
                        ))}
                      </ul>
                    )}
                  </details>
                </article>
              ))}
            </div>
          ) : (
            <p className="training-page__muted">Nenhum exercício encontrado com esses filtros.</p>
          )}

          {selectedExercise && (
            <form className="training-page__form" onSubmit={handleAddExercise}>
              <div className="training-page__selected-exercise">
                <div>
                  <span className="training-page__mono">
                    {EQUIPMENT_LABELS[selectedExercise.equipment] ?? selectedExercise.equipment} · v
                    {selectedExercise.version}
                  </span>
                  <h4>{selectedExercise.name}</h4>
                </div>
                <p>{selectedExercise.execution}</p>
              </div>
              <div className="training-page__form-grid training-page__form-grid--prescription">
                <Field name="sets" label="Séries" type="number" min="1" step="1" required />
                <Field name="repetitions" label="Repetições" type="number" min="1" step="1" required />
                <Field name="load" label="Carga (kg)" type="number" min="0.1" step="0.1" required />
                <SelectField
                  name="intensityType"
                  label="Intensidade"
                  options={INTENSITY_OPTIONS}
                  placeholder="Selecione"
                  required
                />
                <Field name="intensityValue" label="Valor da intensidade" type="number" min="0.1" step="0.1" />
                <Field name="restSeconds" label="Descanso (segundos)" type="number" min="1" step="1" required />
              </div>
              {Boolean(addExerciseError) && (
                <p className="training-page__alert" role="alert">
                  {errorMessage(addExerciseError, "Não foi possível adicionar o exercício.")}
                </p>
              )}
              <Button type="submit" loading={addExercisePending}>
                Adicionar à sessão
              </Button>
            </form>
          )}
        </div>
      )}

      {/* Modal de edição de prescrição */}
      <Modal
        open={editingPrescription !== null}
        title="Editar prescrição"
        onClose={() => setEditingPrescription(null)}
      >
        {editingPrescription && (
          <form
            key={editingPrescription.id}
            className="training-page__form"
            onSubmit={handleUpdatePrescription}
          >
            <Field
              name="exerciseName"
              label="Nome do exercício"
              defaultValue={editingPrescription.exerciseName}
              required
            />
            <div className="training-page__form-grid training-page__form-grid--prescription">
              <Field
                name="sets"
                label="Séries"
                type="number"
                min="1"
                step="1"
                defaultValue={editingPrescription.sets}
                required
              />
              <Field
                name="repetitions"
                label="Repetições"
                type="number"
                min="1"
                step="1"
                defaultValue={editingPrescription.repetitions}
                required
              />
              <Field
                name="load"
                label="Carga (kg)"
                type="number"
                min="0.1"
                step="0.1"
                defaultValue={editingPrescription.load}
                required
              />
              <SelectField
                name="intensityType"
                label="Intensidade"
                options={INTENSITY_OPTIONS}
                defaultValue={editingPrescription.intensityType}
                required
              />
              <Field
                name="intensityValue"
                label="Valor da intensidade"
                type="number"
                min="0.1"
                step="0.1"
                defaultValue={editingPrescription.intensityValue ?? ""}
                required
              />
              <Field
                name="restSeconds"
                label="Descanso (segundos)"
                type="number"
                min="1"
                step="1"
                defaultValue={editingPrescription.restSeconds}
                required
              />
            </div>
            <TextareaField
              name="notes"
              label="Observações"
              defaultValue={editingPrescription.notes ?? ""}
            />
            {Boolean(updatePrescriptionError) && (
              <p className="training-page__alert" role="alert">
                {errorMessage(updatePrescriptionError, "Não foi possível salvar a prescrição.")}
              </p>
            )}
            <div className="training-page__plan-actions">
              <Button type="button" variant="ghost" onClick={() => setEditingPrescription(null)}>
                Cancelar
              </Button>
              <Button type="submit" loading={updatePrescriptionPending}>
                Salvar alterações
              </Button>
            </div>
          </form>
        )}
      </Modal>

      {/* Modal de remoção de prescrição */}
      <Modal
        open={removingPrescription !== null}
        title="Remover prescrição"
        onClose={() => setRemovingPrescription(null)}
      >
        {removingPrescription && (
          <div className="training-page__confirm">
            <p>
              Remover <strong>{removingPrescription.exerciseName}</strong> deste treino?
            </p>
            {selectedSession.status === "ACTIVE" && (
              <p className="training-page__muted">
                O treino está ativo: o atleta receberá um aviso da alteração.
              </p>
            )}
            {Boolean(removePrescriptionError) && (
              <p className="training-page__alert" role="alert">
                {errorMessage(removePrescriptionError, "Não foi possível remover a prescrição.")}
              </p>
            )}
            <div className="training-page__plan-actions">
              <Button type="button" variant="ghost" onClick={() => setRemovingPrescription(null)}>
                Cancelar
              </Button>
              <Button
                type="button"
                loading={removePrescriptionPending}
                onClick={handleRemovePrescription}
              >
                Remover
              </Button>
            </div>
          </div>
        )}
      </Modal>
    </section>
  );
}
