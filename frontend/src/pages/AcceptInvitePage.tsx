import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Link, useNavigate, useParams } from "react-router-dom";
import { AuthLayout } from "@/components/ui/AuthLayout";
import { Field } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { useAcceptInvite, useInvitePreview } from "@/lib/invites";
import "./LoginPage.css";

// Primeira credencial do atleta: o coach gera o convite, o atleta abre o link,
// confere o e-mail e define a própria senha. Rota pública (o token do convite é
// a credencial); ao aceitar, a sessão já fica aberta e o RoleRedirect leva o
// atleta para /athlete.

const schema = z
  .object({
    password: z
      .string()
      .min(8, "A senha precisa ter ao menos 8 caracteres")
      .max(72, "A senha pode ter no máximo 72 caracteres"),
    confirm: z.string().min(1, "Repita a senha"),
  })
  .refine((v) => v.password === v.confirm, {
    path: ["confirm"],
    message: "As senhas não conferem",
  });
type FormValues = z.infer<typeof schema>;

export function AcceptInvitePage() {
  const { token = "" } = useParams();
  const navigate = useNavigate();
  const [serverError, setServerError] = useState<string | null>(null);

  const preview = useInvitePreview(token);
  const accept = useAcceptInvite(token);
  const form = useForm<FormValues>({ resolver: zodResolver(schema) });

  async function onSubmit(values: FormValues) {
    setServerError(null);
    try {
      await accept.mutateAsync(values.password);
      navigate("/", { replace: true });
    } catch (error) {
      const status = (error as { response?: { status?: number } }).response?.status;
      if (status === 404) {
        setServerError("Este convite não é mais válido. Peça um novo ao seu coach.");
      } else if (status === 409) {
        setServerError("Este e-mail ou atleta já tem acesso. Tente entrar com sua senha.");
      } else if (!status) {
        setServerError("Não foi possível conectar ao servidor. Tente novamente.");
      } else {
        setServerError("Não foi possível criar o acesso. Tente novamente.");
      }
    }
  }

  if (preview.isLoading) {
    return (
      <AuthLayout>
        <p className="login-page__subtitle">Verificando convite…</p>
      </AuthLayout>
    );
  }

  if (preview.isError || !preview.data) {
    return (
      <AuthLayout>
        <h1 className="login-page__title">Convite inválido</h1>
        <p className="login-page__subtitle">
          Este link expirou ou já foi usado. Peça um novo convite ao seu coach.
        </p>
        <p className="login-page__register-link">
          Já tem acesso? <Link to="/login">Entrar</Link>
        </p>
      </AuthLayout>
    );
  }

  const { athleteName, email } = preview.data;

  return (
    <AuthLayout>
      <h1 className="login-page__title">Olá, {athleteName}</h1>
      <p className="login-page__subtitle">
        Seu coach convidou você para acompanhar sua dieta, treino e avaliações.
        Escolha uma senha para criar seu acesso.
      </p>

      <form className="login-page__form" onSubmit={form.handleSubmit(onSubmit)} noValidate>
        <Field label="E-mail de acesso" type="email" value={email} readOnly />
        <Field
          label="Senha"
          type="password"
          autoComplete="new-password"
          error={form.formState.errors.password?.message}
          {...form.register("password")}
        />
        <Field
          label="Repita a senha"
          type="password"
          autoComplete="new-password"
          error={form.formState.errors.confirm?.message}
          {...form.register("confirm")}
        />

        {serverError && (
          <p className="login-page__server-error" role="alert">
            {serverError}
          </p>
        )}

        <Button type="submit" loading={accept.isPending}>
          Criar meu acesso
        </Button>
      </form>
    </AuthLayout>
  );
}
