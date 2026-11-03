import { AppShell } from "@/layouts/AppShell";

// Área do atleta (persona ATHLETE, RF-11 / US-038). Nav reduzida e somente
// leitura: quem monta/aprova dieta e treino é sempre o coach; o atleta só
// visualiza os próprios dados e envia check-in.
//
// PENDENTE NO BACKEND: o mecanismo de PRIMEIRA CREDENCIAL do atleta ainda
// não está definido (como o atleta recebe e-mail/senha + MFA na 1ª vez).
// Não travamos essas telas por causa disso: enquanto o endpoint não existe,
// assumimos um usuário atleta já autenticado (mesmo padrão de "contrato
// assumido" usado nos outros endpoints em src/lib/auth.ts). Ajustar quando
// o fluxo de convite/1º acesso for definido no núcleo.
const ATHLETE_NAV = [
  { label: "Dieta", href: "/athlete/dieta" },          // T-054 (somente leitura)
  { label: "Treino", href: "/athlete/treino" },        // T-054 (somente leitura)
  { label: "Avaliações", href: "/athlete/avaliacoes" },// T-055 (somente leitura)
  { label: "Check-in", href: "/athlete/check-in/novo" },// M2-021 (envio pelo atleta)
];

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
