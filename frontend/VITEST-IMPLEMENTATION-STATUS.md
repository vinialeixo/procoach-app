# Checkpoint: Vitest no frontend

Atualizado em 2026-11-04, a pedido do usuário para pausar a implementação.

## Estado

- Progresso estimado: **70%**. O runner, a configuração e os primeiros testes estão implementados e validados; falta alinhar versões ao CI e concluir revisão/publicação.
- Branch independente: `feat/frontend-vitest`, criada a partir de `origin/main`.
- Worktree: `.worktrees/frontend-vitest`.
- Commit atual: `37d8883` (`test(frontend): bootstrap Vitest unit testing`), enviado para `origin`.
- PR [#23](https://github.com/vinialeixo/procoach-app/pull/23) aberto em draft; manter assim até a compatibilidade com Node 20 ser resolvida.
- A instalação de alinhamento de versões foi interrompida a pedido do usuário. Não há processo npm em execução.

## Implementado e validado

- Adicionados como `devDependencies`: Vitest, jsdom e Testing Library (React, DOM, user-event e jest-dom).
- `vite.config.ts` usa `vitest/config`, ambiente jsdom e arquivo de setup com limpeza do DOM e matchers do jest-dom.
- Scripts adicionados: `npm run test` e `npm run test:watch`.
- Testes iniciais em três arquivos:
  - `src/lib/diets.test.ts`: 7 testes de versão corrente e fluxo de status;
  - `src/lib/auth.test.ts`: 3 testes de leitura segura do `athleteId` do JWT;
  - `src/components/ProtectedRoute.test.tsx`: 3 testes de autorização de navegação no frontend.
- Com Vitest 5.1.0 no Node local 26.13.0: **13/13 testes passaram**.
- ESLint, `tsc -b`, build Vite e `npm ls --depth=0` passaram.
- `npm audit --include=dev --audit-level=high`: **0 vulnerabilidades**.

## Ponto que falta resolver

O workflow `.github/workflows/ci.yml` configura Node 20. A primeira seleção de dependências ficou incompatível: Vitest 5 requer Node 22.12 ou superior e jsdom 30/jest-dom 7 têm dependências com requisito Node 22+. A instalação inicial passou localmente porque o Node disponível é 26.

Foi iniciada, mas interrompida antes de gravar alterações, a troca para versões compatíveis com Node 20: `vitest@^4`, `jsdom@^26` e `@testing-library/jest-dom@^6.9.1`. O `package.json` e o lockfile ainda apontam para Vitest 5.1.0, jsdom 30.1.3 e jest-dom 7.0.1.

## Próximos passos ao retomar

1. Atualizar dependências para Vitest 4, jsdom 26 e jest-dom 6, mantendo os demais pacotes compatíveis com Node 20.
2. Confirmar versões e `engines` via `npm ls --depth=0` e repetir `npm audit --include=dev --audit-level=high`.
3. Rodar testes individuais e a suíte completa, ESLint, `tsc -b` e build; validar também com Node 20 (o ambiente local atual só tem Node 26 configurado).
4. Fazer code review, corrigir achados e então commitar/publicar um PR independente contra `main`.

## Referências de compatibilidade

- Vitest 4 requer Node 20+ e Vite 6+: https://v4.vitest.dev/guide/
- Vitest 5 requer Node 22.12+: https://vitest.dev/guide/
- React Testing Library requer `@testing-library/dom`: https://testing-library.com/docs/react-testing-library/intro/
