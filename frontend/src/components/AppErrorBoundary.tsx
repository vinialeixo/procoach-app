import { Component, ErrorInfo, ReactNode } from "react";
import { Button } from "@/components/ui/Button";
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
            <Button
              type="button"
              fullWidth={false}
              onClick={this.handleReload}
            >
              Recarregar página
            </Button>
            <a href="/" className="pc-button pc-button--ghost pc-button--auto">
              Voltar ao início
            </a>
          </div>
        </section>
      </main>
    );
  }
}
