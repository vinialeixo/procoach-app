import { getAthleteId } from "@/lib/auth";
import { DietVersion, useCurrentAthleteDiet } from "@/lib/diets";
import { ATHLETE_NAV } from "@/lib/athlete-nav";
import { AppShell } from "@/layouts/AppShell";
import "./AthleteContent.css";

const objectives: Record<DietVersion["objective"], string> = {
  CUTTING: "Redução de gordura", HYPERTROPHY: "Hipertrofia", MAINTENANCE: "Manutenção",
  BULKING: "Ganho de peso", WEIGHT_LOSS: "Perda de peso",
};

function RequestError({ status }: { status?: number }) {
  if (status === 401) return <p role="alert">Sua sessão expirou. Entre novamente para consultar sua dieta.</p>;
  if (status === 403) return <p role="alert">Você não tem permissão para consultar este conteúdo.</p>;
  return <p role="alert">Não foi possível carregar a dieta. Tente novamente mais tarde.</p>;
}

export function AthleteDietPage() {
  const athleteId = getAthleteId();
  const query = useCurrentAthleteDiet(athleteId ?? "");
  const status = query.error && typeof query.error === "object" && "response" in query.error
    ? (query.error as { response?: { status?: number } }).response?.status : undefined;

  return (
    <AppShell navItems={ATHLETE_NAV}>
      <section className="athlete-content">
        <h1>Minha dieta</h1>
        {!athleteId ? <p role="alert">Não foi possível identificar sua conta. Entre novamente.</p>
          : query.isPending ? <p role="status">Carregando sua dieta…</p>
          : query.isError ? <RequestError status={status} />
          : !query.data ? <p>Ainda não há uma dieta aprovada disponível.</p>
          : <article className="athlete-content__card">
              <p className="athlete-content__eyebrow">Plano alimentar · versão {query.data.version}</p>
              <h2>{objectives[query.data.objective]}</h2>
              <dl className="athlete-content__metrics">
                <div><dt>Calorias</dt><dd>{query.data.macros.calories.toLocaleString("pt-BR")} kcal</dd></div>
                <div><dt>Proteínas</dt><dd>{query.data.macros.protein} g</dd></div>
                <div><dt>Carboidratos</dt><dd>{query.data.macros.carbohydrates} g</dd></div>
                <div><dt>Gorduras</dt><dd>{query.data.macros.fat} g</dd></div>
              </dl>
            </article>}
      </section>
    </AppShell>
  );
}
