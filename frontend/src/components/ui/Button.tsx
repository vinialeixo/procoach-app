import { ButtonHTMLAttributes, forwardRef } from "react";
import "./Button.css";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "ghost";
  loading?: boolean;
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ variant = "primary", loading, children, disabled, ...rest }, ref) => (
    <button
      ref={ref}
      className={`pc-button pc-button--${variant}`}
      disabled={disabled || loading}
      aria-busy={loading}
      {...rest}
    >
      {loading ? "Enviando..." : children}
    </button>
  )
);
Button.displayName = "Button";
