import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useNavigate } from "react-router-dom";
import { AuthLayout } from "@/components/ui/AuthLayout";
import { Field } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { useRegister } from "@/lib/auth";
import "./RegisterPage.css";

// Persona: Coach (tenant). O cadastro cria a conta do coach; o atleta não se
// registra sozinho — a credencial dele é emitida pelo coach/backend (ver
// comentário em AthleteHomePage sobre a pendência desse mecanismo).
const registerSchema = z
  .object({
    name: z.string().min(2, "Informe seu nome completo"),
    email: z.string().min(1, "Informe seu e-mail").email("E-mail inválido"),
    password: z
      .string()
      .min(8, "Mínimo de 8 caracteres")
      .regex(/[A-Za-z]/, "Use ao menos uma letra")
      .regex(/[0-9]/, "Use ao menos um número"),
    confirmPassword: z.string().min(1, "Confirme a senha"),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: "As senhas não conferem",
    path: ["confirmPassword"],
  });
type RegisterForm = z.infer<typeof registerSchema>;

export function RegisterPage() {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);
  const registerMutation = useRegister();

  const form = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
  });

  async function onSubmit(values: RegisterForm) {
    setServerError(null);
    try {
      await registerMutation.mutateAsync({
        name: values.name,
        email: values.email,
        password: values.password,
      });
      // Conta criada -> segue para ativar o MFA (obrigatório, RF-01).
      navigate("/registro/mfa", { replace: true });
    } catch {
      setServerError("Não foi possível criar a conta. Tente novamente.");
    }
  }

  return (
    <AuthLayout>
      <h1 className="register-page__title">Criar conta</h1>
      <p className="register-page__subtitle">
        Cadastro de coach. Em seguida você ativa a verificação em duas etapas.
      </p>

      <form
        className="register-page__form"
        onSubmit={form.handleSubmit(onSubmit)}
        noValidate
      >
        <Field
          label="Nome completo"
          autoComplete="name"
          error={form.formState.errors.name?.message}
          {...form.register("name")}
        />
        <Field
          label="E-mail"
          type="email"
          autoComplete="email"
          error={form.formState.errors.email?.message}
          {...form.register("email")}
        />
        <Field
          label="Senha"
          type="password"
          autoComplete="new-password"
          error={form.formState.errors.password?.message}
          {...form.register("password")}
        />
        <Field
          label="Confirmar senha"
          type="password"
          autoComplete="new-password"
          error={form.formState.errors.confirmPassword?.message}
          {...form.register("confirmPassword")}
        />

        {serverError && (
          <p className="register-page__server-error" role="alert">
            {serverError}
          </p>
        )}

        <Button type="submit" loading={registerMutation.isPending}>
          Continuar
        </Button>
      </form>

      <p className="register-page__login-link">
        Já tem conta? <Link to="/login">Entrar</Link>
      </p>
    </AuthLayout>
  );
}
