import { InputHTMLAttributes, forwardRef } from "react";
import "./Field.css";

type FieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  error?: string;
};

export const Field = forwardRef<HTMLInputElement, FieldProps>(
  ({ label, error, id, ...rest }, ref) => {
    const inputId = id ?? label.toLowerCase().replace(/\s+/g, "-");
    return (
      <div className="pc-field">
        <label htmlFor={inputId} className="pc-field__label">
          {label}
        </label>
        <input
          ref={ref}
          id={inputId}
          className="pc-field__input"
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${inputId}-error` : undefined}
          {...rest}
        />
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
