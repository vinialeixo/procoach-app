import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useNavigate } from "react-router-dom";
import QRCode from "qrcode";
import { AuthLayout } from "@/components/ui/AuthLayout";
import { Field } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import {
  useStartMfaSetup,
  useConfirmMfaSetup,
  type MfaSetupStart,
} from "@/lib/auth";
import "./MfaSetupPage.css";

// T-011: ativação do MFA logo após o registro (rota /registro/mfa). O núcleo
// (POST /api/v1/auth/mfa/setup, T-008) devolve secret + otpauthUrl; exibimos o
// QR Code (lib `qrcode`) e o segredo como alternativa de cadastro manual, e
// confirmamos com o primeiro código de 6 dígitos (setup/confirm).
const mfaSchema = z.object({
  code: z
    .string()
    .min(6, "O código tem 6 dígitos")
    .max(6, "O código tem 6 dígitos")
    .regex(/^\d+$/, "Use apenas números"),
});
type MfaForm = z.infer<typeof mfaSchema>;

export function MfaSetupPage() {
  const navigate = useNavigate();
  const [setup, setSetup] = useState<MfaSetupStart | null>(null);
  const [qrDataUrl, setQrDataUrl] = useState<string | null>(null);
  const [serverError, setServerError] = useState<string | null>(null);

  const startMutation = useStartMfaSetup();
  const confirmMutation = useConfirmMfaSetup();

  const form = useForm<MfaForm>({ resolver: zodResolver(mfaSchema) });

  // Ao abrir, pede o segredo TOTP ao núcleo (uma vez).
  useEffect(() => {
    let cancelled = false;
    startMutation
      .mutateAsync()
      .then((result) => {
        if (!cancelled) setSetup(result);
      })
      .catch(() => {
        if (!cancelled)
          setServerError("Não foi possível iniciar a configuração do MFA.");
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Gera o QR Code PNG a partir da otpauthUrl (mantém o segredo manual abaixo
  // como alternativa de cadastro caso o QR não renderize).
  useEffect(() => {
    if (!setup?.otpauthUrl) return;
    let cancelled = false;
    QRCode.toDataURL(setup.otpauthUrl, {
      width: 224,
      margin: 2,
      color: { dark: "#121826", light: "#ffffff" },
    })
      .then((dataUrl) => {
        if (!cancelled) setQrDataUrl(dataUrl);
      })
      .catch(() => {
        if (!cancelled) setQrDataUrl(null);
      });
    return () => {
      cancelled = true;
    };
  }, [setup]);

  async function onSubmit(values: MfaForm) {
    setServerError(null);
    try {
      await confirmMutation.mutateAsync({ code: values.code });
      navigate("/login", { replace: true });
    } catch {
      setServerError("Código inválido. Confira o aplicativo autenticador.");
    }
  }

  return (
    <AuthLayout>
      <h1 className="mfa-setup__title">Ativar verificação em duas etapas</h1>
      <p className="mfa-setup__subtitle">
        Escaneie o QR code com seu aplicativo autenticador (ou digite o
        segredo) e confirme com o código gerado.
      </p>

      {setup ? (
        <>
          <div className="mfa-setup__qr">
            {qrDataUrl ? (
              <img
                src={qrDataUrl}
                alt="QR Code para cadastrar o ProCoach no seu aplicativo autenticador"
                className="mfa-setup__qr-image"
              />
            ) : (
              <p className="mfa-setup__loading">Gerando QR Code...</p>
            )}
          </div>

          {/* Segredo em texto: alternativa ao QR para cadastro manual. */}
          <div className="mfa-setup__secret">
            <p className="mfa-setup__secret-label">
              Não consegue escanear? Cadastro manual
            </p>
            <code className="mfa-setup__secret-value">{setup.secret}</code>
          </div>
        </>
      ) : (
        <p className="mfa-setup__loading">Gerando segredo...</p>
      )}

      <form
        className="mfa-setup__form"
        onSubmit={form.handleSubmit(onSubmit)}
        noValidate
      >
        <Field
          label="Código de verificação"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          error={form.formState.errors.code?.message}
          {...form.register("code")}
        />

        {serverError && (
          <p className="mfa-setup__server-error" role="alert">
            {serverError}
          </p>
        )}

        <Button type="submit" loading={confirmMutation.isPending}>
          Ativar e concluir
        </Button>
      </form>
    </AuthLayout>
  );
}
