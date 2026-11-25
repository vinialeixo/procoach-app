import { ButtonLink } from "@/components/ui/ButtonLink";
import "@/components/AppErrorBoundary.css";

export function NotFoundPage() {
  return (
    <main className="app-fallback">
      <section className="app-fallback__card">
        <span className="app-fallback__eyebrow">404 · Rota não encontrada</span>
        <h1 className="app-fallback__title">Página não encontrada</h1>
        <p className="app-fallback__description">
          O endereço acessado não existe ou foi movido. Volte para a página
          inicial para continuar no ProCoach.
        </p>
        <div className="app-fallback__actions">
          <ButtonLink to="/" className="pc-button--auto">
            Ir para o início
          </ButtonLink>
        </div>
      </section>
    </main>
  );
}
