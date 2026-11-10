import { FormEvent, useState } from "react";
import { Button } from "@/components/ui/Button";
import { AthleteShell } from "@/layouts/AthleteShell";
import { getAthleteId } from "@/lib/auth";
import {
  NotificationPreferences,
  clearBrowserDeviceToken,
  createBrowserDeviceToken,
  detectCurrentDevicePlatform,
  getBrowserDeviceToken,
  saveBrowserDeviceToken,
  useNotificationPreferences,
  useRegisterNotificationDevice,
  useUnregisterNotificationDevice,
  useUpdateNotificationPreferences,
} from "@/lib/notifications";
import "./AthleteContent.css";

type Feedback = { tone: "success" | "error"; text: string };

function errorStatus(error: unknown): number | undefined {
  if (!error || typeof error !== "object" || !("response" in error)) return undefined;
  return (error as { response?: { status?: number } }).response?.status;
}

function requestErrorText(status: number | undefined, fallback: string) {
  if (status === 401) return "Sua sessão expirou. Entre novamente para consultar suas preferências.";
  if (status === 403) return "Você não tem permissão para alterar estas preferências.";
  return fallback;
}

function saveErrorText(error: unknown) {
  const status = errorStatus(error);
  if (status === 401) return "Sua sessão expirou. Entre novamente para salvar suas preferências.";
  if (status === 403) return "Você não tem permissão para alterar estas preferências.";
  return "Não foi possível salvar as preferências. Tente novamente.";
}

/**
 * T-075 — preferências de aviso do atleta (e-mail/push) e dispositivo de push.
 *
 * O aviso dentro do app é sempre criado; e-mail e push são canais externos e
 * podem ser desligados aqui. A lista de dispositivos não é exposta pelo
 * backend: o token registrado por este navegador fica no localStorage para que
 * o próprio atleta possa removê-lo. O token de push emitido por um provedor
 * real (FCM/APNs/Web Push) entra com a T-076; até lá o identificador local
 * exercita o mesmo contrato (POST/DELETE /notifications/devices).
 */
export function AthleteNotificationSettingsPage() {
  const athleteId = getAthleteId();
  const preferences = useNotificationPreferences(athleteId ?? "");
  const updatePreferences = useUpdateNotificationPreferences(athleteId ?? "");
  const registerDevice = useRegisterNotificationDevice();
  const unregisterDevice = useUnregisterNotificationDevice();

  const [draft, setDraft] = useState<Partial<NotificationPreferences>>({});
  const [deviceToken, setDeviceToken] = useState<string | null>(() =>
    athleteId ? getBrowserDeviceToken(athleteId) : null
  );
  const [preferencesFeedback, setPreferencesFeedback] = useState<Feedback | null>(null);
  const [deviceFeedback, setDeviceFeedback] = useState<Feedback | null>(null);

  if (!athleteId) {
    return (
      <AthleteShell>
        <section className="athlete-content">
          <h1>Avisos e preferências</h1>
          <p role="alert">Não foi possível identificar sua conta. Entre novamente.</p>
        </section>
      </AthleteShell>
    );
  }

  // O rascunho local prevalece sobre o último GET enquanto o atleta edita;
  // depois do PUT, o cache do React Query recebe o estado persistido.
  const current: NotificationPreferences = {
    emailEnabled: draft.emailEnabled ?? preferences.data?.emailEnabled ?? true,
    pushEnabled: draft.pushEnabled ?? preferences.data?.pushEnabled ?? true,
  };
  const isDirty = Boolean(preferences.data) && (
    current.emailEnabled !== preferences.data?.emailEnabled ||
    current.pushEnabled !== preferences.data?.pushEnabled
  );

  async function handleSavePreferences(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPreferencesFeedback(null);
    try {
      await updatePreferences.mutateAsync(current);
      setDraft({});
      setPreferencesFeedback({ tone: "success", text: "Preferências salvas." });
    } catch (error) {
      setPreferencesFeedback({ tone: "error", text: saveErrorText(error) });
    }
  }

  async function handleRegisterDevice() {
    if (!athleteId) return;
    setDeviceFeedback(null);
    const token = createBrowserDeviceToken();
    try {
      await registerDevice.mutateAsync({
        token,
        platform: detectCurrentDevicePlatform(),
      });
      saveBrowserDeviceToken(athleteId, token);
      setDeviceToken(token);
      setDeviceFeedback({ tone: "success", text: "Este navegador foi registrado para receber push." });
    } catch (error) {
      setDeviceFeedback({
        tone: "error",
        text: requestErrorText(errorStatus(error), "Não foi possível registrar este navegador. Tente novamente."),
      });
    }
  }

  async function handleUnregisterDevice() {
    if (!athleteId || !deviceToken) return;
    setDeviceFeedback(null);
    try {
      await unregisterDevice.mutateAsync(deviceToken);
      clearBrowserDeviceToken(athleteId);
      setDeviceToken(null);
      setDeviceFeedback({ tone: "success", text: "Este navegador não recebe mais push." });
    } catch (error) {
      setDeviceFeedback({
        tone: "error",
        text: requestErrorText(errorStatus(error), "Não foi possível remover este navegador. Tente novamente."),
      });
    }
  }

  return (
    <AthleteShell>
      <section className="athlete-content">
        <h1>Avisos e preferências</h1>
        <p>
          Escolha como quer receber os avisos de treino e dieta. O aviso dentro do app
          continua sempre disponível, mesmo com os canais externos desligados.
        </p>

        <form
          className="athlete-content__card athlete-content__preferences"
          onSubmit={handleSavePreferences}
        >
          <h2>Canais de aviso</h2>

          {preferences.isPending ? (
            <p role="status">Carregando suas preferências…</p>
          ) : preferences.isError ? (
            <p role="alert">
              {requestErrorText(errorStatus(preferences.error), "Não foi possível carregar suas preferências. Tente novamente.")}
            </p>
          ) : (
            <>
              <div className="athlete-content__channels" role="group" aria-label="Canais de aviso">
                <label className="athlete-content__channel">
                  <input
                    type="checkbox"
                    checked={current.emailEnabled}
                    onChange={(event) =>
                      setDraft((value) => ({ ...value, emailEnabled: event.target.checked }))
                    }
                  />
                  <span>
                    <strong>E-mail</strong>
                    <span className="athlete-content__hint">
                      Avisos enviados para o e-mail de acesso da sua conta.
                    </span>
                  </span>
                </label>

                <label className="athlete-content__channel">
                  <input
                    type="checkbox"
                    checked={current.pushEnabled}
                    onChange={(event) =>
                      setDraft((value) => ({ ...value, pushEnabled: event.target.checked }))
                    }
                  />
                  <span>
                    <strong>Push</strong>
                    <span className="athlete-content__hint">
                      Avisos no navegador ou aplicativo, nos dispositivos registrados abaixo.
                    </span>
                  </span>
                </label>
              </div>

              <div className="athlete-content__actions">
                <Button
                  type="submit"
                  loading={updatePreferences.isPending}
                  disabled={!isDirty}
                >
                  Salvar preferências
                </Button>
              </div>

              {preferencesFeedback && (
                <p
                  className={`athlete-content__feedback athlete-content__feedback--${preferencesFeedback.tone}`}
                  role={preferencesFeedback.tone === "error" ? "alert" : "status"}
                >
                  {preferencesFeedback.text}
                </p>
              )}
            </>
          )}
        </form>

        <section className="athlete-content__card athlete-content__preferences" aria-labelledby="push-devices-title">
          <h2 id="push-devices-title">Dispositivos para push</h2>
          <p className="athlete-content__hint">
            O push é entregue apenas nos dispositivos registrados. Registre este navegador
            para receber os avisos nele; você pode remover o registro a qualquer momento.
          </p>

          {deviceToken ? (
            <div className="athlete-content__device">
              <p>
                <strong>Este navegador está registrado para push.</strong>{" "}
                <span className="athlete-content__hint">
                  O registro vale para a conta atual. Em outro navegador ou aparelho,
                  registre-o separadamente.
                </span>
              </p>
              <div className="athlete-content__actions">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={handleUnregisterDevice}
                  loading={unregisterDevice.isPending}
                >
                  Remover este navegador
                </Button>
              </div>
            </div>
          ) : (
            <div className="athlete-content__actions">
              <Button
                type="button"
                onClick={handleRegisterDevice}
                loading={registerDevice.isPending}
              >
                Registrar este navegador
              </Button>
            </div>
          )}

          {deviceFeedback && (
            <p
              className={`athlete-content__feedback athlete-content__feedback--${deviceFeedback.tone}`}
              role={deviceFeedback.tone === "error" ? "alert" : "status"}
            >
              {deviceFeedback.text}
            </p>
          )}
        </section>
      </section>
    </AthleteShell>
  );
}
