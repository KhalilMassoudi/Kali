#!/bin/sh
set -e

# The SSH tunnel is internal-only (127.0.0.1) — nginx in front of it is what
# actually gets exposed, so it can rewrite the Host header to "localhost",
# which is the only value Ollama trusts unconditionally. This avoids relying
# on Ollama's OLLAMA_ORIGINS allowlist matching whatever hostname a
# Kubernetes Service happens to be named.
INTERNAL_PORT=11435

cat > /etc/nginx/http.d/default.conf <<NGINXEOF
server {
    listen 0.0.0.0:${LOCAL_PORT:-11434};
    location / {
        proxy_pass http://127.0.0.1:${INTERNAL_PORT};
        proxy_set_header Host "localhost:${REMOTE_PORT:-11434}";
    }
}
NGINXEOF

autossh -M 0 -N \
  -o "ServerAliveInterval=30" \
  -o "ServerAliveCountMax=3" \
  -o "ExitOnForwardFailure=yes" \
  -o "StrictHostKeyChecking=accept-new" \
  -i /keys/gpu_tunnel \
  -p "${SSH_PORT}" \
  -L "127.0.0.1:${INTERNAL_PORT}:localhost:${REMOTE_PORT:-11434}" \
  "${SSH_USER}@${SSH_HOST}" &
TUNNEL_PID=$!

nginx -g "daemon off;" &
NGINX_PID=$!

# Exit (and let Kubernetes restart the pod) if either process dies.
wait "$TUNNEL_PID"
kill "$NGINX_PID" 2>/dev/null || true
