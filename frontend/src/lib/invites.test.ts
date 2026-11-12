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

  it("maps legacy 409 message with 'já possui acesso' when code is omitted", () => {
    const error = {
      response: {
        status: 409,
        data: {
          message: "Este atleta já possui acesso",
        },
      },
    };

    expect(getInviteErrorMessage(error)).toBe(
      "Este atleta já possui uma credencial de acesso ativa no aplicativo."
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

  it("maps legacy 409 message with 'já cadastrado' when code is omitted", () => {
    const error = {
      response: {
        status: 409,
        data: {
          message: "E-mail já cadastrado: fulano@exemplo.com",
        },
      },
    };

    expect(getInviteErrorMessage(error)).toBe(
      "Este e-mail já pertence a outro usuário cadastrado no sistema. Utilize outro e-mail para o convite."
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
