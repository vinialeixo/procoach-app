/**
 * Decodifica o payload de um JWT SEM validar assinatura.
 *
 * Uso exclusivo: dar dicas de navegação no front (ex.: mandar o coach pra
 * /coach e o atleta pra /athlete). NUNCA usar isso para decidir permissão de
 * verdade — a autorização real (RBAC, RN-01, RF-11) é sempre validada no
 * núcleo Java a cada requisição.
 */
export type JwtPayload = {
  sub?: string;
  role?: "COACH" | "ATHLETE";
  exp?: number;
  [key: string]: unknown;
};

export function decodeJwtPayload(token: string): JwtPayload | null {
  try {
    const [, payload] = token.split(".");
    if (!payload) return null;
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const json = atob(normalized);
    return JSON.parse(json) as JwtPayload;
  } catch {
    return null;
  }
}
