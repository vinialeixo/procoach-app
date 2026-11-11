import { ReactNode } from "react";
import { Link, LinkProps } from "react-router-dom";
import "./Button.css";

type ButtonLinkProps = Omit<LinkProps, "children"> & {
  variant?: "primary" | "ghost";
  children: ReactNode;
};

// Link de navegação com a aparência do Button. Renderiza um único <a>
// (em vez de <Link><Button/></Link>, que aninha elementos interativos).
export function ButtonLink({
  variant = "primary",
  className,
  children,
  ...rest
}: ButtonLinkProps) {
  const classes = ["pc-button", `pc-button--${variant}`, className]
    .filter(Boolean)
    .join(" ");
  return (
    <Link className={classes} {...rest}>
      {children}
    </Link>
  );
}
