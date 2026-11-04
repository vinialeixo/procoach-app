import { afterEach, describe, expect, it } from "vitest";
import { getAthleteId } from "./auth";

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
