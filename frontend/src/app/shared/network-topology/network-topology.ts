import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

export interface TopologyNetwork {
  id: string;
  name: string;
  external: boolean;
}

export interface TopologyRouter {
  id: string;
  name: string;
  gatewayNetworkId: string | null;
  connectedNetworkIds: string[];
}

interface Node {
  id: string;
  label: string;
  x: number;
  y: number;
  kind: 'external' | 'router' | 'private';
}

interface Edge {
  x1: number;
  y1: number;
  x2: number;
  y2: number;
}

const WIDTH = 720;
const ROW_Y = { external: 60, router: 220, private: 380 };
const NODE_W = 140;
const NODE_H = 56;

@Component({
  selector: 'app-network-topology',
  imports: [CommonModule],
  templateUrl: './network-topology.html',
  styleUrl: './network-topology.scss',
})
export class NetworkTopology {
  readonly networks = input<TopologyNetwork[]>([]);
  readonly routers = input<TopologyRouter[]>([]);

  readonly width = WIDTH;
  readonly height = 440;

  private layoutRow(items: { id: string; label: string }[], y: number, kind: Node['kind']): Node[] {
    if (items.length === 0) return [];
    const gap = WIDTH / (items.length + 1);
    return items.map((item, i) => ({ id: item.id, label: item.label, x: gap * (i + 1), y, kind }));
  }

  readonly externalNodes = computed(() =>
    this.layoutRow(
      this.networks()
        .filter((n) => n.external)
        .map((n) => ({ id: n.id, label: n.name })),
      ROW_Y.external,
      'external',
    ),
  );

  readonly routerNodes = computed(() =>
    this.layoutRow(
      this.routers().map((r) => ({ id: r.id, label: r.name })),
      ROW_Y.router,
      'router',
    ),
  );

  readonly privateNodes = computed(() =>
    this.layoutRow(
      this.networks()
        .filter((n) => !n.external)
        .map((n) => ({ id: n.id, label: n.name })),
      ROW_Y.private,
      'private',
    ),
  );

  readonly allNodes = computed(() => [...this.externalNodes(), ...this.routerNodes(), ...this.privateNodes()]);

  readonly edges = computed(() => {
    const nodesById = new Map(this.allNodes().map((n) => [n.id, n]));
    const edges: Edge[] = [];
    for (const router of this.routers()) {
      const routerNode = nodesById.get(router.id);
      if (!routerNode) continue;
      if (router.gatewayNetworkId) {
        const gw = nodesById.get(router.gatewayNetworkId);
        if (gw) edges.push({ x1: gw.x, y1: gw.y + NODE_H / 2, x2: routerNode.x, y2: routerNode.y - NODE_H / 2 });
      }
      for (const netId of router.connectedNetworkIds) {
        const net = nodesById.get(netId);
        if (net) edges.push({ x1: routerNode.x, y1: routerNode.y + NODE_H / 2, x2: net.x, y2: net.y - NODE_H / 2 });
      }
    }
    return edges;
  });

  readonly isEmpty = computed(() => this.allNodes().length === 0);
  readonly nodeWidth = NODE_W;
  readonly nodeHeight = NODE_H;
}
