---
description: Roda o gate de qualidade (backend + front) e resume falhas
---

Rode o gate do projeto e corrija o que falhar (máx. 3 tentativas):

Backend (na raiz do repo):
- Windows: `.\mvnw.cmd clean test`
- Linux/macOS: `./mvnw clean test`
Isso inclui a verificação de boundaries do Spring Modulith.

Front (em `procoach-front`), se houve mudança no front:
- `npm run lint`
- `npm run build`

Regras:
- Mostre apenas o resumo: nº de testes, falhas, e os 1-3 erros mais relevantes. Não cole logs inteiros.
- Não desabilite nem apague testes para "passar". Se um teste está errado, explique antes de alterar.
- Se após 3 tentativas ainda falhar, pare e descreva a causa provável.
