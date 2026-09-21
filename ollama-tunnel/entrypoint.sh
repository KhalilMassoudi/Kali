#!/bin/sh
set -e

exec autossh -M 0 -N \
  -o "ServerAliveInterval=30" \
  -o "ServerAliveCountMax=3" \
  -o "ExitOnForwardFailure=yes" \
  -o "StrictHostKeyChecking=accept-new" \
  -i /keys/gpu_tunnel \
  -p "${SSH_PORT}" \
  -L "0.0.0.0:${LOCAL_PORT:-11434}:localhost:${REMOTE_PORT:-11434}" \
  "${SSH_USER}@${SSH_HOST}"
