/* global self, caches, fetch, URL */
const STATIC_CACHE = "procoach-static-v1";
const API_CACHE = "procoach-api-v1";

// Endpoints de leitura do atleta elegíveis para cache offline (treino e dieta atuais)
const OFFLINE_API_PREFIXES = [
  "/api/v1/diets",
  "/api/training/plans/athlete/",
  "/api/training/trainings/athlete/",
  "/api/v1/assessments",
];

self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE).then((cache) => cache.addAll(["/", "/manifest.webmanifest", "/icon.svg"]))
  );
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys
          .filter((key) => key !== STATIC_CACHE && key !== API_CACHE)
          .map((key) => caches.delete(key))
      )
    )
  );
  self.clients.claim();
});

function isCacheableAthleteApi(url) {
  return OFFLINE_API_PREFIXES.some((prefix) => url.pathname.startsWith(prefix));
}

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;

  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;
  if (url.pathname.startsWith("/api/v1/auth/")) return;

  // Network-first com fallback em cache para dados clínicos de leitura do atleta
  if (isCacheableAthleteApi(url)) {
    event.respondWith(
      fetch(request)
        .then((response) => {
          if (response.ok) {
            const copy = response.clone();
            caches.open(API_CACHE).then((cache) => cache.put(request, copy));
          }
          return response;
        })
        .catch(() => caches.match(request))
    );
    return;
  }

  // Navegação SPA: network-first com fallback para o shell "/"
  if (request.mode === "navigate") {
    event.respondWith(
      fetch(request).catch(() => caches.match("/"))
    );
    return;
  }

  // Assets estáticos (/assets/*, ícones): stale-while-revalidate
  if (url.pathname.startsWith("/assets/") || url.pathname === "/icon.svg") {
    event.respondWith(
      caches.match(request).then((cached) => {
        const networkFetch = fetch(request).then((response) => {
          if (response.ok) {
            const copy = response.clone();
            caches.open(STATIC_CACHE).then((cache) => cache.put(request, copy));
          }
          return response;
        });
        return cached || networkFetch;
      })
    );
  }
});
