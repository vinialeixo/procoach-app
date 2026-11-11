# Checkpoint: Vitest no frontend

Atualizado em 2026-11-11 — alinhamento com Node 22 (Active LTS) e CI concluído.

## Estado

- Progresso: **100%**. O runner, a configuração, as dependências mais recentes (`vitest@5`, `jsdom@30`, `jest-dom@7`) alinhadas ao Node 22 LTS (CI) e Node 26 (local), e a execução de `npm run test` no workflow de CI estão implementados e validados.

## Implementado e validado

- Dependências alinhadas para Node 22 Active LTS (`.github/workflows/ci.yml`):
  - `vitest`: `^5.1.0` (suporta Node 22+ e Vite 8);
  - `jsdom`: `^30.1.3`;
  - `@testing-library/jest-dom`: `^7.0.1`;
  - `typescript`: `^5.9.3` (compatível com peerDependency `<6.1.0` do `@typescript-eslint/eslint-plugin`).
- `vite.config.ts` usa `vitest/config`, ambiente `jsdom`, `include: ["src/**/*.{test,spec}.{ts,tsx}"]` (isolando os testes unitários dos specs E2E do Playwright na raiz) e arquivo de setup (`./src/test/setup.ts`).
- `tsconfig.node.json` configurado com `emitDeclarationOnly: true` e `outDir: "./node_modules/.tmp"` para prevenir artefatos `vite.config.js` residuais no build.
- `src/test/setup.ts` vincula `localStorage` e `sessionStorage` da instância do JSDOM (`globalThis.jsdom.window`) em `globalThis`, evitando o `ExperimentalWarning` e o retorno `undefined` do getter nativo de Web Storage do Node 22/25/26+ sem `--localstorage-file`.
- `.github/workflows/ci.yml` configurado com `node-version: '22'` e execução de `npm run test` no job `frontend`.
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
