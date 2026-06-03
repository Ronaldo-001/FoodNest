# ─────────────────────────────────────────────────────────────
# Multi-stage Dockerfile for React frontend
# ─────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci --silent
COPY . .
RUN npm run build

# ─────────────────────────────────────────────────────────────
# Serve with nginx
# ─────────────────────────────────────────────────────────────
FROM nginx:alpine
# SECURITY: remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
