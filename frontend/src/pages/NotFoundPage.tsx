import { Link } from "react-router-dom";
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
          <Link to="/" className="app-fallback__btn">
            Ir para o início
          </Link>
        </div>
      </section>
    </main>
  );
}
