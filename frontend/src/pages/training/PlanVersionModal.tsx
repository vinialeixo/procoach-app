import { FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { Field } from "@/components/ui/Field";
import { Modal } from "@/components/ui/Modal";
import { TextareaField } from "@/components/ui/TextareaField";
import { TrainingPlan } from "@/lib/training";
import { errorMessage } from "./training-constants";

interface PlanVersionModalProps {
  open: boolean;
  selectedPlan: TrainingPlan | undefined;
  sessionsCount: number;
  isPending: boolean;
  error: unknown;
  onClose: () => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
}

export function PlanVersionModal({
  open,
  selectedPlan,
  sessionsCount,
  isPending,
  error,
  onClose,
  onSubmit,
}: PlanVersionModalProps) {
  if (!open || !selectedPlan) return null;

  return (
    <Modal
      open={open}
      title="Criar nova versão do plano"
      onClose={onClose}
    >
      <form
        key={`version-${selectedPlan.id}`}
        className="training-page__form"
        onSubmit={onSubmit}
      >
        <p className="training-page__muted">
          A versão v{selectedPlan.version} permanece intacta no histórico. A nova versão
          v{selectedPlan.version + 1} nasce como rascunho encadeada à versão anterior.
        </p>
        <fieldset className="training-page__version-strategy">
          <legend className="training-page__version-strategy-legend">
            Estratégia de criação
          </legend>
          <label className="training-page__radio-option">
            <input
              type="radio"
              name="strategy"
              value="duplicate"
              defaultChecked
            />
            <span>
              <strong>Duplicar sessões e prescrições da versão atual (Recomendado)</strong>
              <small className="training-page__muted">
                Copia as {sessionsCount === 1 ? "1 sessão" : `${sessionsCount} sessões`} e
                respectivas prescrições para você apenas ajustar as alterações.
              </small>
            </span>
          </label>
          <label className="training-page__radio-option">
            <input
              type="radio"
              name="strategy"
              value="blank"
            />
            <span>
              <strong>Começar em branco (nova versão sem sessões copiadas)</strong>
              <small className="training-page__muted">
                Cria a nova versão v{selectedPlan.version + 1} vazia, sem sessões ou prescrições copiadas.
              </small>
            </span>
          </label>
        </fieldset>
        <Field
          name="newName"
          label="Nome da nova versão"
          defaultValue={selectedPlan.name}
          required
        />
        <TextareaField
          name="newDescription"
          label="Descrição da nova versão"
          defaultValue={selectedPlan.description ?? ""}
        />
        {Boolean(error) && (
          <p className="training-page__alert" role="alert">
            {errorMessage(
              error,
              "Não foi possível criar a nova versão do plano."
            )}
          </p>
        )}
        <div className="training-page__plan-actions">
          <Button
            type="button"
            variant="ghost"
            onClick={onClose}
          >
            Cancelar
          </Button>
          <Button type="submit" loading={isPending}>
            Criar versão
          </Button>
        </div>
      </form>
    </Modal>
  );
}
