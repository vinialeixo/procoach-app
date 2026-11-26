# syntax=docker/dockerfile:1

FROM node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Padrão same-origin via proxy Nginx; mocks sempre desabilitados em produção
ARG VITE_API_BASE_URL=/api/v1
ARG VITE_USE_MOCKS=false
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_USE_MOCKS=${VITE_USE_MOCKS}

RUN npm run lint && npm run build && \
    if [ "$VITE_USE_MOCKS" != "true" ]; then rm -f dist/mockServiceWorker.js; fi

FROM nginxinc/nginx-unprivileged:1.27-alpine AS runtime

# Origem do backend para proxy reverso /api/ (configurável em runtime)
ENV BACKEND_ORIGIN=http://backend:8080

COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/healthz || exit 1
