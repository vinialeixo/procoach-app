# ProCoach

Monorepo com backend e frontend.

| Pasta | Stack | Porta dev |
|---|---|---|
| `backend/` | Java / Spring (Maven) | 8080 |
| `frontend/` | React + Vite + TypeScript | 5173 |

## Desenvolvimento

```bash
# backend
cd backend && ./mvnw spring-boot:run

# frontend (proxy /api -> localhost:8080)
cd frontend && npm install && npm run dev
```

Documentação de cada parte: `backend/README.md`, `frontend/README.md`.
