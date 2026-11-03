import { AppShell } from "@/layouts/AppShell";
import { ATHLETE_NAV } from "@/lib/athlete-nav";

// Área do atleta (persona ATHLETE, RF-11 / US-038). Nav reduzida e somente
// leitura: quem monta/aprova dieta e treino é sempre o coach; o atleta só
// visualiza os próprios dados e envia check-in.
//
// Primeira credencial: o coach gera um convite (link de uso único) na ficha do
// atleta e o atleta define a senha em /convite/:token (AcceptInvitePage).
export function AthleteHomePage() {
  return (
    <AppShell navItems={ATHLETE_NAV}>
      <h1>Minha área</h1>
      <p>
        Aqui entram a dieta e o treino atuais do atleta (somente leitura —
        quem monta e aprova é sempre o coach), o histórico de avaliações e o
        envio de check-ins. Ver tasks T-054/T-055 e M2-021 no backlog.
      </p>
    </AppShell>
  );
}
