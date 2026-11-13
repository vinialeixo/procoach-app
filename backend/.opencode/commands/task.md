---
description: Implementa uma task do TASKS.md (uso - /task T-018)
---

Implemente a task **$ARGUMENTS** seguindo o processo de "Backlog e como trabalhar tasks" do AGENTS.md.

Economia de contexto (importante):
- Localize SÓ a linha da task em TASKS.md (grep pelo ID). Não leia o arquivo inteiro.
- Consulte `docs/US_MAP.md` para saber quais US reais (US-0XX) valem para a task e leia SÓ essas seções em USER_STORIES.md.
- Do UML, leia só a seção do agregado que você vai tocar.

Passos:
1. Confira a coluna "Depende de". Se alguma dependência estiver `[ ]`, PARE e me avise.
2. Releia as regras inegociáveis do AGENTS.md (inclusive: `coachId` vem sempre do JWT, nunca do body/params).
3. Implemente em camadas (domain -> application -> infrastructure -> interfaces), só o que a task pede.
4. Escreva testes cobrindo os critérios de aceite das US, priorizando versionamento/auditoria.
5. Rode o gate (`/gate`) e corrija falhas, incluindo boundaries do Spring Modulith. Máximo 3 tentativas; se não passar, pare e explique.
6. Marque `[x]` apenas na linha desta task em TASKS.md.
7. Resuma em poucas linhas o que mudou e o que ficou de fora.

Escopo: faça somente esta task. Não refatore código não relacionado e não mexa em outras tasks.
Se a task conflitar com uma regra inegociável, pare e pergunte.
Se a task não existir em TASKS.md, avise em vez de inventar.
