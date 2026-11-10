import { act, renderHook, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createElement, ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiClient } from "./api-client";
import {
  clearBrowserDeviceToken,
  createBrowserDeviceToken,
  detectDevicePlatform,
  getBrowserDeviceToken,
  saveBrowserDeviceToken,
  useNotificationPreferences,
  useRegisterNotificationDevice,
  useUnregisterNotificationDevice,
  useUpdateNotificationPreferences,
} from "./notifications";

vi.mock("./api-client", () => ({
  apiClient: {
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockedClient = vi.mocked(apiClient);

function wrapper({ children }: { children: ReactNode }) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return createElement(QueryClientProvider, { client: queryClient }, children);
}

beforeEach(() => {
  localStorage.clear();
});

describe("detectDevicePlatform (T-075)", () => {
  it.each([
    ["Mozilla/5.0 (Linux; Android 14; Pixel 8)", 5, "ANDROID"],
    ["Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X)", 5, "IOS"],
    ["Mozilla/5.0 (iPad; CPU OS 18_0 like Mac OS X)", 5, "IOS"],
    // iPadOS recente se apresenta como macOS; o toque diferencia de um Mac.
    ["Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)", 5, "IOS"],
    ["Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)", 0, "WEB"],
    ["Mozilla/5.0 (Windows NT 10.0; Win64; x64)", 0, "WEB"],
  ] as const)("maps %s (touch: %i) to %s", (userAgent, touchPoints, expected) => {
    expect(detectDevicePlatform(userAgent, touchPoints)).toBe(expected);
  });
});

describe("browser device token (T-075)", () => {
  it("creates distinct, provider-compatible tokens", () => {
    const first = createBrowserDeviceToken();
    const second = createBrowserDeviceToken();

    expect(first).toMatch(/^web-/);
    expect(first.length).toBeLessThanOrEqual(512);
    expect(first).not.toBe(second);
  });

  it("keeps the registered token per athlete and clears only the requested one", () => {
    saveBrowserDeviceToken("athlete-a", "web-a");
    saveBrowserDeviceToken("athlete-b", "web-b");

    expect(getBrowserDeviceToken("athlete-a")).toBe("web-a");
    expect(getBrowserDeviceToken("athlete-b")).toBe("web-b");

    clearBrowserDeviceToken("athlete-a");

    expect(getBrowserDeviceToken("athlete-a")).toBeNull();
    expect(getBrowserDeviceToken("athlete-b")).toBe("web-b");
  });
});

describe("notification preferences contract (T-075)", () => {
  it("loads GET /notifications/preferences", async () => {
    mockedClient.get.mockResolvedValue({
      data: { emailEnabled: true, pushEnabled: false },
    });

    const { result } = renderHook(() => useNotificationPreferences("athlete-1"), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(mockedClient.get).toHaveBeenCalledWith("/notifications/preferences");
    expect(result.current.data).toEqual({ emailEnabled: true, pushEnabled: false });
  });

  it("saves PUT /notifications/preferences with both channels", async () => {
    mockedClient.put.mockResolvedValue({
      data: { emailEnabled: false, pushEnabled: true },
    });
    const { result } = renderHook(() => useUpdateNotificationPreferences("athlete-1"), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ emailEnabled: false, pushEnabled: true });
    });

    expect(mockedClient.put).toHaveBeenCalledWith("/notifications/preferences", {
      emailEnabled: false,
      pushEnabled: true,
    });
  });
});

describe("push device contract (T-075)", () => {
  it("registers POST /notifications/devices with token and platform", async () => {
    mockedClient.post.mockResolvedValue({ data: undefined });
    const { result } = renderHook(() => useRegisterNotificationDevice(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ token: "web-123", platform: "WEB" });
    });

    expect(mockedClient.post).toHaveBeenCalledWith("/notifications/devices", {
      token: "web-123",
      platform: "WEB",
    });
  });

  it("removes DELETE /notifications/devices?token=", async () => {
    mockedClient.delete.mockResolvedValue({ data: undefined });
    const { result } = renderHook(() => useUnregisterNotificationDevice(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync("web-123");
    });

    expect(mockedClient.delete).toHaveBeenCalledWith("/notifications/devices", {
      params: { token: "web-123" },
    });
  });
});
