# ProCoach — User Stories (Backlog Produto)

> Fonte: planilha de backlog de produto (importado em 2026-09-01). Cada US tem ID, épico, critérios de aceite, prioridade MoSCoW, estimativa em pontos, sprint sugerida e os RF/BR do `SPEC.md`/`AGENT.md` correspondentes.

> **Nota de reconciliação**: a planilha de *tasks* (`TASKS.md`) referencia User Stories como `US-01`, `US-02`... (numeração curta, por épico) enquanto esta planilha usa `US-001`...`US-042` (numeração sequencial global). As duas numerações **não são idênticas** — os IDs aqui (`US-XXX`) são a referência oficial de critérios de aceite; ao trabalhar uma task, confirme a US certa pelo **épico + descrição**, não só pelo número.


## E1 – Gestão de Atletas

| ID | User Story | Prioridade | Pts | Sprint | RF | BR |
|---|---|---|---|---|---|---|
| US-001 | Como coach, quero cadastrar um atleta informando nome, sexo, data de nascimento, altura, peso e objetivo, para iniciar o acompanhamento. | Must Have | 3 | Sprint 1-2 | RF-01 | BR-100 |
| US-002 | Como coach, quero que cada atleta tenha um identificador único e imutável, para garantir rastreabilidade. | Must Have | 3 | Sprint 1-2 | RF-02 | BR-101 |
| US-003 | Como coach, quero visualizar todos os atletas vinculados a mim, para gerenciar minha carteira. | Must Have | 3 | Sprint 1-2 | RF-03 | BR-001 |
| US-004 | Como coach, quero que um atleta possa ter múltiplos protocolos simultâneos, para atender diferentes objetivos. | Must Have | 5 | Sprint 1-2 | RF-04 | BR-102 |
| US-005 | Como coach, quero alterar o estado do atleta (Ativo, Pausado, Finalizado), para refletir a situação real do acompanhamento. | Must Have | 5 | Sprint 1-2 | RF-05 | — |
| US-006 | Como coach, quero que o histórico do atleta nunca seja apagado, para garantir auditabilidade completa. | Must Have | 5 | Sprint 1-2 | RF-06 | BR-103 |

<details><summary>Critérios de aceitação — E1 – Gestão de Atletas</summary>


**US-001** (MVP v1.0) — RF-01 / BR-100

Como coach, quero cadastrar um atleta informando nome, sexo, data de nascimento, altura, peso e objetivo, para iniciar o acompanhamento.


Critérios de aceite: 1) Todos os campos obrigatórios (nome, sexo, data nasc., altura, peso, objetivo) devem ser validados antes de salvar. 2) O sistema deve exibir mensagem de sucesso após cadastro. 3) Dados salvos devem aparecer na listagem de atletas.


**US-002** (MVP v1.0) — RF-02 / BR-101

Como coach, quero que cada atleta tenha um identificador único e imutável, para garantir rastreabilidade.


Critérios de aceite: 1) O ID deve ser gerado automaticamente pelo sistema. 2) O ID não pode ser alterado ou reutilizado após criação. 3) O ID deve ser visível na ficha do atleta.


**US-003** (MVP v1.0) — RF-03 / BR-001

Como coach, quero visualizar todos os atletas vinculados a mim, para gerenciar minha carteira.


Critérios de aceite: 1) A listagem deve exibir nome, status e objetivo de cada atleta. 2) Deve ser possível filtrar por status (Ativo, Pausado, Finalizado). 3) Apenas atletas vinculados ao coach logado devem aparecer.


**US-004** (MVP v1.0) — RF-04 / BR-102

Como coach, quero que um atleta possa ter múltiplos protocolos simultâneos, para atender diferentes objetivos.


Critérios de aceite: 1) O sistema deve permitir criar mais de um protocolo ativo por atleta. 2) Cada protocolo deve ter objetivo independente. 3) A listagem de protocolos deve indicar qual está ativo.


**US-005** (MVP v1.0) — RF-05

Como coach, quero alterar o estado do atleta (Ativo, Pausado, Finalizado), para refletir a situação real do acompanhamento.


Critérios de aceite: 1) Apenas os estados Ativo, Pausado e Finalizado devem estar disponíveis. 2) A mudança de estado deve ser registrada com data e autor. 3) Atletas finalizados não devem aceitar novos protocolos.


**US-006** (MVP v1.0) — RF-06 / BR-103

Como coach, quero que o histórico do atleta nunca seja apagado, para garantir auditabilidade completa.


Critérios de aceite: 1) Nenhuma exclusão física de dados deve ser permitida. 2) Exclusões devem ser lógicas (soft delete). 3) O histórico completo deve ser acessível a qualquer momento.


</details>


## E2 – Avaliação Física

| ID | User Story | Prioridade | Pts | Sprint | RF | BR |
|---|---|---|---|---|---|---|
| US-007 | Como coach, quero registrar uma avaliação física com data obrigatória, para organizar o histórico cronológico. | Must Have | 3 | Sprint 3-4 | RF-07 | BR-200 |
| US-008 | Como coach, quero que o sistema armazene todas as avaliações sem sobrescrever as anteriores, para manter o histórico completo. | Must Have | 5 | Sprint 3-4 | RF-08 | BR-201 |
| US-009 | Como coach, quero comparar duas ou mais avaliações lado a lado, para analisar a evolução do atleta. | Must Have | 5 | Sprint 3-4 | RF-10 | BR-202 |
| US-010 | Como coach, quero que o sistema calcule automaticamente IMC, TMB, GET, evolução de peso e percentual, para agilizar a análise. | Must Have | 5 | Sprint 3-4 | RF-09 | BR-203 |

<details><summary>Critérios de aceitação — E2 – Avaliação Física</summary>


**US-007** (MVP v1.0) — RF-07 / BR-200

Como coach, quero registrar uma avaliação física com data obrigatória, para organizar o histórico cronológico.


Critérios de aceite: 1) A data da avaliação deve ser campo obrigatório. 2) Não deve ser possível salvar avaliação sem data. 3) Avaliações devem ser listadas em ordem cronológica.


**US-008** (MVP v1.0) — RF-08 / BR-201

Como coach, quero que o sistema armazene todas as avaliações sem sobrescrever as anteriores, para manter o histórico completo.


Critérios de aceite: 1) Cada nova avaliação deve criar um novo registro. 2) Avaliações anteriores devem permanecer inalteradas. 3) Deve ser possível acessar qualquer avaliação passada.


**US-009** (MVP v1.0) — RF-10 / BR-202

Como coach, quero comparar duas ou mais avaliações lado a lado, para analisar a evolução do atleta.


Critérios de aceite: 1) O coach deve poder selecionar 2+ avaliações para comparação. 2) Os dados devem ser exibidos lado a lado com destaque nas diferenças. 3) A comparação deve incluir todas as métricas registradas.


**US-010** (MVP v1.0) — RF-09 / BR-203

Como coach, quero que o sistema calcule automaticamente IMC, TMB, GET, evolução de peso e percentual, para agilizar a análise.


Critérios de aceite: 1) Os cálculos devem ser executados automaticamente ao salvar a avaliação. 2) As fórmulas utilizadas devem ser cientificamente validadas. 3) Os resultados devem ser exibidos junto à avaliação.


</details>


## E3 – Nutrição/Dietas

| ID | User Story | Prioridade | Pts | Sprint | RF | BR |
|---|---|---|---|---|---|---|
| US-011 | Como coach, quero criar uma dieta com objetivo definido (cutting, hipertrofia etc.), para alinhar o protocolo ao objetivo do atleta. | Must Have | 3 | Sprint 5-7 | RF-11 | BR-300 |
| US-012 | Como coach, quero que o sistema calcule automaticamente TMB, GET, déficit e superávit, para embasar as prescrições nutricionais. | Must Have | 5 | Sprint 5-7 | RF-12 | BR-301 |
| US-013 | Como coach, quero receber sugestões de macronutrientes geradas pela IA, para otimizar minhas prescrições com base em evidências. | Should Have | 8 | Sprint 5-7 | RF-15 | BR-302 |
| US-014 | Como coach, quero que toda dieta registre calorias, proteínas, carboidratos e gorduras, para garantir completude do protocolo. | Must Have | 3 | Sprint 5-7 | RF-13 | BR-303 |
| US-015 | Como coach, quero criar múltiplas versões da dieta (v1→v2→v3) com histórico encadeado, para rastrear todas as alterações. | Must Have | 5 | Sprint 5-7 | RF-14 | BR-304 |
| US-016 | Como coach, quero que toda alteração na dieta registre quem alterou, quando e o motivo, para garantir auditabilidade. | Must Have | 5 | Sprint 5-7 | RF-16 | BR-305 |
| US-017 | Como coach, quero gerenciar os estados da dieta (Rascunho, Em Revisão, Aprovada, Arquivada), para controlar o fluxo de aprovação. | Must Have | 5 | Sprint 5-7 | RF-17 | — |

<details><summary>Critérios de aceitação — E3 – Nutrição/Dietas</summary>


**US-011** (MVP v1.0) — RF-11 / BR-300

Como coach, quero criar uma dieta com objetivo definido (cutting, hipertrofia etc.), para alinhar o protocolo ao objetivo do atleta.


Critérios de aceite: 1) O objetivo deve ser selecionado ao criar a dieta. 2) Os objetivos disponíveis devem incluir cutting, hipertrofia, manutenção etc. 3) A dieta deve ser vinculada ao atleta e ao protocolo.


**US-012** (MVP v1.0) — RF-12 / BR-301

Como coach, quero que o sistema calcule automaticamente TMB, GET, déficit e superávit, para embasar as prescrições nutricionais.


Critérios de aceite: 1) Os cálculos devem considerar sexo, idade, peso e nível de atividade. 2) Os valores devem ser recalculados ao alterar dados da avaliação. 3) Os resultados devem ser visíveis na tela de criação da dieta.


**US-013** (MVP v1.1) — RF-15 / BR-302

Como coach, quero receber sugestões de macronutrientes geradas pela IA, para otimizar minhas prescrições com base em evidências.


Critérios de aceite: 1) A IA deve sugerir distribuição de macros com base no objetivo e perfil do atleta. 2) As sugestões devem incluir justificativa científica. 3) O coach deve poder aceitar, modificar ou rejeitar as sugestões.


**US-014** (MVP v1.0) — RF-13 / BR-303

Como coach, quero que toda dieta registre calorias, proteínas, carboidratos e gorduras, para garantir completude do protocolo.


Critérios de aceite: 1) Os campos de macronutrientes devem ser obrigatórios. 2) O sistema deve validar valores numéricos positivos. 3) Os totais devem ser exibidos automaticamente.


**US-015** (MVP v1.0) — RF-14 / BR-304

Como coach, quero criar múltiplas versões da dieta (v1→v2→v3) com histórico encadeado, para rastrear todas as alterações.


Critérios de aceite: 1) Cada nova versão deve manter referência à anterior. 2) Versões anteriores devem ser somente leitura. 3) Deve ser possível comparar versões lado a lado.


**US-016** (MVP v1.0) — RF-16 / BR-305

Como coach, quero que toda alteração na dieta registre quem alterou, quando e o motivo, para garantir auditabilidade.


Critérios de aceite: 1) O sistema deve registrar autor, data/hora e justificativa da alteração. 2) O log deve ser imutável. 3) O histórico de alterações deve ser acessível na ficha da dieta.


**US-017** (MVP v1.0) — RF-17

Como coach, quero gerenciar os estados da dieta (Rascunho, Em Revisão, Aprovada, Arquivada), para controlar o fluxo de aprovação.


Critérios de aceite: 1) Os estados devem seguir o fluxo: Rascunho → Em Revisão → Aprovada → Arquivada. 2) Transições inválidas devem ser bloqueadas. 3) A mudança de estado deve ser registrada no log de auditoria.


</details>


## E4 – Treinamento

| ID | User Story | Prioridade | Pts | Sprint | RF | BR |
|---|---|---|---|---|---|---|
| US-018 | Como coach, quero criar um treino vinculado a uma periodização, para organizar o plano de treinamento do atleta. | Must Have | 5 | Sprint 8-9 | RF-18 | BR-400 |
| US-019 | Como coach, quero adicionar exercícios com séries, repetições, carga, intensidade e descanso, para detalhar o treino. | Must Have | 3 | Sprint 8-9 | RF-19 | BR-401 |
| US-020 | Como coach, quero visualizar informações científicas de cada exercício, para embasar minhas decisões. | Must Have | 3 | Sprint 8-9 | RF-20 | BR-402 |
| US-021 | Como coach, quero criar exercícios personalizados, para adaptar o treino às necessidades específicas do atleta. | Must Have | 3 | Sprint 8-9 | RF-21 | BR-403 |
| US-022 | Como coach, quero receber sugestões de exercícios semelhantes do sistema, para facilitar substituições. | Should Have | 8 | Sprint 8-9 | RF-22 | BR-404 |
| US-023 | Como coach, quero gerenciar os estados do treino (Rascunho, Em Revisão, Ativo, Finalizado), para controlar o fluxo de aprovação. | Must Have | 5 | Sprint 8-9 | RF-23 | — |

<details><summary>Critérios de aceitação — E4 – Treinamento</summary>


**US-018** (MVP v1.0) — RF-18 / BR-400

Como coach, quero criar um treino vinculado a uma periodização, para organizar o plano de treinamento do atleta.


Critérios de aceite: 1) O treino deve ser obrigatoriamente vinculado a uma periodização. 2) A periodização deve ter data de início e fim. 3) Deve ser possível visualizar o cronograma completo.


**US-019** (MVP v1.0) — RF-19 / BR-401

Como coach, quero adicionar exercícios com séries, repetições, carga, intensidade e descanso, para detalhar o treino.


Critérios de aceite: 1) Todos os parâmetros (séries, reps, carga, intensidade, descanso) devem ser configuráveis. 2) Deve ser possível reordenar exercícios no treino. 3) O sistema deve validar valores numéricos positivos.


**US-020** (MVP v1.0) — RF-20 / BR-402

Como coach, quero visualizar informações científicas de cada exercício, para embasar minhas decisões.


Critérios de aceite: 1) As informações devem incluir biomecânica, evidências e contraindicações. 2) Os dados devem vir da Knowledge Base ou Biblioteca de Exercícios. 3) As referências bibliográficas devem ser exibidas.


**US-021** (MVP v1.0) — RF-21 / BR-403

Como coach, quero criar exercícios personalizados, para adaptar o treino às necessidades específicas do atleta.


Critérios de aceite: 1) O coach deve poder adicionar exercícios não presentes na biblioteca. 2) Exercícios personalizados devem ter os mesmos campos obrigatórios. 3) Exercícios personalizados devem ser salvos no perfil do coach.


**US-022** (MVP v1.1) — RF-22 / BR-404

Como coach, quero receber sugestões de exercícios semelhantes do sistema, para facilitar substituições.


Critérios de aceite: 1) O sistema deve sugerir exercícios com grupos musculares similares. 2) As sugestões devem considerar equipamento disponível. 3) O coach deve poder aceitar ou ignorar as sugestões.


**US-023** (MVP v1.0) — RF-23

Como coach, quero gerenciar os estados do treino (Rascunho, Em Revisão, Ativo, Finalizado), para controlar o fluxo de aprovação.


Critérios de aceite: 1) Os estados devem seguir fluxo: Rascunho → Em Revisão → Ativo → Finalizado. 2) Transições inválidas devem ser bloqueadas. 3) A mudança de estado deve ser registrada no log.


</details>


## E5 – Check-ins

| ID | User Story | Prioridade | Pts | Sprint | RF | BR |
|---|---|---|---|---|---|---|
| US-024 | Como coach, quero registrar check-ins com data obrigatória, para acompanhar a evolução cronológica. | Should Have | 3 | Sprint 11 | RF-24 | BR-500 |
| US-025 | Como coach, quero anexar fotos, peso, medidas e observações ao check-in, para registrar a evolução completa do atleta. | Should Have | 5 | Sprint 11 | RF-25 | BR-501 |
| US-026 | Como coach, quero que a IA gere uma análise comparativa entre check-ins, para identificar tendências rapidamente. | Should Have | 8 | Sprint 11 | RF-26 | BR-502 |
| US-027 | Como coach, quero garantir que nenhum ajuste de protocolo ocorra automaticamente, para manter o controle das decisões. | Should Have | 3 | Sprint 11 | RF-27 | BR-503 |

<details><summary>Critérios de aceitação — E5 – Check-ins</summary>


**US-024** (MVP v1.1) — RF-24 / BR-500

Como coach, quero registrar check-ins com data obrigatória, para acompanhar a evolução cronológica.


Critérios de aceite: 1) A data deve ser campo obrigatório. 2) Check-ins devem ser listados em ordem cronológica. 3) Não deve ser possível alterar a data após registro.


**US-025** (MVP v1.1) — RF-25 / BR-501

Como coach, quero anexar fotos, peso, medidas e observações ao check-in, para registrar a evolução completa do atleta.


Critérios de aceite: 1) O sistema deve aceitar upload de fotos (JPEG/PNG). 2) Peso e medidas devem ser campos numéricos validados. 3) O campo de observações deve aceitar texto livre.


**US-026** (MVP v1.1) — RF-26 / BR-502

Como coach, quero que a IA gere uma análise comparativa entre check-ins, para identificar tendências rapidamente.


Critérios de aceite: 1) A análise deve comparar métricas entre check-ins consecutivos. 2) Tendências (melhora/piora/estabilidade) devem ser destacadas. 3) A análise deve ser apresentada com gráficos e texto.


**US-027** (MVP v1.1) — RF-27 / BR-503

Como coach, quero garantir que nenhum ajuste de protocolo ocorra automaticamente, para manter o controle das decisões.


Critérios de aceite: 1) Nenhuma alteração automática de protocolo deve ser executada. 2) Sugestões de ajuste devem ser apresentadas para aprovação. 3) O coach deve confirmar manualmente qualquer mudança.


</details>


## E6 – IA Assistiva

| ID | User Story | Prioridade | Pts | Sprint | RF | BR |
|---|---|---|---|---|---|---|
| US-028 | Como coach, quero que a IA apenas sugira alterações, nunca as execute, para manter minha autoridade sobre os protocolos. | Should Have | 8 | Sprint 12-13 | RF-28 | BR-600 |
| US-029 | Como coach, quero que toda sugestão da IA inclua justificativa e origem (KB, regra ou evidência), para confiar nas recomendações. | Should Have | 8 | Sprint 12-13 | RF-29 | BR-601 |
| US-030 | Como coach, quero que a IA cite artigos, livros e consensos nas sugestões, para embasar minhas decisões em evidências. | Should Have | 8 | Sprint 12-13 | RF-30 | BR-602 |
| US-031 | Como coach, quero que a IA apresente diferentes perspectivas em caso de evidências conflitantes, para tomar decisões informadas. | Should Have | 8 | Sprint 12-13 | RF-31 | BR-603 |
| US-032 | Como coach, quero aceitar, modificar ou recusar qualquer sugestão da IA, para manter controle total sobre os protocolos. | Should Have | 5 | Sprint 12-13 | RF-32 | BR-004, BR-005 |

<details><summary>Critérios de aceitação — E6 – IA Assistiva</summary>


**US-028** (MVP v2.0) — RF-28 / BR-600

Como coach, quero que a IA apenas sugira alterações, nunca as execute, para manter minha autoridade sobre os protocolos.


Critérios de aceite: 1) A IA nunca deve alterar dados diretamente. 2) Todas as ações da IA devem ser propostas para aprovação. 3) O sistema deve bloquear qualquer tentativa de escrita direta pela IA.


**US-029** (MVP v2.0) — RF-29 / BR-601

Como coach, quero que toda sugestão da IA inclua justificativa e origem (KB, regra ou evidência), para confiar nas recomendações.


Critérios de aceite: 1) Cada sugestão deve exibir a fonte (KB, regra, evidência). 2) A justificativa deve ser clara e compreensível. 3) Deve ser possível acessar a referência original.


**US-030** (MVP v2.0) — RF-30 / BR-602

Como coach, quero que a IA cite artigos, livros e consensos nas sugestões, para embasar minhas decisões em evidências.


Critérios de aceite: 1) As citações devem incluir título, autor e ano. 2) Devem ser referências de fontes reconhecidas. 3) O coach deve poder acessar o link ou DOI quando disponível.


**US-031** (MVP v2.0) — RF-31 / BR-603

Como coach, quero que a IA apresente diferentes perspectivas em caso de evidências conflitantes, para tomar decisões informadas.


Critérios de aceite: 1) Evidências conflitantes devem ser apresentadas lado a lado. 2) O nível de confiança de cada evidência deve ser indicado. 3) O coach deve escolher qual perspectiva adotar.


**US-032** (MVP v2.0) — RF-32 / BR-004, BR-005

Como coach, quero aceitar, modificar ou recusar qualquer sugestão da IA, para manter controle total sobre os protocolos.


Critérios de aceite: 1) Cada sugestão deve ter botões de aceitar, modificar e recusar. 2) A decisão do coach deve ser registrada no log. 3) Sugestões recusadas devem registrar o motivo.


</details>


## E7 – Knowledge Base

| ID | User Story | Prioridade | Pts | Sprint | RF | BR |
|---|---|---|---|---|---|---|
| US-033 | Como coach, quero consultar conceitos com definição, categoria, referências e nível de evidência, para embasar minha prática. | Should Have | 5 | Sprint 12-13 | RF-33 | BR-700 |
| US-034 | Como coach, quero que todo conhecimento seja versionado, para rastrear atualizações científicas. | Should Have | 5 | Sprint 12-13 | RF-34 | BR-701 |
| US-035 | Como coach, quero que conceitos não sejam excluídos, apenas arquivados, para preservar o histórico. | Should Have | 3 | Sprint 12-13 | RF-35 | BR-702 |

<details><summary>Critérios de aceitação — E7 – Knowledge Base</summary>


**US-033** (MVP v2.0) — RF-33 / BR-700

Como coach, quero consultar conceitos com definição, categoria, referências e nível de evidência, para embasar minha prática.


Critérios de aceite: 1) Os conceitos devem incluir definição, categoria e referências. 2) O nível de evidência deve ser classificado (A, B, C, D). 3) Deve ser possível pesquisar por palavra-chave ou categoria.


**US-034** (MVP v2.0) — RF-34 / BR-701

Como coach, quero que todo conhecimento seja versionado, para rastrear atualizações científicas.


Critérios de aceite: 1) Cada atualização deve gerar uma nova versão. 2) Versões anteriores devem ser acessíveis. 3) O autor e data da alteração devem ser registrados.


**US-035** (MVP v2.0) — RF-35 / BR-702

Como coach, quero que conceitos não sejam excluídos, apenas arquivados, para preservar o histórico.


Critérios de aceite: 1) A exclusão física de conceitos deve ser bloqueada. 2) Conceitos arquivados devem ser marcados como inativos. 3) Conceitos arquivados devem continuar acessíveis para consulta.


</details>


## E8 – Biblioteca de Exercícios

| ID | User Story | Prioridade | Pts | Sprint | RF | BR |
|---|---|---|---|---|---|---|
| US-036 | Como coach, quero consultar exercícios com nome, grupos musculares, equipamento, execução, erros comuns, progressões, regressões, contraindicações e referências, para prescrever com segurança e embasamento. | Must Have | 5 | Sprint 10 | RF-36 | BR-800 |

<details><summary>Critérios de aceitação — E8 – Biblioteca de Exercícios</summary>


**US-036** (MVP v1.0) — RF-36 / BR-800

Como coach, quero consultar exercícios com nome, grupos musculares, equipamento, execução, erros comuns, progressões, regressões, contraindicações e referências, para prescrever com segurança e embasamento.


Critérios de aceite: 1) Todos os campos (nome, grupos musculares, equipamento, execução, erros, progressões, regressões, contraindicações, referências) devem estar preenchidos. 2) Deve ser possível buscar por nome ou grupo muscular. 3) As informações devem ser baseadas em referências científicas.


</details>


## E9 – Segurança & Permissões

| ID | User Story | Prioridade | Pts | Sprint | RF | BR |
|---|---|---|---|---|---|---|
| US-037 | Como coach, quero ser o único capaz de criar e alterar protocolos, para garantir a integridade das prescrições. | Must Have | 8 | Sprint 1-2 | RF-37 | BR-900 |
| US-038 | Como atleta, quero visualizar apenas meus próprios dados, para garantir minha privacidade. | Must Have | 8 | Sprint 1-2 | RF-38 | BR-901 |
| US-039 | Como administrador, quero gerenciar papéis e permissões (RBAC: Coach x Atleta), para controlar o acesso ao sistema. | Must Have | 8 | Sprint 1-2 | RF-39 | — |

<details><summary>Critérios de aceitação — E9 – Segurança & Permissões</summary>


**US-037** (MVP v1.0) — RF-37 / BR-900

Como coach, quero ser o único capaz de criar e alterar protocolos, para garantir a integridade das prescrições.


Critérios de aceite: 1) Apenas usuários com papel Coach podem criar/editar protocolos. 2) Tentativas de acesso não autorizado devem ser bloqueadas e logadas. 3) A permissão deve ser verificada em cada operação.


**US-038** (MVP v1.0) — RF-38 / BR-901

Como atleta, quero visualizar apenas meus próprios dados, para garantir minha privacidade.


Critérios de aceite: 1) O atleta deve ver somente seus dados (avaliações, dietas, treinos, check-ins). 2) Tentativas de acessar dados de outros atletas devem ser bloqueadas. 3) O isolamento deve ser garantido em todas as consultas.


**US-039** (MVP v1.0) — RF-39

Como administrador, quero gerenciar papéis e permissões (RBAC: Coach x Atleta), para controlar o acesso ao sistema.


Critérios de aceite: 1) O admin deve poder criar, editar e desativar papéis. 2) Permissões devem ser granulares por funcionalidade. 3) Alterações de permissão devem ser registradas no log de auditoria.


</details>


## E10 – Auditoria & Versionamento

| ID | User Story | Prioridade | Pts | Sprint | RF | BR |
|---|---|---|---|---|---|---|
| US-040 | Como coach, quero que todas as alterações sejam versionadas e nunca sobrescritas, para garantir rastreabilidade completa. | Must Have | 5 | Sprint 10 | RF-40 | BR-002 |
| US-041 | Como coach, quero que todo objeto registre versão, data, autor e histórico de alterações, para auditar qualquer mudança. | Must Have | 5 | Sprint 10 | RF-41 | — |
| US-042 | Como administrador, quero que o log de auditoria registre cadastro, exclusão lógica, alteração, login, check-in, aprovação e publicação, para garantir conformidade e segurança. | Must Have | 8 | Sprint 10 | RF-42 | BR-902 |

<details><summary>Critérios de aceitação — E10 – Auditoria & Versionamento</summary>


**US-040** (MVP v1.0) — RF-40 / BR-002

Como coach, quero que todas as alterações sejam versionadas e nunca sobrescritas, para garantir rastreabilidade completa.


Critérios de aceite: 1) Cada alteração deve criar uma nova versão do registro. 2) Versões anteriores devem ser imutáveis. 3) Deve ser possível consultar qualquer versão anterior.


**US-041** (MVP v1.0) — RF-41

Como coach, quero que todo objeto registre versão, data, autor e histórico de alterações, para auditar qualquer mudança.


Critérios de aceite: 1) Versão, data e autor devem ser campos obrigatórios em todo registro. 2) O histórico deve ser exibido em ordem cronológica. 3) Deve ser possível filtrar o histórico por autor ou data.


**US-042** (MVP v1.0) — RF-42 / BR-902

Como administrador, quero que o log de auditoria registre cadastro, exclusão lógica, alteração, login, check-in, aprovação e publicação, para garantir conformidade e segurança.


Critérios de aceite: 1) Todos os eventos listados devem ser registrados no log. 2) O log deve ser imutável e somente leitura. 3) Deve ser possível exportar o log para análise externa.


</details>
