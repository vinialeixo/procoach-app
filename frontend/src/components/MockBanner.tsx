import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { homeForRole, saveSession, type Role } from "@/lib/auth";
import "./MockBanner.css";

export function MockBanner() {
  const queryClient = useQueryClient();
  const [feedback, setFeedback] = useState<string | null>(null);

  async function handleReset() {
    const { resetMockDb } = await import("@/mocks/db");
    resetMockDb();
    await queryClient.invalidateQueries();
    setFeedback("Dados resetados");
    window.setTimeout(() => setFeedback(null), 2000);
  }

  async function handleSwitchRole(role: Role) {
    const [{ MOCK_ATHLETE_CREDENTIALS, MOCK_COACH_CREDENTIALS }, { createMockJwt }] =
      await Promise.all([import("@/mocks/db"), import("@/mocks/jwt")]);

    const token =
      role === "ATHLETE"
        ? createMockJwt({
            sub: MOCK_ATHLETE_CREDENTIALS.athleteId,
            role: "ATHLETE",
            athleteId: MOCK_ATHLETE_CREDENTIALS.athleteId,
            email: MOCK_ATHLETE_CREDENTIALS.email,
            name: MOCK_ATHLETE_CREDENTIALS.name,
          })
        : createMockJwt({
            sub: MOCK_COACH_CREDENTIALS.id,
            role: "COACH",
            email: MOCK_COACH_CREDENTIALS.email,
            name: MOCK_COACH_CREDENTIALS.name,
          });

    saveSession(token, `mock-refresh-${role.toLowerCase()}`);
    queryClient.clear();
    window.location.assign(homeForRole(role));
  }

  return (
    <aside className="mock-banner" aria-label="Controles do modo mock">
      <span className="mock-banner__badge">MODO MOCK</span>
      <button
        type="button"
        className="mock-banner__btn"
        onClick={() => void handleSwitchRole("COACH")}
      >
        Coach
      </button>
      <button
        type="button"
        className="mock-banner__btn"
        onClick={() => void handleSwitchRole("ATHLETE")}
      >
        Atleta
      </button>
      <button
        type="button"
        className="mock-banner__btn mock-banner__btn--reset"
        onClick={() => void handleReset()}
      >
        Resetar dados mockados
      </button>
      {feedback && (
        <span className="mock-banner__feedback" role="status">
          {feedback}
        </span>
      )}
    </aside>
  );
}
