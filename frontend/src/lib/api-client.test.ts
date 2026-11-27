import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from "vitest";
import { http, HttpResponse } from "msw";
import { setupServer } from "msw/node";
import {
  apiClient,
  REFRESH_TOKEN_STORAGE_KEY,
  resetRefreshStateForTests,
  setSessionExpiredHandler,
  TOKEN_STORAGE_KEY,
} from "./api-client";
import { queryClient } from "./query-client";

const server = setupServer();

beforeAll(() => server.listen({ onUnhandledFrame: "error" }));
afterEach(() => {
  server.resetHandlers();
  localStorage.clear();
  queryClient.clear();
  resetRefreshStateForTests();
});
afterAll(() => server.close());

describe("apiClient 401 refresh interceptor", () => {
  it("refreshes the token on 401 and retries the original request once", async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, "expired-access");
    localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, "valid-refresh");

    let protectedCalls = 0;
    let refreshBody: unknown = null;

    server.use(
      http.get("/api/v1/athletes", ({ request }) => {
        protectedCalls += 1;
        if (request.headers.get("Authorization") === "Bearer new-access") {
          return HttpResponse.json({ items: ["ok"] });
        }
        return HttpResponse.json({ code: "UNAUTHORIZED" }, { status: 401 });
      }),
      http.post("/api/v1/auth/refresh", async ({ request }) => {
        refreshBody = await request.json();
        return HttpResponse.json({
          accessToken: "new-access",
          refreshToken: "new-refresh",
        });
      })
    );

    const response = await apiClient.get<{ items: string[] }>("/athletes");

    expect(response.data).toEqual({ items: ["ok"] });
    expect(protectedCalls).toBe(2);
    expect(refreshBody).toEqual({ refreshToken: "valid-refresh" });
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBe("new-access");
    expect(localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY)).toBe("new-refresh");
  });

  it("queues concurrent 401 requests so /auth/refresh is called only once", async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, "expired-access");
    localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, "valid-refresh");

    let refreshCalls = 0;

    server.use(
      http.get("/api/v1/resource/:id", ({ params, request }) => {
        if (request.headers.get("Authorization") === "Bearer fresh-access") {
          return HttpResponse.json({ id: params.id });
        }
        return HttpResponse.json({ code: "UNAUTHORIZED" }, { status: 401 });
      }),
      http.post("/api/v1/auth/refresh", async () => {
        refreshCalls += 1;
        await new Promise((resolve) => setTimeout(resolve, 20));
        return HttpResponse.json({
          accessToken: "fresh-access",
          refreshToken: "fresh-refresh",
        });
      })
    );

    const results = await Promise.all([
      apiClient.get<{ id: string }>("/resource/1"),
      apiClient.get<{ id: string }>("/resource/2"),
      apiClient.get<{ id: string }>("/resource/3"),
    ]);

    expect(refreshCalls).toBe(1);
    expect(results.map((r) => r.data.id)).toEqual(["1", "2", "3"]);
  });

  it("does not attempt refresh on /auth/login or /auth/refresh 401", async () => {
    localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, "some-refresh");
    let refreshCalls = 0;

    server.use(
      http.post("/api/v1/auth/login", () =>
        HttpResponse.json({ code: "INVALID_CREDENTIALS" }, { status: 401 })
      ),
      http.post("/api/v1/auth/refresh", () => {
        refreshCalls += 1;
        return HttpResponse.json({ code: "INVALID_REFRESH" }, { status: 401 });
      })
    );

    await expect(
      apiClient.post("/auth/login", { email: "a@b.com", password: "wrong" })
    ).rejects.toThrow();

    expect(refreshCalls).toBe(0);
  });

  it("clears tokens, clears QueryClient cache, and triggers session expired redirect when refresh fails", async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, "expired-access");
    localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, "expired-refresh");
    queryClient.setQueryData(["athletes"], [{ id: "1" }]);

    const clearSpy = vi.spyOn(queryClient, "clear");
    const onSessionExpired = vi.fn();
    setSessionExpiredHandler(onSessionExpired);

    server.use(
      http.get("/api/v1/athletes", () =>
        HttpResponse.json({ code: "UNAUTHORIZED" }, { status: 401 })
      ),
      http.post("/api/v1/auth/refresh", () =>
        HttpResponse.json({ code: "INVALID_REFRESH" }, { status: 401 })
      )
    );

    await expect(apiClient.get("/athletes")).rejects.toThrow();

    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
    expect(localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY)).toBeNull();
    expect(clearSpy).toHaveBeenCalled();
    expect(queryClient.getQueryData(["athletes"])).toBeUndefined();
    expect(onSessionExpired).toHaveBeenCalledTimes(1);
  });

  it("clears tokens, clears QueryClient cache, and redirects when no refreshToken is stored", async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, "expired-access");
    queryClient.setQueryData(["diets"], [{ id: "d1" }]);

    const clearSpy = vi.spyOn(queryClient, "clear");
    const onSessionExpired = vi.fn();
    setSessionExpiredHandler(onSessionExpired);

    let refreshCalls = 0;
    server.use(
      http.get("/api/v1/diets", () =>
        HttpResponse.json({ code: "UNAUTHORIZED" }, { status: 401 })
      ),
      http.post("/api/v1/auth/refresh", () => {
        refreshCalls += 1;
        return HttpResponse.json({ accessToken: "x" });
      })
    );

    await expect(apiClient.get("/diets")).rejects.toThrow();

    expect(refreshCalls).toBe(0);
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
    expect(clearSpy).toHaveBeenCalled();
    expect(onSessionExpired).toHaveBeenCalledTimes(1);
  });

  it("does not loop infinitely if the retried request also returns 401", async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, "expired-access");
    localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, "valid-refresh");

    const onSessionExpired = vi.fn();
    setSessionExpiredHandler(onSessionExpired);

    let protectedCalls = 0;
    let refreshCalls = 0;

    server.use(
      http.get("/api/v1/athletes", () => {
        protectedCalls += 1;
        return HttpResponse.json({ code: "UNAUTHORIZED" }, { status: 401 });
      }),
      http.post("/api/v1/auth/refresh", () => {
        refreshCalls += 1;
        return HttpResponse.json({
          accessToken: "still-rejected",
          refreshToken: "next-refresh",
        });
      })
    );

    await expect(apiClient.get("/athletes")).rejects.toThrow();

    expect(refreshCalls).toBe(1);
    expect(protectedCalls).toBe(2);
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
    expect(localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY)).toBeNull();
    expect(onSessionExpired).toHaveBeenCalledTimes(1);
  });
});
