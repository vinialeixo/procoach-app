import { InputHTMLAttributes, forwardRef, useState } from "react";
import "./Field.css";

type FieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  error?: string;
  hint?: string;
};

export const Field = forwardRef<HTMLInputElement, FieldProps>(
  ({ label, error, hint, id, type, ...rest }, ref) => {
    const inputId = id ?? label.toLowerCase().replace(/\s+/g, "-");
    const [showPassword, setShowPassword] = useState(false);
    const isPassword = type === "password";
    const resolvedType = isPassword && showPassword ? "text" : type;

    const describedBy = [
      hint ? `${inputId}-hint` : null,
      error ? `${inputId}-error` : null,
    ]
      .filter(Boolean)
      .join(" ") || undefined;

    return (
      <div className="pc-field">
        <label htmlFor={inputId} className="pc-field__label">
          {label}
        </label>
        <div className={isPassword ? "pc-field__input-wrap" : undefined}>
          <input
            ref={ref}
            id={inputId}
            type={resolvedType}
            className="pc-field__input"
            aria-invalid={Boolean(error)}
            aria-describedby={describedBy}
            {...rest}
          />
          {isPassword && (
            <button
              type="button"
              className="pc-field__toggle"
              aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
              aria-pressed={showPassword}
              onClick={() => setShowPassword((prev) => !prev)}
            >
              {showPassword ? "Ocultar" : "Mostrar"}
            </button>
          )}
        </div>
        {hint && (
          <p id={`${inputId}-hint`} className="pc-field__hint">
            {hint}
          </p>
        )}
        {error && (
          <p id={`${inputId}-error`} className="pc-field__error" role="alert">
            {error}
          </p>
        )}
      </div>
    );
  }
);
Field.displayName = "Field";
