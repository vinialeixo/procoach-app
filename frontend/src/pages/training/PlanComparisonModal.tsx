import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { PlanComparison } from "@/lib/training";
import { CHANGE_LABEL, DAY_LABELS } from "./training-constants";

interface PlanComparisonModalProps {
  open: boolean;
  comparison: PlanComparison | null;
  onClose: () => void;
}

export function PlanComparisonModal({
  open,
  comparison,
  onClose,
}: PlanComparisonModalProps) {
  if (!open || !comparison) return null;

  return (
    <Modal
      open={open}
      title="Comparar versões do plano"
      className="training-page__compare-modal"
      onClose={onClose}
    >
      <div className="training-page__compare">
        <div className="training-page__compare-summary">
          <div className="training-page__compare-card">
            <span className="training-page__mono">
              ANTERIOR · V{comparison.previous.version}
            </span>
            <strong>{comparison.previous.name}</strong>
            <small>
              {comparison.summary.previousExerciseCount} exercícios
            </small>
          </div>
          <div className="training-page__compare-card training-page__compare-card--current">
            <span className="training-page__mono">
              CORRENTE · V{comparison.current.version}
            </span>
            <strong>{comparison.current.name}</strong>
            <small>
              {comparison.summary.currentExerciseCount} exercícios
            </small>
          </div>
        </div>

        <p className="training-page__muted">
          {comparison.summary.added} nova(s) ·{" "}
          {comparison.summary.removed} removida(s) ·{" "}
          {comparison.summary.changed} alterada(s) ·{" "}
          {comparison.summary.unchanged} sem mudanças
        </p>

        {comparison.sessions.length === 0 ? (
          <p className="training-page__muted">
            Nenhuma sessão nas duas versões.
          </p>
        ) : (
          <ol className="training-page__compare-list">
            {comparison.sessions.map((session) => {
              const changedExercises = session.exerciseChanges.filter(
                (exercise) => exercise.change !== "UNCHANGED"
              );
              return (
                <li
                  key={session.key}
                  className={`training-page__compare-session training-page__compare-session--${session.change.toLowerCase()}`}
                >
                  <div className="training-page__compare-session-heading">
                    <div>
                      <span className="training-page__mono">
                        SEM {session.weekNumber} ·{" "}
                        {DAY_LABELS[String(session.dayOfWeek)]}
                      </span>
                      <strong>{session.name}</strong>
                    </div>
                    <span
                      className={`training-page__compare-badge training-page__compare-badge--${session.change.toLowerCase()}`}
                    >
                      {CHANGE_LABEL[session.change]}
                    </span>
                  </div>

                  {changedExercises.length === 0 ? (
                    <p className="training-page__muted">
                      Nenhuma alteração nos exercícios desta sessão.
                    </p>
                  ) : (
                    <ul className="training-page__compare-exercises">
                      {changedExercises.map((exercise, index) => (
                        <li key={`${session.key}-${exercise.exerciseName}-${index}`}>
                          <span className="training-page__compare-badge training-page__compare-badge--small">
                            {CHANGE_LABEL[exercise.change]}
                          </span>
                          <strong>{exercise.exerciseName}</strong>
                          {exercise.change !== "ADDED" &&
                            exercise.previousSummary && (
                              <span className="training-page__compare-before">
                                Antes: {exercise.previousSummary}
                              </span>
                            )}
                          {exercise.change !== "REMOVED" &&
                            exercise.currentSummary && (
                              <span className="training-page__compare-after">
                                Depois: {exercise.currentSummary}
                              </span>
                            )}
                        </li>
                      ))}
                    </ul>
                  )}
                </li>
              );
            })}
          </ol>
        )}

        <div className="training-page__plan-actions">
          <Button
            type="button"
            variant="ghost"
            onClick={onClose}
          >
            Fechar
          </Button>
        </div>
      </div>
    </Modal>
  );
}
