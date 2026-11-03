// T-056 — Config do ESLint (flat config), compatível com ESLint 8.57.
// Reúne os plugins já presentes como devDependencies: @typescript-eslint,
// eslint-plugin-react-hooks e eslint-plugin-react-refresh. Sem regras de
// estilo/formatacao opinativas (isso e do Prettier/editor) — foco em
// corretude de React Hooks e erros comuns de TS.
import js from "@eslint/js";
import tsParser from "@typescript-eslint/parser";
import tsPlugin from "@typescript-eslint/eslint-plugin";
import reactHooks from "eslint-plugin-react-hooks";
import reactRefresh from "eslint-plugin-react-refresh";
import globals from "globals";

export default [
  { ignores: ["dist", "node_modules", "vite.config.*"] },
  js.configs.recommended,
  {
    files: ["src/**/*.{ts,tsx}", "vite.config.ts"],
    languageOptions: {
      parser: tsParser,
      parserOptions: { ecmaFeatures: { jsx: true } },
      globals: { ...globals.browser, ...globals.node },
    },
    plugins: {
      "@typescript-eslint": tsPlugin,
      "react-hooks": reactHooks,
      "react-refresh": reactRefresh,
    },
    rules: {
      // Base TS (sem type-checking, que o `tsc` ja faz no build).
      ...tsPlugin.configs.recommended.rules,
      // Hooks: deps de useEffect/useMemo corretas — evita bug sutil nas telas.
      ...reactHooks.configs.recommended.rules,
      // Fast Refresh: paginas/componentes exportam so componentes.
      "react-refresh/only-export-components": [
        "warn",
        { allowConstantExport: true },
      ],
      "@typescript-eslint/no-unused-vars": [
        "warn",
        { argsIgnorePattern: "^_", varsIgnorePattern: "^_" },
      ],
    },
  },
];
