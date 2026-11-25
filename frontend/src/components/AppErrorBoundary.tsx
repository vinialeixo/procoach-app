import { Component, ErrorInfo, ReactNode } from "react";
import "./AppErrorBoundary.css";

type Props = {
  children: ReactNode;
};

type State = {
  hasError: boolean;
};

export class AppErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error("ProCoach render error:", error, info.componentStack);
  }

  handleReload = () => {
    window.location.reload();
  };

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    return (
      <main className="app-fallback" role="alert">
        <section className="app-fallback__card">
          <span className="app-fallback__eyebrow">Erro inesperado</span>
          <h1 className="app-fallback__title">
            Não foi possível exibir esta tela
          </h1>
          <p className="app-fallback__description">
            Ocorreu uma falha ao carregar a interface. Recarregue a página ou
            volte para a tela inicial.
          </p>
          <div className="app-fallback__actions">
            <button
              type="button"
              className="app-fallback__btn"
              onClick={this.handleReload}
            >
              Recarregar página
            </button>
            <a
              href="/"
              className="app-fallback__btn app-fallback__btn--ghost"
            >
              Voltar ao início
            </a>
          </div>
        </section>
      </main>
    );
  }
}
