import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useNavigate } from "react-router-dom";
import { AuthLayout } from "@/components/ui/AuthLayout";
import { Field } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { useLogin } from "@/lib/auth";
import "./LoginPage.css";

const credentialsSchema = z.object({
  email: z.string().min(1, "Informe seu e-mail").email("E-mail inválido"),
  password: z.string().min(1, "Informe sua senha"),
});
type CredentialsForm = z.infer<typeof credentialsSchema>;

// MFA/TOTP fora do fluxo por enquanto (backlog T-059/T-060).
export function LoginPage() {
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const loginMutation = useLogin();

  const credentialsForm = useForm<CredentialsForm>({
    resolver: zodResolver(credentialsSchema),
  });

  async function onSubmitCredentials(values: CredentialsForm) {
    setServerError(null);
    try {
      await loginMutation.mutateAsync(values);
      // A sessão (token + refresh) já foi persistida pelo lib/auth.
      navigate("/", { replace: true });
    } catch (error) {
      if (!(error as { response?: unknown }).response) {
        setServerError(
          "Não foi possível conectar ao servidor. Tente novamente."
        );
        return;
      }
      setServerError("E-mail ou senha incorretos. Tente novamente.");
    }
  }

  return (
    <AuthLayout>
      <h1 className="login-page__title">Entrar</h1>
      <p className="login-page__subtitle">
        Acesse sua conta de coach para continuar.
      </p>

      <form
        className="login-page__form"
        onSubmit={credentialsForm.handleSubmit(onSubmitCredentials)}
        noValidate
      >
        <Field
          label="E-mail"
          type="email"
          autoComplete="email"
          error={credentialsForm.formState.errors.email?.message}
          {...credentialsForm.register("email")}
        />
        <Field
          label="Senha"
          type="password"
          autoComplete="current-password"
          error={credentialsForm.formState.errors.password?.message}
          {...credentialsForm.register("password")}
        />

        {serverError && (
          <p className="login-page__server-error" role="alert">
            {serverError}
          </p>
        )}

        <Button type="submit" loading={loginMutation.isPending}>
          Entrar
        </Button>

        <p className="login-page__register-link">
          Ainda não tem conta? <Link to="/registro">Criar conta</Link>
        </p>
      </form>
    </AuthLayout>
  );
}
