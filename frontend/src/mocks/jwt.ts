import type { Role } from "@/lib/auth";
import { decodeJwtPayload, type JwtPayload } from "@/lib/jwt";

export type MockTokenPayload = {
  sub: string;
  role: Role;
  athleteId?: string;
  email: string;
  name: string;
};

function toBase64Url(value: string): string {
  const bytes = new TextEncoder().encode(value);
  let binary = "";
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/**
 * Gera um JWT fake válido para o frontend (`decodeJwtPayload` em `src/lib/jwt.ts`).
 * Não possui assinatura real — serve apenas para o modo standalone com MSW.
 */
export function createMockJwt(payload: MockTokenPayload): string {
  const header = toBase64Url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const body = toBase64Url(
    JSON.stringify({
      ...payload,
      exp: Math.floor(Date.now() / 1000) + 60 * 60 * 24,
    })
  );
  return `${header}.${body}.mock-signature`;
}

export function extractBearerPayload(request: Request): JwtPayload | null {
  const authHeader = request.headers.get("Authorization");
  if (!authHeader?.startsWith("Bearer ")) {
    return null;
  }
  const token = authHeader.slice("Bearer ".length).trim();
  return decodeJwtPayload(token);
}
