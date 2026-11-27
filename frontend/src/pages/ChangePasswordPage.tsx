import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate } from "react-router-dom";
import { AppShell } from "@/layouts/AppShell";
import { AuthLayout } from "@/components/ui/AuthLayout";
import { Field } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { StateMessage } from "@/components/ui/StateMessage";
import { ADMIN_NAV } from "@/lib/admin-nav";
import { useChangeOwnPassword } from "@/lib/admin";
import { logout, mustChangePassword } from "@/lib/auth";

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, "Informe a senha atual"),
    newPassword: z
      .string()
      .min(8, "A nova senha deve ter pelo menos 8 caracteres"),
    confirmPassword: z.string().min(1, "Confirme a nova senha"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "As senhas não conferem",
    path: ["confirmPassword"],
  });

type PasswordForm = z.infer<typeof passwordSchema>;

export function ChangePasswordPage() {
  const navigate = useNavigate();
  const forced = mustChangePassword();
  const [serverError, setServerError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const mutation = useChangeOwnPassword();

  const form = useForm<PasswordForm>({
    resolver: zodResolver(passwordSchema),
  });

  async function onSubmit(values: PasswordForm) {
    setServerError(null);
    setSaved(false);
    try {
      await mutation.mutateAsync({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      });
      if (forced) {
        navigate("/admin", { replace: true });
        return;
      }
      setSaved(true);
      form.reset();
    } catch (error) {
      const code = (error as { response?: { data?: { code?: string } } })
        ?.response?.data?.code;
      if (code === "INVALID_CURRENT_PASSWORD") {
        setServerError("A senha atual está incorreta.");
        return;
      }
      if (code === "DEFAULT_PASSWORD_NOT_ALLOWED") {
        setServerError("Não é permitido reutilizar a senha padrão.");
        return;
      }
      setServerError("Não foi possível alterar a senha. Tente novamente.");
    }
  }

  const content = (
    <div style={{ maxWidth: 440, display: "flex", flexDirection: "column", gap: "var(--space-4)" }}>
      <div>
        <h1>{forced ? "Defina sua nova senha" : "Alterar minha senha"}</h1>
        <p style={{ marginTop: "var(--space-1)", color: "var(--color-ink-muted)" }}>
          {forced
            ? "Por segurança, substitua a senha inicial antes de acessar o painel administrativo."
            : "Atualize sua senha de acesso administrativo."}
        </p>
      </div>

      {forced && (
        <StateMessage
          kind="warning"
          title="Troca obrigatória no primeiro acesso"
          description="O acesso ao restante do painel será liberado assim que a nova senha for salva."
        />
      )}

      {saved && (
        <StateMessage
          kind="success"
          title="Senha atualizada"
          description="Sua nova senha já está em vigor."
        />
      )}

      <form
        onSubmit={form.handleSubmit(onSubmit)}
        noValidate
        style={{ display: "flex", flexDirection: "column", gap: "var(--space-4)" }}
      >
        <Field
          label="Senha atual"
          type="password"
          autoComplete="current-password"
          error={form.formState.errors.currentPassword?.message}
          {...form.register("currentPassword")}
        />
        <Field
          label="Nova senha"
          type="password"
          autoComplete="new-password"
          error={form.formState.errors.newPassword?.message}
          {...form.register("newPassword")}
        />
        <Field
          label="Confirmar nova senha"
          type="password"
          autoComplete="new-password"
          error={form.formState.errors.confirmPassword?.message}
          {...form.register("confirmPassword")}
        />

        {serverError && (
          <p role="alert" style={{ color: "var(--color-danger)", margin: 0, fontSize: "var(--text-sm)" }}>
            {serverError}
          </p>
        )}

        <div style={{ display: "flex", gap: "var(--space-3)", flexWrap: "wrap" }}>
          <Button type="submit" loading={mutation.isPending}>
            {forced ? "Salvar senha e entrar no painel" : "Salvar nova senha"}
          </Button>
          {forced && (
            <Button
              type="button"
              variant="ghost"
              onClick={() => {
                logout();
                navigate("/login", { replace: true });
              }}
            >
              Sair
            </Button>
          )}
        </div>
      </form>
    </div>
  );

  // Modo focado no primeiro acesso: evita menu lateral bloqueado com 403 em loop.
  if (forced) {
    return <AuthLayout>{content}</AuthLayout>;
  }

  return <AppShell navItems={ADMIN_NAV}>{content}</AppShell>;
}
