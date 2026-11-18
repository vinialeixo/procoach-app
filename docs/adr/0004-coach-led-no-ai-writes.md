# ADR-0004: Plataforma Coach-Led e IA/RAG Estritamente Read-Only

- **Status:** Aceito
- **Data:** 2026-09-01

## Contexto

O ProCoach incorpora recursos assistivos baseados em IA/RAG (validação de dietas/treinos com citações científicas no MVP2 e importação de propostas de plano alimentar em JSON v1 via `T-088`). Existe o risco de automação excessiva alterar protocolos de atletas sem supervisão profissional ou permitir que um serviço de IA escreva diretamente no domínio.

## Decisão

A plataforma é estritamente **coach-led**:

1. **Decisão final do coach:** nenhuma funcionalidade de IA pode aprovar dietas, ativar planos de treino ou alterar dados de atletas autonomamente.
2. **RAG consultivo e read-only:** o serviço Python de RAG (planejado para MVP2 via gRPC read-only) não possui credenciais nem endpoints de escrita no domínio; ele apenas retorna sugestões, justificativas e citações.
3. **Propostas de IA como rascunho:** contratos de importação de plano gerado por IA (`AiPlanProposal`, T-088) persistem exclusivamente como `DRAFT` (`requiresCoachApproval = true`) e registram evento `AI_PLAN_PROPOSED` no `AuditLog`, exigindo revisão e aprovação explícita do coach.

## Consequências

### Positivas
- Garante responsabilidade técnica e clínica do profissional em todas as prescrições.
- Isola falhas ou alucinações de IA, impedindo impacto direto em protocolos ativos ou dados do atleta.

### Negativas / Trade-offs
- Exige uma etapa explícita de revisão/aprovação humana mesmo quando a sugestão da IA estiver correta.

## Referências
- `AGENTS.md` (regra de decisão do coach)
- `backend/SPEC.md` (Seção 1, RF-06, RNF-04, RN-03)
- `docs/nutricao-plano-alimentar.md`
- `backend/TASKS.md` (T-088, M2-014, M2-016)
