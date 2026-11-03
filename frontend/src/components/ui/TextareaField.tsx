import { TextareaHTMLAttributes, forwardRef } from "react";
import "./Field.css";

type TextareaFieldProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  label: string;
  error?: string;
};

export const TextareaField = forwardRef<HTMLTextAreaElement, TextareaFieldProps>(
  ({ label, error, id, ...rest }, ref) => {
    const inputId = id ?? label.toLowerCase().replace(/\s+/g, "-");
    return (
      <div className="pc-field">
        <label htmlFor={inputId} className="pc-field__label">
          {label}
        </label>
        <textarea
          ref={ref}
          id={inputId}
          className="pc-field__input pc-field__input--textarea"
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
TextareaField.displayName = "TextareaField";
