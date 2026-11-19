# ADR-0003: Desativação Temporária de MFA/TOTP via Feature Flag

- **Status:** Aceito
- **Data:** 2026-11-03

## Contexto

A implementação inicial de MFA/TOTP (épico E9 / T-008) introduziu fluxo de 2 etapas no login e endpoints `/api/v1/auth/mfa/*`. Durante revisão de segurança, foram identificadas pendências que precisam ser endereçadas antes de expor MFA em produção: limite de tentativas por desafio, rate limit em `/login` e `/mfa/verify`, proteção anti-replay do código TOTP (armazenar último passo aceito), cifragem do segredo TOTP no banco (AES-GCM) e códigos de recuperação.

## Decisão

Desligar o MFA/TOTP por padrão através da feature flag `procoach.security.mfa.enabled` (variável `PROCOACH_MFA_ENABLED`, padrão `false`):

- Com a flag desligada, o `/login` autentica por e-mail e senha e os endpoints `/api/v1/auth/mfa/*` respondem `404`.
- O código de TOTP permanece no repositório atrás da flag para evitar retrabalho quando as pendências de hardening (`T-059` no backend, `T-060` no frontend) forem concluídas.

## Consequências

### Positivas
- Reduz fricção no onboarding e evita expor um fluxo MFA sem proteção contra brute-force e replay.
- Mantém a base de código pronta para reativação controlada após conclusão de `T-059` e `T-060`.

### Negativas / Trade-offs
- Contas operam temporariamente apenas com autenticação por e-mail/senha + JWT até a reativação do MFA endurecido.

## Referências
- `backend/SPEC.md` (RF-01, RNF-03)
- `backend/TASKS.md` (T-059)
- `frontend/TASKS.md` (T-060)
