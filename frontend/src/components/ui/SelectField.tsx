import { SelectHTMLAttributes, forwardRef } from "react";
import "./Field.css";

type Option = { value: string; label: string };

type SelectFieldProps = SelectHTMLAttributes<HTMLSelectElement> & {
  label: string;
  error?: string;
  options: Option[];
  placeholder?: string;
};

export const SelectField = forwardRef<HTMLSelectElement, SelectFieldProps>(
  ({ label, error, id, options, placeholder, ...rest }, ref) => {
    const inputId = id ?? label.toLowerCase().replace(/\s+/g, "-");
    return (
      <div className="pc-field">
        <label htmlFor={inputId} className="pc-field__label">
          {label}
        </label>
        <select
          ref={ref}
          id={inputId}
          className="pc-field__input"
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${inputId}-error` : undefined}
          {...rest}
        >
          {placeholder !== undefined && (
            <option value="" disabled>
              {placeholder}
            </option>
          )}
          {options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        {error && (
          <p id={`${inputId}-error`} className="pc-field__error" role="alert">
            {error}
          </p>
        )}
      </div>
    );
  }
);
SelectField.displayName = "SelectField";
