import { afterEach, describe, expect, it } from "vitest";
import {
  clearCachedInvitePath,
  getCachedInvitePath,
  getInviteErrorMessage,
  inviteUrl,
  rememberInviteLink,
} from "./invites";

afterEach(() => {
  sessionStorage.clear();
});

describe("getInviteErrorMessage", () => {
  it("maps ATHLETE_HAS_CREDENTIAL 409 error code to actionable Portuguese message", () => {
    const error = {
      response: {
        status: 409,
        data: {
          code: "ATHLETE_HAS_CREDENTIAL",
          message: "Este atleta já possui acesso",
        },
      },
    };

    expect(getInviteErrorMessage(error)).toBe(
      "Este atleta já possui uma credencial de acesso ativa no aplicativo."
    );
  });

  it("returns the generic conflict message for a 409 without a known code", () => {
    const error = {
      response: {
        status: 409,
        data: {
          message: "Conflito genérico",
        },
      },
    };

    expect(getInviteErrorMessage(error)).toBe(
      "Conflito ao gerar convite. O atleta já possui acesso ou o e-mail já está em uso."
    );
  });

  it("maps EMAIL_IN_USE 409 error code to actionable Portuguese message", () => {
    const error = {
      response: {
        status: 409,
        data: {
          code: "EMAIL_IN_USE",
          message: "E-mail já cadastrado: outro@exemplo.com",
        },
      },
    };

    expect(getInviteErrorMessage(error)).toBe(
      "Este e-mail já pertence a outro usuário cadastrado no sistema. Utilize outro e-mail para o convite."
    );
  });

  it("returns the generic conflict message for a 409 with an unknown code", () => {
    const error = {
      response: {
        status: 409,
        data: {
          code: "SOMETHING_ELSE",
          message: "E-mail já cadastrado: fulano@exemplo.com",
        },
      },
    };

    expect(getInviteErrorMessage(error)).toBe(
      "Conflito ao gerar convite. O atleta já possui acesso ou o e-mail já está em uso."
    );
  });

  it("falls back to generic error message for non-409 errors", () => {
    const error = {
      response: {
        status: 500,
        data: {
          message: "Internal server error",
        },
      },
    };

    expect(getInviteErrorMessage(error)).toBe(
      "Não foi possível gerar o convite. Tente novamente."
    );
  });
});

describe("invite cache helpers", () => {
  it("stores, retrieves, and clears cached invite paths", () => {
    const athleteId = "ath-123";
    expect(getCachedInvitePath(athleteId)).toBeNull();

    rememberInviteLink(athleteId, "/convite/xyz789");
    expect(getCachedInvitePath(athleteId)).toBe("/convite/xyz789");

    clearCachedInvitePath(athleteId);
    expect(getCachedInvitePath(athleteId)).toBeNull();
  });

  it("generates full invite URL from invitePath", () => {
    const url = inviteUrl({ invitePath: "/convite/abc123token" });
    expect(url).toContain("/convite/abc123token");
    expect(url.startsWith("http")).toBe(true);
  });
});
