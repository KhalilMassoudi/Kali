import { Vm, Volume, VpsNetwork, VpsSecurityGroup } from './vm.model';

export interface ClientProject {
  id: number;
  userId: number;
  name: string;
  description: string | null;
  externalId: string | null; // always null today — reserved for a future real OpenStack project
  createdAt: string;
  updatedAt: string;
}

export interface CreateClientProjectRequest {
  userId: number;
  name: string;
  description?: string;
}

export interface UpdateClientProjectRequest {
  name: string;
  description?: string;
}

export interface ProjectResourcesSummary {
  vps: Vm[];
  volumes: Volume[];
  networks: VpsNetwork[];
  securityGroups: VpsSecurityGroup[];
}

export interface ResourceCounts {
  vps: number;
  volumes: number;
  networks: number;
  securityGroups: number;
}

export interface AdminProjectSummary {
  id: number;
  name: string;
  description: string | null;
  counts: ResourceCounts;
}

export interface AdminClientSummary {
  userId: number;
  projects: AdminProjectSummary[];
  unassigned: ResourceCounts;
}
