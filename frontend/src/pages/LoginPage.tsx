import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { AuthLayout } from "@/components/ui/AuthLayout";
import { Field } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { StateMessage } from "@/components/ui/StateMessage";
import { useLogin } from "@/lib/auth";
import "./LoginPage.css";

const credentialsSchema = z.object({
  email: z.string().min(1, "Informe seu e-mail ou usuário"),
  password: z.string().min(1, "Informe sua senha"),
});
type CredentialsForm = z.infer<typeof credentialsSchema>;

// MFA/TOTP fora do fluxo por enquanto (backlog T-059/T-060).
export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const [serverError, setServerError] = useState<string | null>(null);

  const sessionExpired =
    Boolean((location.state as { sessionExpired?: boolean } | null)?.sessionExpired) ||
    searchParams.get("reason") === "session-expired";

  const inviteOnlyNotice = searchParams.get("reason") === "invite-only";

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
      setServerError("E-mail, usuário ou senha incorretos. Tente novamente.");
    }
  }

  return (
    <AuthLayout>
      <h1 className="login-page__title">Entrar</h1>
      <p className="login-page__subtitle">
        Acesse sua conta ProCoach (coach, atleta ou administrador) para continuar.
      </p>

      {sessionExpired && (
        <StateMessage
          kind="warning"
          title="Sessão expirada"
          description="Sua sessão expirou. Faça login novamente para continuar."
        />
      )}

      {inviteOnlyNotice && (
        <StateMessage
          kind="info"
          title="Cadastro por convite"
          description="O cadastro de novos coaches é feito apenas por convite do administrador."
        />
      )}

      <form
        className="login-page__form"
        onSubmit={credentialsForm.handleSubmit(onSubmitCredentials)}
        noValidate
      >
        <Field
          label="E-mail ou usuário"
          type="text"
          autoComplete="username"
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
      </form>
    </AuthLayout>
  );
}
