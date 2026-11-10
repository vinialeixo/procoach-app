import { ReactNode, useEffect } from "react";
import "./Modal.css";

type ModalProps = {
  open: boolean;
  title: string;
  onClose: () => void;
  className?: string;
  children: ReactNode;
};

// Modal simples e acessível. Usado para confirmações que exigem motivo
// (mudança de status, recusa de sugestão, arquivamento) — tudo que alimenta
// o AuditLog.
export function Modal({ open, title, onClose, className, children }: ModalProps) {
  useEffect(() => {
    if (!open) return;
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") onClose();
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  return (
    <div className="pc-modal__backdrop" onClick={onClose}>
      <div
        className={className ? `pc-modal ${className}` : "pc-modal"}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(event) => event.stopPropagation()}
      >
        <h2 className="pc-modal__title">{title}</h2>
        <div className="pc-modal__body">{children}</div>
      </div>
    </div>
  );
}
