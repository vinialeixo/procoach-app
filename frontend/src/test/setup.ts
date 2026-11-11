import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

// No Node 22/25/26+, `globalThis.localStorage` e `globalThis.sessionStorage`
// são getters nativos que exigem `--localstorage-file` e retornam `undefined`.
// Como `window` é um alias para `globalThis` no Vitest (`populateGlobal`) e
// `localStorage` não está na lista estática `KEYS` do Vitest 4, recuperamos a
// implementação real da instância JSDOM (`globalThis.jsdom.window`) para que os
// testes continuem funcionando tanto em Node 20 (CI) quanto em Node 22/26+.
const jsdomWindow = (
  globalThis as typeof globalThis & {
    jsdom?: { window: Window & typeof globalThis };
  }
).jsdom?.window;

if (jsdomWindow) {
  for (const key of ["localStorage", "sessionStorage"] as const) {
    Object.defineProperty(globalThis, key, {
      value: jsdomWindow[key],
      configurable: true,
      writable: true,
    });
  }
}

afterEach(() => {
  cleanup();
});
