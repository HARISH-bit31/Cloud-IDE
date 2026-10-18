# Stage 1: Build React/Vite Frontend
FROM node:20-alpine AS builder

WORKDIR /app

# Install dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy source files and build
COPY . .

# Build with relative /api base URL for production Nginx proxy
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN npm run build

# Stage 2: Serve using Nginx
FROM nginx:alpine

# Remove default server config and ipv6 script that attempts to modify files at runtime
RUN rm -f /etc/nginx/conf.d/default.conf /docker-entrypoint.d/10-listen-on-ipv6-by-default.sh

# Configure Nginx to run as unprivileged user:
# 1. Update PID path to /tmp/nginx.pid
# 2. Comment out 'user nginx;' directive in /etc/nginx/nginx.conf
RUN sed -i 's|/run/nginx.pid|/tmp/nginx.pid|g' /etc/nginx/nginx.conf && \
    sed -i 's|/var/run/nginx.pid|/tmp/nginx.pid|g' /etc/nginx/nginx.conf && \
    sed -i 's|^user |#user |g' /etc/nginx/nginx.conf

# Copy custom Nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy build artifacts to Nginx html directory
COPY --from=builder /app/dist /usr/share/nginx/html

# Set proper ownership and permissions for non-root user (nginx: uid 101, gid 101)
RUN chown -R 101:101 /usr/share/nginx/html /var/cache/nginx /etc/nginx && \
    chmod -R 755 /usr/share/nginx/html

USER 101

EXPOSE 8080

CMD ["nginx", "-g", "daemon off;"]
