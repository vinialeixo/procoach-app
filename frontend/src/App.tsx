import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { LoginPage } from "@/pages/LoginPage";
import { RegisterPage } from "@/pages/RegisterPage";
import { CoachHomePage } from "@/pages/CoachHomePage";
import { AthleteHomePage } from "@/pages/AthleteHomePage";
import { AthleteListPage } from "@/pages/AthleteListPage";
import { AthleteCreatePage } from "@/pages/AthleteCreatePage";
import { AthleteDetailPage } from "@/pages/AthleteDetailPage";
import { AssessmentCreatePage } from "@/pages/AssessmentCreatePage";
import { DietEditorPage } from "@/pages/DietEditorPage";
import { ProtectedRoute } from "@/components/ProtectedRoute";
import { RoleRedirect } from "@/components/RoleRedirect";

export function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/registro" element={<RegisterPage />} />

        {/* Área do coach — RF-01..RF-42 em geral, RN-01 (ownership) */}
        <Route element={<ProtectedRoute allow={["COACH"]} />}>
          <Route path="/coach" element={<CoachHomePage />} />
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
          {/* próximas telas de coach entram aqui: /coach/library... */}
        </Route>

        {/* Área do atleta — RF-11 (só os próprios dados) */}
        <Route element={<ProtectedRoute allow={["ATHLETE"]} />}>
          <Route path="/athlete" element={<AthleteHomePage />} />
        </Route>

        {/* "/" decide pra onde mandar com base no papel de quem está logado */}
        <Route path="/" element={<RoleRedirect />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
