FROM node:22.13.1-alpine AS build
WORKDIR /workspace

COPY frontend/package*.json ./
RUN npm ci

COPY frontend/ .
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
RUN npm run build

FROM nginx:1.27.4-alpine
RUN apk add --no-cache libcap \
    && addgroup -S app \
    && adduser -S app -G app \
    && setcap 'cap_net_bind_service=+ep' /usr/sbin/nginx \
    && mkdir -p /tmp/nginx/client_temp /tmp/nginx/proxy_temp /tmp/nginx/fastcgi_temp /tmp/nginx/uwsgi_temp /tmp/nginx/scgi_temp \
    && chown -R app:app /tmp/nginx /var/cache/nginx /var/log/nginx /usr/share/nginx/html
COPY frontend/nginx.conf /etc/nginx/nginx.conf
COPY --from=build /workspace/dist /usr/share/nginx/html
USER app
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -qO- http://127.0.0.1/health >/dev/null || exit 1
CMD ["nginx", "-g", "daemon off;"]
