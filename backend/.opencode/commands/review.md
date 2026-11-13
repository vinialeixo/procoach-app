---
description: Revisa o diff atual contra as regras inegociáveis do ProCoach
agent: reviewer
---

Revise o diff atual contra as regras inegociáveis do AGENTS.md.

Diff:
!`git diff HEAD`

Checklist (responda só o que for violação ou dúvida):
1. Algum update/overwrite de Diet, TrainingPlan ou Assessment em vez de nova versão?
2. Toda escrita gera AuditLog? Algum update/delete em AuditLog?
3. DELETE físico em vez de soft-delete?
4. Anotações Spring/JPA dentro de `*/domain`?
5. Referência entre agregados por objeto em vez de UUID?
6. `coachId` aceito do body/params em vez de resolvido do JWT? (risco de IDOR)
7. Endpoint de atleta sem `@athleteGuard.canAccess` / `@PreAuthorize`?
8. Acesso a classes internas de outro módulo (boundary do Spring Modulith)?
9. Testes cobrem os critérios de aceite das US citadas em `docs/US_MAP.md`?
10. Algo fora do escopo da task (refactor não pedido, outras tasks marcadas)?

Formato: lista curta por severidade (Bloqueante / Atenção / Sugestão), com arquivo:linha.
