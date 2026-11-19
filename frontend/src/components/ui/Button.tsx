import { ButtonHTMLAttributes, forwardRef } from "react";
import "./Button.css";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "ghost";
  loading?: boolean;
  fullWidth?: boolean;
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      variant = "primary",
      loading,
      fullWidth = true,
      className,
      children,
      disabled,
      ...rest
    },
    ref
  ) => (
    <button
      ref={ref}
      className={[
        "pc-button",
        `pc-button--${variant}`,
        fullWidth ? "" : "pc-button--auto",
        className ?? "",
      ]
        .filter(Boolean)
        .join(" ")}
      disabled={disabled || loading}
      aria-busy={loading}
      {...rest}
    >
      {loading ? "Enviando..." : children}
    </button>
  )
);
Button.displayName = "Button";
