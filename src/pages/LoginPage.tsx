import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useNavigate } from "react-router-dom";
import { AuthLayout } from "@/components/ui/AuthLayout";
import { Field } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { useLogin, useVerifyMfa } from "@/lib/auth";
import "./LoginPage.css";

const credentialsSchema = z.object({
  email: z.string().min(1, "Informe seu e-mail").email("E-mail inválido"),
  password: z.string().min(1, "Informe sua senha"),
});
type CredentialsForm = z.infer<typeof credentialsSchema>;

const mfaSchema = z.object({
  code: z
    .string()
    .min(6, "O código tem 6 dígitos")
    .max(6, "O código tem 6 dígitos")
    .regex(/^\d+$/, "Use apenas números"),
});
type MfaForm = z.infer<typeof mfaSchema>;

export function LoginPage() {
  const navigate = useNavigate();
  const [challengeId, setChallengeId] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);

  const loginMutation = useLogin();
  const mfaMutation = useVerifyMfa();

  const credentialsForm = useForm<CredentialsForm>({
    resolver: zodResolver(credentialsSchema),
  });

  const mfaForm = useForm<MfaForm>({
    resolver: zodResolver(mfaSchema),
  });

  async function onSubmitCredentials(values: CredentialsForm) {
    setServerError(null);
    try {
      const result = await loginMutation.mutateAsync(values);
      if (result.status === "mfa_required") {
        setChallengeId(result.challengeId);
      } else {
        // A sessão (token + refresh) já foi persistida pelo lib/auth.
        navigate("/", { replace: true });
      }
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

  async function onSubmitMfa(values: MfaForm) {
    if (!challengeId) return;
    setServerError(null);
    try {
      await mfaMutation.mutateAsync({
        challengeId,
        code: values.code,
      });
      navigate("/", { replace: true });
    } catch (error) {
      if (!(error as { response?: unknown }).response) {
        setServerError(
          "Não foi possível conectar ao servidor. Tente novamente."
        );
        return;
      }
      setServerError("Código inválido ou expirado.");
    }
  }

  return (
    <AuthLayout>
      {challengeId === null ? (
        <>
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
        </>
      ) : (
        <>
          <h1 className="login-page__title">Verificação em duas etapas</h1>
          <p className="login-page__subtitle">
            Digite o código de 6 dígitos gerado pelo seu aplicativo autenticador.
          </p>

          <form
            className="login-page__form"
            onSubmit={mfaForm.handleSubmit(onSubmitMfa)}
            noValidate
          >
            <Field
              label="Código de verificação"
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              error={mfaForm.formState.errors.code?.message}
              {...mfaForm.register("code")}
            />

            {serverError && (
              <p className="login-page__server-error" role="alert">
                {serverError}
              </p>
            )}

            <Button type="submit" loading={mfaMutation.isPending}>
              Verificar
            </Button>
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                setChallengeId(null);
                setServerError(null);
              }}
            >
              Voltar
            </Button>
          </form>
        </>
      )}
    </AuthLayout>
  );
}
