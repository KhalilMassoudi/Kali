#!/bin/bash
set -e

# Runs with hostNetwork: true, so everything this script does to networking
# (the wg0 interface, the route) applies directly to the node itself - any
# other pod scheduled on this same node gains reachability to WG_ALLOWED_IPS
# through this tunnel, the same way it would if set up manually on the node.
mkdir -p /etc/wireguard
cat > /etc/wireguard/wg0.conf <<EOF
[Interface]
PrivateKey = ${WG_PRIVATE_KEY}
Address = ${WG_ADDRESS}

[Peer]
PublicKey = ${WG_PEER_PUBKEY}
Endpoint = ${WG_PEER_ENDPOINT}
AllowedIPs = ${WG_ALLOWED_IPS}
PersistentKeepalive = 25
EOF
chmod 600 /etc/wireguard/wg0.conf

ip link delete dev wg0 2>/dev/null || true

WG_SUDO=true /usr/local/bin/boringtun-cli -f --disable-drop-privileges -v info wg0 &
TUNNEL_PID=$!
sleep 2

wg setconf wg0 <(wg-quick strip /etc/wireguard/wg0.conf)
ip -4 address add "${WG_ADDRESS}" dev wg0
ip link set mtu 1420 up dev wg0
ip route add "${WG_ALLOWED_IPS}" dev wg0 2>/dev/null || true

# Exit (and let Kubernetes restart the pod) if the tunnel process dies.
wait "$TUNNEL_PID"
