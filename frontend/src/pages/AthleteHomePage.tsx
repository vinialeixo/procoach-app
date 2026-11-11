import { Link } from "react-router-dom";
import { AthleteShell } from "@/layouts/AthleteShell";
import "./HomeCards.css";

// Área do atleta (persona ATHLETE, RF-11 / US-038). Nav reduzida e somente
// leitura: quem monta/aprova dieta e treino é sempre o coach; o atleta só
// visualiza os próprios dados.
//
// Primeira credencial: o coach gera um convite (link de uso único) na ficha do
// atleta e o atleta define a senha em /convite/:token (AcceptInvitePage).
const SECTIONS = [
  {
    to: "/athlete/dieta",
    title: "Dieta",
    text: "Veja a dieta aprovada pelo seu coach e o histórico de versões.",
  },
  {
    to: "/athlete/treino",
    title: "Treino",
    text: "Consulte o plano e as sessões de treino atuais.",
  },
  {
    to: "/athlete/avaliacoes",
    title: "Avaliações",
    text: "Acompanhe a evolução das suas avaliações físicas.",
  },
  {
    to: "/athlete/avisos",
    title: "Preferências de aviso",
    text: "Escolha como quer receber os avisos do seu coach.",
  },
];

export function AthleteHomePage() {
  return (
    <AthleteShell>
      <h1>Minha área</h1>
      <p>Acompanhe o que o seu coach preparou para você.</p>
      <ul className="home-cards">
        {SECTIONS.map((section) => (
          <li key={section.to}>
            <Link to={section.to} className="home-cards__card">
              <span className="home-cards__title">{section.title}</span>
              <span className="home-cards__text">{section.text}</span>
            </Link>
          </li>
        ))}
      </ul>
    </AthleteShell>
  );
}
