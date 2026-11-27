import { lazy, Suspense, useEffect } from "react";
import { BrowserRouter, Navigate, Routes, Route, useNavigate } from "react-router-dom";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { RoleRedirect } from "@/components/RoleRedirect";
import { GlobalForbiddenBanner } from "@/components/GlobalForbiddenBanner";
import {
  setPasswordChangeRequiredHandler,
  setSessionExpiredHandler,
} from "@/lib/api-client";

const LoginPage = lazy(() =>
  import("@/pages/LoginPage").then((m) => ({ default: m.LoginPage }))
);
const AdminHomePage = lazy(() =>
  import("@/pages/AdminHomePage").then((m) => ({ default: m.AdminHomePage }))
);
const AdminCoachListPage = lazy(() =>
  import("@/pages/AdminCoachListPage").then((m) => ({
    default: m.AdminCoachListPage,
  }))
);
const AdminCoachDetailPage = lazy(() =>
  import("@/pages/AdminCoachDetailPage").then((m) => ({
    default: m.AdminCoachDetailPage,
  }))
);
const AdminAccountsPage = lazy(() =>
  import("@/pages/AdminAccountsPage").then((m) => ({
    default: m.AdminAccountsPage,
  }))
);
const ChangePasswordPage = lazy(() =>
  import("@/pages/ChangePasswordPage").then((m) => ({
    default: m.ChangePasswordPage,
  }))
);
const AcceptInvitePage = lazy(() =>
  import("@/pages/AcceptInvitePage").then((m) => ({ default: m.AcceptInvitePage }))
);
const CoachHomePage = lazy(() =>
  import("@/pages/CoachHomePage").then((m) => ({ default: m.CoachHomePage }))
);
const AthleteHomePage = lazy(() =>
  import("@/pages/AthleteHomePage").then((m) => ({ default: m.AthleteHomePage }))
);
const AthleteAssessmentHistoryPage = lazy(() =>
  import("@/pages/AthleteAssessmentHistoryPage").then((m) => ({
    default: m.AthleteAssessmentHistoryPage,
  }))
);
const AthleteDietPage = lazy(() =>
  import("@/pages/AthleteDietPage").then((m) => ({ default: m.AthleteDietPage }))
);
const AthleteTrainingPage = lazy(() =>
  import("@/pages/AthleteTrainingPage").then((m) => ({
    default: m.AthleteTrainingPage,
  }))
);
const AthleteNotificationSettingsPage = lazy(() =>
  import("@/pages/AthleteNotificationSettingsPage").then((m) => ({
    default: m.AthleteNotificationSettingsPage,
  }))
);
const AthleteListPage = lazy(() =>
  import("@/pages/AthleteListPage").then((m) => ({ default: m.AthleteListPage }))
);
const AthleteCreatePage = lazy(() =>
  import("@/pages/AthleteCreatePage").then((m) => ({
    default: m.AthleteCreatePage,
  }))
);
const AthleteDetailPage = lazy(() =>
  import("@/pages/AthleteDetailPage").then((m) => ({
    default: m.AthleteDetailPage,
  }))
);
const AssessmentCreatePage = lazy(() =>
  import("@/pages/AssessmentCreatePage").then((m) => ({
    default: m.AssessmentCreatePage,
  }))
);
const DietEditorPage = lazy(() =>
  import("@/pages/DietEditorPage").then((m) => ({ default: m.DietEditorPage }))
);
const DietDetailPage = lazy(() =>
  import("@/pages/DietDetailPage").then((m) => ({ default: m.DietDetailPage }))
);
const DietMealsPage = lazy(() =>
  import("@/pages/DietMealsPage").then((m) => ({ default: m.DietMealsPage }))
);
const TrainingPage = lazy(() =>
  import("@/pages/TrainingPage").then((m) => ({ default: m.TrainingPage }))
);

function RouteLoadingFallback() {
  return (
    <div
      role="status"
      aria-live="polite"
      style={{
        padding: "2rem",
        textAlign: "center",
        color: "var(--color-ink-muted)",
        fontFamily: "var(--font-sans)",
      }}
    >
      Carregando…
    </div>
  );
}

const MockBanner =
  import.meta.env.VITE_USE_MOCKS === "true"
    ? lazy(() =>
        import("@/components/MockBanner").then((module) => ({
          default: module.MockBanner,
        }))
      )
    : null;

function AuthNavigationBridge() {
  const navigate = useNavigate();

  useEffect(() => {
    setSessionExpiredHandler(() => {
      navigate("/login?reason=session-expired", {
        replace: true,
        state: { sessionExpired: true },
      });
    });
    setPasswordChangeRequiredHandler(() => {
      navigate("/admin/alterar-senha", { replace: true });
    });
    return () => {
      setSessionExpiredHandler(null);
      setPasswordChangeRequiredHandler(null);
    };
  }, [navigate]);

  return null;
}

export function App() {
  return (
    <BrowserRouter>
      <AuthNavigationBridge />
      <GlobalForbiddenBanner />
      <Suspense fallback={<RouteLoadingFallback />}>
        <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/registro" element={<Navigate to="/login?reason=invite-only" replace />} />
        {/* Convite do atleta (primeira credencial) — público: o token é a credencial */}
        <Route path="/convite/:token" element={<AcceptInvitePage />} />

        {/* Área do administrador — BR-ADM-01 (isolada de /coach e /athlete) */}
        <Route element={<ProtectedRoute allow={["ADMIN"]} />}>
          <Route path="/admin" element={<AdminHomePage />} />
          <Route path="/admin/coaches" element={<AdminCoachListPage />} />
          <Route path="/admin/coaches/:coachId" element={<AdminCoachDetailPage />} />
          <Route path="/admin/admins" element={<AdminAccountsPage />} />
          <Route path="/admin/alterar-senha" element={<ChangePasswordPage />} />
        </Route>

        {/* Área do coach — RF-01..RF-42 em geral, RN-01 (ownership) */}
        <Route element={<ProtectedRoute allow={["COACH"]} />}>
          <Route path="/coach" element={<CoachHomePage />} />
          <Route path="/coach/training" element={<TrainingPage />} />
          {/* Gestão de atletas — T-019 (cadastro) e T-022 (listagem/detalhe/status) */}
          <Route path="/coach/athletes" element={<AthleteListPage />} />
          <Route path="/coach/athletes/new" element={<AthleteCreatePage />} />
          <Route path="/coach/athletes/:id" element={<AthleteDetailPage />} />
          {/* T-027 — registro de avaliação física */}
          <Route
            path="/coach/athletes/:id/assessments/new"
            element={<AssessmentCreatePage />}
          />
          {/* T-034 — dieta: v1 e novas versões (nunca sobrescreve) */}
          <Route
            path="/coach/athletes/:id/diets/new"
            element={<DietEditorPage />}
          />
          <Route
            path="/coach/athletes/:id/diets/:versionId/new-version"
            element={<DietEditorPage />}
          />
          <Route
            path="/coach/athletes/:id/diets/:versionId"
            element={<DietDetailPage />}
          />
          <Route
            path="/coach/athletes/:id/diets/:versionId/meals"
            element={<DietMealsPage />}
          />
          {/* próximas telas de coach entram aqui: /coach/library... */}
        </Route>

        {/* Área do atleta — RF-11 (só os próprios dados) */}
        <Route element={<ProtectedRoute allow={["ATHLETE"]} />}>
          <Route path="/athlete" element={<AthleteHomePage />} />
          <Route path="/athlete/dieta" element={<AthleteDietPage />} />
          <Route path="/athlete/treino" element={<AthleteTrainingPage />} />
          {/* T-075 — preferências de aviso e dispositivos de push */}
          <Route path="/athlete/avisos" element={<AthleteNotificationSettingsPage />} />
          <Route
            path="/athlete/avaliacoes"
            element={<AthleteAssessmentHistoryPage />}
          />
        </Route>

        {/* "/" decide pra onde mandar com base no papel de quem está logado */}
        <Route path="/" element={<RoleRedirect />} />
        <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Suspense>
      {MockBanner && (
        <Suspense fallback={null}>
          <MockBanner />
        </Suspense>
      )}
    </BrowserRouter>
  );
}
