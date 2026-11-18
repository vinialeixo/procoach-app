import { afterEach, describe, expect, it } from "vitest";
import { getAthleteId, isAuthenticated } from "./auth";

function saveToken(claims: Record<string, unknown>) {
  const payload = btoa(JSON.stringify(claims));
  localStorage.setItem("procoach.token", `header.${payload}.signature`);
}

afterEach(() => {
  localStorage.clear();
});

describe("getAthleteId", () => {
  it("returns the athlete claim for an athlete session", () => {
    saveToken({ role: "ATHLETE", athleteId: "athlete-123" });

    expect(getAthleteId()).toBe("athlete-123");
  });

  it("does not expose an athlete claim from a coach session", () => {
    saveToken({ role: "COACH", athleteId: "athlete-123" });

    expect(getAthleteId()).toBeNull();
  });

  it("returns null when there is no valid athlete session", () => {
    expect(getAthleteId()).toBeNull();

    localStorage.setItem("procoach.token", "not-a-jwt");
    expect(getAthleteId()).toBeNull();
  });
});

describe("isAuthenticated", () => {
  it("returns true for a valid non-expired token", () => {
    saveToken({ role: "COACH", exp: Math.floor(Date.now() / 1000) + 3600 });
    expect(isAuthenticated()).toBe(true);
  });

  it("returns false for an expired token when no refreshToken exists", () => {
    saveToken({ role: "COACH", exp: Math.floor(Date.now() / 1000) - 60 });
    expect(isAuthenticated()).toBe(false);
  });

  it("returns true for an expired token when a refreshToken is available", () => {
    saveToken({ role: "COACH", exp: Math.floor(Date.now() / 1000) - 60 });
    localStorage.setItem("procoach.refreshToken", "valid-refresh");
    expect(isAuthenticated()).toBe(true);
  });

  it("returns false when token is missing or malformed", () => {
    expect(isAuthenticated()).toBe(false);
    localStorage.setItem("procoach.token", "invalid-token");
    expect(isAuthenticated()).toBe(false);
  });
});
