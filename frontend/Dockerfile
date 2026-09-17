# Build stage
# Angular's CLI hard-requires Node >=22.22.3 (refuses to run below it, not just a
# warning) - confirmed live: node:20-alpine failed the build outright.
FROM node:24-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

# Runtime stage
FROM nginx:alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
# .envsh files are sourced (not executed) by the official image's entrypoint, in
# name order, before it envsubst's *.template files - this one must run before the
# built-in "20-envsubst-on-templates.sh" step, hence the "05" prefix.
COPY set-resolver.envsh /docker-entrypoint.d/05-set-resolver.envsh
RUN chmod +x /docker-entrypoint.d/05-set-resolver.envsh
# Default matches the k8s Service name Gateway-service will get in the CD phase;
# override at `docker run` / deployment time with -e GATEWAY_URL=... if it differs.
ENV GATEWAY_URL=http://gateway-service:8080
EXPOSE 80
