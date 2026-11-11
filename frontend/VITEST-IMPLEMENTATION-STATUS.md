# Checkpoint: Vitest no frontend

Atualizado em 2026-11-10 — alinhamento com Node 20 e CI concluído.

## Estado

- Progresso: **100%**. O runner, a configuração, o alinhamento de dependências para Node 20 (CI) e Node 26 (local), e a execução de `npm run test` no workflow de CI estão implementados e validados.
- PR inicial [#23](https://github.com/vinialeixo/procoach-app/pull/23) já mergeado na `main`; o alinhamento final de versões e CI completa os pontos pendentes daquele checkpoint.

## Implementado e validado

- Dependências alinhadas para compatibilidade com Node 20 (`.github/workflows/ci.yml`) e Vite 8:
  - `vitest`: `^4.1.12` (suporta Node `^20.0.0 || ^22.0.0 || >=24.0.0` e Vite `^6 || ^7 || ^8`);
  - `jsdom`: `^26.1.0`;
  - `@testing-library/jest-dom`: `^6.9.1`;
  - `typescript`: `^5.9.3` (compatível com peerDependency `<6.1.0` do `@typescript-eslint/eslint-plugin`).
- `vite.config.ts` usa `vitest/config`, ambiente `jsdom`, `include: ["src/**/*.{test,spec}.{ts,tsx}"]` (isolando os testes unitários dos specs E2E do Playwright na raiz) e arquivo de setup (`./src/test/setup.ts`).
- `src/test/setup.ts` vincula `localStorage` e `sessionStorage` da instância do JSDOM (`globalThis.jsdom.window`) em `globalThis`, evitando o `ExperimentalWarning` e o retorno `undefined` do getter nativo de Web Storage do Node 22/25/26+ sem `--localstorage-file`.
- `.github/workflows/ci.yml` atualizado para rodar `npm run test` no job `frontend`.
- Suíte de testes unitários passando integralmente (**5 arquivos, 34/34 testes**):
  - `src/lib/diets.test.ts` (7 testes);
  - `src/lib/auth.test.ts` (3 testes);
  - `src/components/ProtectedRoute.test.tsx` (3 testes);
  - `src/lib/training.test.ts` (9 testes);
  - `src/lib/notifications.test.ts` (12 testes).
- ESLint (`npm run lint`), TypeScript + build (`npm run build`), `npm ls --depth=0` e `npm audit --include=dev --audit-level=high` (**0 vulnerabilidades**) validados.

## Referências de compatibilidade

- Vitest 4 requer Node 20+ e Vite 6+: https://v4.vitest.dev/guide/
- Vitest 5 requer Node 22.12+: https://vitest.dev/guide/
- React Testing Library requer `@testing-library/dom`: https://testing-library.com/docs/react-testing-library/intro/
