# ADR-0002: Soft-delete e AuditLog Append-only

- **Status:** Aceito
- **Data:** 2026-11-03

## Contexto

O ProCoach exige auditabilidade total das ações realizadas por coaches e pelo sistema (cadastros, alterações de status, aprovações, prescrições, convites e logins), além de conformidade futura com LGPD e preservação de histórico de atletas inativos ou removidos.

## Decisão

1. **Exclusão lógica (`soft-delete`):** entidades de domínio não sofrem `DELETE` físico no banco; a remoção marca o registro como excluído logicamente (`deleted = true` / `deletedAt`), ocultando-o das consultas padrão sem quebrar referências históricas.
2. **`AuditLog` append-only (`com.fitness.procoach.audit`):** operações de escrita relevantes registram um evento no `AuditLog` (autor, ação, entidade, timestamp e resumo antes/depois) **na mesma transação** da alteração de domínio via `AuditService`.
3. **Imutabilidade no banco:** no PostgreSQL, a tabela `audit_log` é protegida por triggers (migração `V802`) que rejeitam `UPDATE`, `DELETE` e `TRUNCATE`.

## Consequências

### Positivas
- Trilha de auditoria confiável e à prova de adulteração acidental ou lógica na aplicação.
- Histórico completo do atleta consultável pelo coach dono (`GET /api/v1/athletes/{id}/history`).
- Preservação de integridade referencial entre versões, prescrições e eventos passados.

### Negativas / Trade-offs
- Consultas de listagem e busca precisam filtrar explicitamente registros com `deleted = false`.
- Triggers específicos de PostgreSQL (`V802`) não são exercitados em testes com perfil `h2`, exigindo validação contínua na suíte Testcontainers/Postgres.

## Referências
- `AGENTS.md` (auditoria e exclusão)
- `backend/SPEC.md` (RF-10, RNF-01, RN-04, RN-05)
- `backend/TASKS.md` (T-013, T-015, V802)
