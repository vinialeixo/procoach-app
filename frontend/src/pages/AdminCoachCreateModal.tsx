import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Modal } from "@/components/ui/Modal";
import { Field } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { StateMessage } from "@/components/ui/StateMessage";
import {
  coachInviteUrl,
  useCreateCoach,
  type CreateCoachResponse,
} from "@/lib/admin";

const schema = z.object({
  name: z.string().trim().min(2, "Informe o nome completo do coach"),
  email: z.string().trim().min(1, "Informe o e-mail").email("E-mail inválido"),
});

type FormValues = z.infer<typeof schema>;

type Props = {
  open: boolean;
  onClose: () => void;
};

export function AdminCoachCreateModal({ open, onClose }: Props) {
  const createCoach = useCreateCoach();
  const [created, setCreated] = useState<CreateCoachResponse | null>(null);
  const [copied, setCopied] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", email: "" },
  });

  function close() {
    setCreated(null);
    setCopied(false);
    createCoach.reset();
    reset();
    onClose();
  }

  async function onSubmit(values: FormValues) {
    try {
      const response = await createCoach.mutateAsync(values);
      setCreated(response);
    } catch (error) {
      const status = (error as { response?: { status?: number } })?.response?.status;
      const code = (error as { response?: { data?: { code?: string } } })
        ?.response?.data?.code;
      if (status === 409 && code === "EMAIL_ALREADY_REGISTERED") {
        setError("email", {
          message:
            "Este e-mail já está cadastrado na plataforma. Verifique a lista de coaches ou use outro e-mail.",
        });
        return;
      }
      setError("root", {
        message: "Não foi possível criar o coach. Tente novamente.",
      });
    }
  }

  async function copyLink() {
    if (!created) return;
    try {
      await navigator.clipboard.writeText(coachInviteUrl(created.invite.token));
      setCopied(true);
    } catch {
      setCopied(false);
    }
  }

  return (
    <Modal
      open={open}
      title={created ? "Convite de coach criado" : "Novo coach por convite"}
      onClose={close}
    >
      {created ? (
        <div className="pc-modal__body">
          <StateMessage
            kind="warning"
            title="Copie este link agora"
            description={`Por segurança, o token não é armazenado em texto claro e não será exibido novamente após fechar este aviso. Válido até ${new Date(created.invite.expiresAt).toLocaleDateString("pt-BR")}.`}
          />

          <div>
            <div className="admin-cell-primary">{created.name}</div>
            <div className="admin-cell-mono">{created.email}</div>
          </div>

          <div className="admin-invite-box">
            <input
              type="text"
              readOnly
              aria-label="Link de ativação do coach"
              value={coachInviteUrl(created.invite.token)}
            />
            <Button
              type="button"
              fullWidth={false}
              className="admin-copy-btn"
              onClick={() => void copyLink()}
            >
              {copied ? "Link copiado" : "Copiar link"}
            </Button>
          </div>

          {!created.invite.emailSent && (
            <p className="field__hint">
              E-mail automático não configurado neste ambiente — envie o link manualmente para o coach.
            </p>
          )}

          <div className="pc-modal__actions">
            <Button type="button" variant="ghost" fullWidth={false} onClick={close}>
              Fechar
            </Button>
          </div>
        </div>
      ) : (
        <form className="pc-modal__body" onSubmit={handleSubmit(onSubmit)} noValidate>
          <p style={{ margin: 0, fontSize: "var(--text-sm)", color: "var(--color-ink-muted)" }}>
            O coach receberá um link de uso único (7 dias) para definir a própria senha e ativar o acesso.
          </p>

          <Field
            label="Nome do coach"
            type="text"
            autoComplete="off"
            error={errors.name?.message}
            {...register("name")}
          />

          <Field
            label="E-mail"
            type="email"
            autoComplete="off"
            error={errors.email?.message}
            {...register("email")}
          />

          {errors.root && (
            <p className="admin-modal__error" role="alert">
              {errors.root.message}
            </p>
          )}

          <div className="pc-modal__actions">
            <Button type="button" variant="ghost" fullWidth={false} onClick={close}>
              Cancelar
            </Button>
            <Button type="submit" fullWidth={false} loading={createCoach.isPending}>
              Criar e gerar convite
            </Button>
          </div>
        </form>
      )}
    </Modal>
  );
}
