# Checkpoint: Vitest no frontend

Atualizado em 2026-11-18 — inventário sincronizado com os arquivos de teste atuais em `frontend/src/`.

## Estado

- Progresso: **100%**. O runner, a configuração, as dependências (`vitest@5`, `jsdom@31`, `jest-dom@7`) alinhadas ao Node 22 LTS (CI), e a execução de `npm run test` no workflow de CI estão configurados.
- Inventário atual: **19 arquivos** de teste unitário/componente em `frontend/src/**/*.{test,spec}.{ts,tsx}`.

## Implementado e inventário de testes

- Dependências configuradas no `frontend/package.json` e executadas no `.github/workflows/ci.yml` (`node-version: '22'`):
  - `vitest`: `^5.1.0`;
  - `jsdom`: `^31.0.1`;
  - `@testing-library/jest-dom`: `^7.0.1`;
  - `typescript`: `^5.9.3` (compatível com peerDependency `<6.1.0` do `@typescript-eslint/eslint-plugin`).
- `vite.config.ts` usa `vitest/config`, ambiente `jsdom`, `include: ["src/**/*.{test,spec}.{ts,tsx}"]` (isolando os testes unitários dos specs E2E do Playwright na raiz) e arquivo de setup (`./src/test/setup.ts`).
- `tsconfig.node.json` configurado com `emitDeclarationOnly: true` e `outDir: "./node_modules/.tmp"` para prevenir artefatos `vite.config.js` residuais no build.
- `src/test/setup.ts` vincula `localStorage` e `sessionStorage` da instância do JSDOM (`globalThis.jsdom.window`) em `globalThis`, evitando o `ExperimentalWarning` e o retorno `undefined` do getter nativo de Web Storage do Node 22/25/26+ sem `--localstorage-file`.
- `.github/workflows/ci.yml` configurado com `node-version: '22'` e execução de `npm run lint`, `npm run test` e `npm run build` no job `frontend`.
- Arquivos de teste atualmente presentes em `frontend/src/` (**19 arquivos**):
  - `src/components/ProtectedRoute.test.tsx`
  - `src/lib/athletes.test.ts`
  - `src/lib/auth.test.ts`
  - `src/lib/diets.test.ts`
  - `src/lib/invites.test.ts`
  - `src/lib/notifications.test.ts`
  - `src/lib/training.test.ts`
  - `src/mocks/mocks.test.ts`
  - `src/pages/AthleteAccess.test.tsx`
  - `src/pages/AthleteCreatePage.test.tsx`
  - `src/pages/AthleteDietPage.test.tsx`
  - `src/pages/AthleteDietsPanel.test.tsx`
  - `src/pages/DietDetailPage.test.tsx`
  - `src/pages/DietMealsPage.test.tsx`
  - `src/pages/TrainingPage.test.tsx`
  - `src/pages/training/PlanComparisonModal.test.tsx`
  - `src/pages/training/PlanOverviewView.test.tsx`
  - `src/pages/training/PlanVersionModal.test.tsx`
  - `src/pages/training/SessionEditorView.test.tsx`

## Referências de compatibilidade

- Vitest 4 requer Node 20+ e Vite 6+: https://v4.vitest.dev/guide/
- Vitest 5 requer Node 22.12+: https://vitest.dev/guide/
- React Testing Library requer `@testing-library/dom`: https://testing-library.com/docs/react-testing-library/intro/
