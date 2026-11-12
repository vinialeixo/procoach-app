import { describe, expect, it } from "vitest";
import {
  ATHLETE_ACCESS_LABEL,
  getAthleteAccessStatus,
  type Athlete,
} from "./athletes";

describe("getAthleteAccessStatus", () => {
  it("returns HAS_ACCESS when athlete hasCredential is true", () => {
    const athlete: Pick<Athlete, "hasCredential" | "pendingInvite"> = {
      hasCredential: true,
      pendingInvite: null,
    };
    const status = getAthleteAccessStatus(athlete);

    expect(status).toBe("HAS_ACCESS");
    expect(ATHLETE_ACCESS_LABEL[status]).toBe("Tem acesso");
  });

  it("returns HAS_ACCESS when hasCredential is true even if pendingInvite is present", () => {
    const athlete: Pick<Athlete, "hasCredential" | "pendingInvite"> = {
      hasCredential: true,
      pendingInvite: {
        email: "atleta@exemplo.com",
        expiresAt: "2026-12-01T00:00:00Z",
      },
    };
    const status = getAthleteAccessStatus(athlete);

    expect(status).toBe("HAS_ACCESS");
  });

  it("returns INVITE_PENDING when hasCredential is false but pendingInvite exists", () => {
    const athlete: Pick<Athlete, "hasCredential" | "pendingInvite"> = {
      hasCredential: false,
      pendingInvite: {
        email: "convite@exemplo.com",
        expiresAt: "2026-12-01T00:00:00Z",
      },
    };
    const status = getAthleteAccessStatus(athlete);

    expect(status).toBe("INVITE_PENDING");
    expect(ATHLETE_ACCESS_LABEL[status]).toBe("Convite pendente");
  });

  it("returns NO_ACCESS when hasCredential is false and pendingInvite is null or undefined", () => {
    const athleteWithNull: Pick<Athlete, "hasCredential" | "pendingInvite"> = {
      hasCredential: false,
      pendingInvite: null,
    };
    expect(getAthleteAccessStatus(athleteWithNull)).toBe("NO_ACCESS");
    expect(ATHLETE_ACCESS_LABEL.NO_ACCESS).toBe("Sem acesso");

    const athleteWithUndefined: Pick<Athlete, "hasCredential" | "pendingInvite"> = {
      hasCredential: false,
      pendingInvite: undefined,
    };
    expect(getAthleteAccessStatus(athleteWithUndefined)).toBe("NO_ACCESS");
  });
});
