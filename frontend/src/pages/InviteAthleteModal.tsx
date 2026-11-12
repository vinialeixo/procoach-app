import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Modal } from "@/components/ui/Modal";
import { Field } from "@/components/ui/Field";
import { Button } from "@/components/ui/Button";
import { getInviteErrorMessage, inviteUrl, useCreateInvite, type Invite } from "@/lib/invites";

// Coach gera o convite do atleta (primeira credencial). Não há envio de
// e-mail: o link aparece uma única vez e o coach o repassa ao atleta. Gerar
// um novo convite revoga o anterior do mesmo atleta.

const schema = z.object({
  email: z.string().min(1, "Informe o e-mail do atleta").email("E-mail inválido"),
});
type FormValues = z.infer<typeof schema>;

type Props = {
  open: boolean;
  athleteId: string;
  athleteName: string;
  defaultEmail?: string | null;
  onClose: () => void;
};

export function InviteAthleteModal({ open, athleteId, athleteName, defaultEmail, onClose }: Props) {
  const createInvite = useCreateInvite();
  const [invite, setInvite] = useState<Invite | null>(null);
  const [copied, setCopied] = useState(false);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { email: defaultEmail ?? "" },
  });

  useEffect(() => {
    if (open) {
      reset({ email: defaultEmail ?? "" });
    }
  }, [open, defaultEmail, reset]);

  function close() {
    setInvite(null);
    setCopied(false);
    createInvite.reset();
    reset();
    onClose();
  }

  async function onSubmit(values: FormValues) {
    try {
      setInvite(await createInvite.mutateAsync({ athleteId, email: values.email }));
    } catch (error) {
      setError("root", {
        message: getInviteErrorMessage(error),
      });
    }
  }

  async function copy() {
    if (!invite) return;
    try {
      await navigator.clipboard.writeText(inviteUrl(invite));
      setCopied(true);
    } catch {
      setCopied(false);
    }
  }

  return (
    <Modal open={open} title={`Convidar ${athleteName} para o app`} onClose={close}>
      {invite ? (
        <div>
          <p>
            Convite criado para <strong>{invite.email}</strong>. Envie o link abaixo ao atleta —
            ele vale por 7 dias, só pode ser usado uma vez e não será exibido de novo.
          </p>
          <Field label="Link do convite" value={inviteUrl(invite)} readOnly />
          <div className="pc-modal__actions">
            <Button type="button" variant="ghost" onClick={close}>
              Fechar
            </Button>
            <Button type="button" onClick={copy}>
              {copied ? "Link copiado" : "Copiar link"}
            </Button>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} noValidate>
          <p>
            O atleta usa este e-mail para entrar e define a própria senha pelo link. Um novo
            convite substitui o anterior.
          </p>
          <Field
            label="E-mail do atleta"
            type="email"
            autoComplete="off"
            error={errors.email?.message}
            {...register("email")}
          />
          {errors.root && (
            <p className="athlete-detail__error" role="alert">
              {errors.root.message}
            </p>
          )}
          <div className="pc-modal__actions">
            <Button type="button" variant="ghost" onClick={close}>
              Cancelar
            </Button>
            <Button type="submit" loading={createInvite.isPending}>
              Gerar convite
            </Button>
          </div>
        </form>
      )}
    </Modal>
  );
}
