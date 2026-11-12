import { useState } from "react";
import { useParams, Link, useLocation, useSearchParams } from "react-router-dom";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { AppShell } from "@/layouts/AppShell";
import { COACH_NAV } from "@/lib/coach-nav";
import { AthleteAccessBadge, StatusBadge } from "@/components/ui/StatusBadge";
import { Modal } from "@/components/ui/Modal";
import { SelectField } from "@/components/ui/SelectField";
import { TextareaField } from "@/components/ui/TextareaField";
import { Button } from "@/components/ui/Button";
import { GOAL_LABEL, SEX_LABEL, useAthlete, useChangeAthleteStatus } from "@/lib/athletes";
import { AthleteAssessmentsPanel } from "./AthleteAssessmentsPanel";
import { AthleteDietsPanel } from "./AthleteDietsPanel";
import { AthleteInvitesSection } from "./AthleteInvitesSection";
import { InviteAthleteModal } from "./InviteAthleteModal";
import "./AthleteDetail.css";

// T-022 — Detalhe do atleta + troca de status (US-003, US-005, US-006).
// Persona: Coach. A ficha é o hub das próximas telas (abas: Avaliações,
// Dietas, Treinos, Check-ins). O ID imutável (US-002) fica em destaque.
// Não há exclusão física: status é soft-delete e toda mudança exige motivo
// (vai pro AuditLog, US-005/RF-42). A aba ativa vive na URL (?tab=) para
// deep-linkar direto na seção (ex.: após criar dieta).

const statusSchema = z.object({
  status: z.enum(["ACTIVE", "PAUSED", "FINISHED"], {
    error: "Selecione o novo status",
  }),
  reason: z.string().min(3, "Informe o motivo da alteração"),
});
type StatusForm = z.infer<typeof statusSchema>;

const TABS: { id: string; label: string }[] = [
  { id: "avaliacoes", label: "Avaliações" }, // T-027 — conteúdo real
  { id: "dietas", label: "Dietas" }, // T-034 — conteúdo real
  { id: "treinos", label: "Treinos" },
  { id: "checkins", label: "Check-ins" },
];

export function AthleteDetailPage() {
  const { id = "" } = useParams();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  // Banners de sucesso ao chegar do cadastro do atleta (US-001, critério 2),
  // do registro de avaliação (T-027) ou do editor de dieta (T-034).
  type DetailNavState = {
    created?: boolean;
    assessmentCreated?: boolean;
    dietVersionCreated?: number;
  };
  const navState = (location.state as DetailNavState | null) ?? {};
  const justCreated = navState.created ?? false;
  const assessmentCreated = navState.assessmentCreated ?? false;
  const dietVersionCreated = navState.dietVersionCreated ?? null;
  const { data: athlete, isLoading, isError } = useAthlete(id);
  const changeStatus = useChangeAthleteStatus(id);

  const [statusModalOpen, setStatusModalOpen] = useState(false);
  const [inviteModalOpen, setInviteModalOpen] = useState(false);

  // Aba ativa derivada da URL (?tab=): quem volta do editor de dieta cai
  // direto na aba Dietas, sem estado efêmero entre rotas.
  const requestedTab = searchParams.get("tab");
  const activeTab = TABS.some((t) => t.id === requestedTab)
    ? (requestedTab as string)
    : TABS[0].id;

  function selectTab(tabId: string) {
    setSearchParams({ tab: tabId });
  }

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm<StatusForm>({ resolver: zodResolver(statusSchema) });

  // Ao fechar sem salvar, descarta o que foi digitado e o erro anterior.
  function closeStatusModal() {
    setStatusModalOpen(false);
    reset();
  }

  async function onChangeStatus(values: StatusForm) {
    try {
      await changeStatus.mutateAsync(values);
      setStatusModalOpen(false);
      reset();
    } catch {
      // Atleta finalizado não aceita novo protocolo etc. — o backend bloqueia
      // (409/422); aqui só refletimos a rejeição.
      setError("root", { message: "Alteração recusada. Verifique a transição de status." });
    }
  }

  if (isError) {
    return (
      <AppShell navItems={COACH_NAV}>
        <p className="athlete-detail__error" role="alert">
          Não foi possível carregar o atleta.
        </p>
      </AppShell>
    );
  }

  if (isLoading || !athlete) {
    return (
      <AppShell navItems={COACH_NAV}>
        <p className="athlete-detail__loading">Carregando ficha…</p>
      </AppShell>
    );
  }

  return (
    <AppShell navItems={COACH_NAV}>
      <div className="athlete-detail">
        <Link to="/coach/athletes" className="athlete-detail__back">
          &larr; Voltar para a listagem
        </Link>

        {justCreated && (
          <p className="athlete-detail__created" role="status">
            Atleta cadastrado com sucesso.
          </p>
        )}
        {assessmentCreated && (
          <p className="athlete-detail__created" role="status">
            Avaliação registrada com sucesso.
          </p>
        )}
        {dietVersionCreated && (
          <p className="athlete-detail__created" role="status">
            {dietVersionCreated === 1
              ? "Dieta v1 criada com sucesso."
              : `Nova versão v${dietVersionCreated} criada — as anteriores ficaram somente leitura.`}
          </p>
        )}

        <div className="athlete-detail__head">
          <div>
            <h1 className="athlete-detail__name">{athlete.name}</h1>
            {/* ID único e imutável (US-002) — sempre visível, em mono */}
            <span className="athlete-detail__id">ID {athlete.id}</span>
          </div>
          <div className="athlete-detail__status">
            <StatusBadge status={athlete.status} />
            <AthleteAccessBadge athlete={athlete} />
            <Button
              variant="ghost"
              onClick={() => setInviteModalOpen(true)}
              disabled={athlete.hasCredential}
              title={athlete.hasCredential ? "Atleta já possui credencial de acesso" : undefined}
            >
              Convidar para o app
            </Button>
            <Button variant="ghost" onClick={() => setStatusModalOpen(true)}>
              Alterar status
            </Button>
          </div>
        </div>

        <dl className="athlete-detail__grid">
          <div><dt>Sexo</dt><dd>{SEX_LABEL[athlete.sex]}</dd></div>
          <div><dt>Nascimento</dt><dd>{athlete.birthDate}</dd></div>
          <div><dt>Altura</dt><dd>{athlete.heightCm} cm</dd></div>
          <div><dt>Peso</dt><dd>{athlete.weightKg} kg</dd></div>
          <div className="athlete-detail__goal"><dt>Objetivo</dt><dd>{GOAL_LABEL[athlete.goal]}</dd></div>
        </dl>

        <AthleteInvitesSection
          athlete={athlete}
          onOpenInviteModal={() => setInviteModalOpen(true)}
        />

        <div
          className="athlete-detail__tabs"
          role="tablist"
          aria-label="Seções do atleta"
        >
          {TABS.map((tab) => (
            <button
              key={tab.id}
              type="button"
              role="tab"
              id={`tab-${tab.id}`}
              aria-selected={activeTab === tab.id}
              aria-controls="athlete-detail-panel"
              tabIndex={activeTab === tab.id ? 0 : -1}
              className={
                "athlete-detail__tab" +
                (activeTab === tab.id ? " athlete-detail__tab--active" : "")
              }
              onClick={() => selectTab(tab.id)}
              onKeyDown={(event) => {
                const step =
                  event.key === "ArrowRight" ? 1 : event.key === "ArrowLeft" ? -1 : 0;
                if (!step) return;
                event.preventDefault();
                const index = TABS.findIndex((t) => t.id === activeTab);
                const next = TABS[(index + step + TABS.length) % TABS.length];
                selectTab(next.id);
                document.getElementById(`tab-${next.id}`)?.focus();
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        <section
          className="athlete-detail__panel"
          role="tabpanel"
          id="athlete-detail-panel"
          aria-labelledby={`tab-${activeTab}`}
        >
          {activeTab === "avaliacoes" ? (
            <AthleteAssessmentsPanel athleteId={athlete.id} />
          ) : activeTab === "dietas" ? (
            <AthleteDietsPanel athleteId={athlete.id} />
          ) : activeTab === "treinos" ? (
            <p className="athlete-detail__placeholder">
              Monte e acompanhe os treinos deste atleta em{" "}
              <Link to={`/coach/training?athlete=${athlete.id}`}>
                Treinos e biblioteca
              </Link>
              .
            </p>
          ) : (
            <p className="athlete-detail__placeholder">
              Os check-ins do atleta estarão disponíveis em breve.
            </p>
          )}
        </section>
      </div>

      <InviteAthleteModal
        open={inviteModalOpen}
        athleteId={athlete.id}
        athleteName={athlete.name}
        defaultEmail={athlete.email}
        onClose={() => setInviteModalOpen(false)}
      />

      <Modal
        open={statusModalOpen}
        title="Alterar status do atleta"
        onClose={closeStatusModal}
      >
        <form onSubmit={handleSubmit(onChangeStatus)} noValidate>
          <SelectField
            label="Novo status"
            placeholder="Selecione"
            error={errors.status?.message}
            options={[
              { value: "ACTIVE", label: "Ativo" },
              { value: "PAUSED", label: "Pausado" },
              { value: "FINISHED", label: "Finalizado" },
            ]}
            {...register("status")}
          />
          <TextareaField
            label="Motivo"
            error={errors.reason?.message}
            {...register("reason")}
          />
          {errors.root && (
            <p className="athlete-detail__error" role="alert">
              {errors.root.message}
            </p>
          )}
          <div className="pc-modal__actions">
            <Button
              type="button"
              variant="ghost"
              onClick={closeStatusModal}
            >
              Cancelar
            </Button>
            <Button type="submit" loading={changeStatus.isPending}>
              Confirmar
            </Button>
          </div>
        </form>
      </Modal>
    </AppShell>
  );
}
