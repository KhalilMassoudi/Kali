// Mirrors kali.microservices.infrastructureservice.entities.VpsServer.VpsStatus exactly —
// the old React frontend also checked for 'ACTIVE', which the backend enum never emits.
export type VpsStatus =
  | 'PENDING'
  | 'RUNNING'
  | 'STOPPED'
  | 'DELETED'
  | 'ERROR'
  | 'RESIZING'
  | 'VERIFY_RESIZE'
  | 'RESCUE'
  | 'PAUSED'
  | 'SUSPENDED'
  | 'SHELVED'
  | 'SHELVED_OFFLOADED';

export interface Vm {
  id: number;
  userId: number;
  name: string;
  os: string;
  ram: number | null;
  cpu: number | null;
  storage: number | null;
  externalId: string | null;
  ipAddress: string | null;
  floatingIp: string | null;
  region: string | null;
  status: VpsStatus;
  projectId: number | null;
  locked: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateVpsRequest {
  userId: number;
  name: string;
  os: string;
  ram: number;
  cpu: number;
  storage: number;
  region?: string;
  networkId?: string;
  securityGroups?: string[];
  imageId?: string;
  keypairId?: number;
  serverGroupId?: number;
}

export interface VmHealth {
  status: string;
  ipAddress: string | null;
  floatingIp: string | null;
  diagnostics: Record<string, number> | null;
  checkedAt: string;
}

export interface NetworkOption {
  id: string;
  name: string;
  external: boolean;
}

export interface SecurityGroupOption {
  id: string;
  name: string;
  description: string | null;
}

export type VolumeStatus = 'CREATING' | 'AVAILABLE' | 'IN_USE' | 'ERROR' | 'DELETED';

export interface Volume {
  id: number;
  userId: number;
  name: string;
  sizeGb: number;
  externalId: string | null;
  attachedVpsId: number | null;
  device: string | null;
  region: string | null;
  status: VolumeStatus;
  projectId: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateVolumeRequest {
  userId: number;
  name: string;
  sizeGb: number;
  region?: string;
}

export interface FlavorOption {
  id: string;
  name: string;
  ram: number;
  vcpus: number;
  disk: number;
}

export type VmMetadata = Record<string, string>;

export interface VpsActivityLog {
  id: number;
  vpsId: number;
  userId: number;
  action: string;
  detail: string | null;
  createdAt: string;
}

export type BackupFrequency = 'DAILY' | 'WEEKLY';

export interface VpsBackupSchedule {
  id: number;
  vpsId: number;
  userId: number;
  frequency: BackupFrequency;
  retentionCount: number;
  enabled: boolean;
  lastRunAt: string | null;
  nextRunAt: string | null;
}

export interface BackupScheduleRequest {
  frequency: BackupFrequency;
  retentionCount: number;
  enabled: boolean;
}

export interface ImageOption {
  id: string;
  name: string;
  minDiskGb: number | null;
  minRamMb: number | null;
}

export type ImageVisibility = 'PUBLISHED' | 'HIDDEN';
export type ImageSource = 'ADOPTED' | 'IMPORTED_URL' | 'UPLOADED';

export interface PlatformImage {
  id: number;
  externalId: string;
  displayName: string;
  osDistro: string | null;
  osVersion: string | null;
  minDiskGb: number | null;
  minRamMb: number | null;
  visibility: ImageVisibility;
  source: ImageSource;
  createdAt: string;
}

export interface AdoptImageRequest {
  externalId: string;
  displayName: string;
  osDistro?: string;
  osVersion?: string;
}

export interface ImportImageUrlRequest {
  displayName: string;
  osDistro?: string;
  osVersion?: string;
  imageUrl: string;
  diskFormat?: string;
  minDiskGb?: number;
  minRamMb?: number;
}

export interface SecurityGroupRuleOption {
  id: string;
  direction: string | null;
  protocol: string | null;
  portMin: number | null;
  portMax: number | null;
  remoteCidr: string | null;
}

export interface AddSecurityGroupRuleRequest {
  direction: 'ingress' | 'egress';
  protocol: 'tcp' | 'udp' | 'icmp';
  portMin?: number;
  portMax?: number;
  cidr: string;
}

export type NetworkStatus = 'CREATING' | 'ACTIVE' | 'DELETED' | 'ERROR';

export interface VpsNetwork {
  id: number;
  userId: number;
  name: string;
  externalId: string | null;
  subnetId: string | null;
  cidr: string;
  status: NetworkStatus;
  projectId: number | null;
  createdAt: string;
}

export interface CreateNetworkRequest {
  userId: number;
  name: string;
  cidr: string;
}

export type SecurityGroupStatus = 'ACTIVE' | 'DELETED' | 'ERROR';

export interface VpsSecurityGroup {
  id: number;
  userId: number;
  name: string;
  description: string | null;
  externalId: string | null;
  status: SecurityGroupStatus;
  projectId: number | null;
  createdAt: string;
}

export interface CreateSecurityGroupRequest {
  userId: number;
  name: string;
  description?: string;
}

export interface UserQuota {
  maxVcpu: number;
  maxRamMb: number;
  maxStorageGb: number;
  maxVms: number;
  maxVolumes: number;
  maxNetworks: number;
  maxSecurityGroups: number;
}

export interface QuotaUsage {
  vmCount: number;
  totalVcpu: number;
  totalRamMb: number;
  volumeCount: number;
  totalStorageGb: number;
  networkCount: number;
  securityGroupCount: number;
}

export interface ClientKeypair {
  id: number;
  userId: number;
  displayName: string;
  openstackName: string;
  publicKey: string | null;
  fingerprint: string | null;
  createdAt: string;
}

export interface CreateKeypairRequest {
  userId: number;
  name: string;
  publicKey?: string;
}

export interface CreateKeypairResponse {
  keypair: ClientKeypair;
  privateKey: string | null; // set only when OpenStack generated the key — shown once, never persisted client-side
}

export type ServerGroupPolicy = 'AFFINITY' | 'ANTI_AFFINITY' | 'SOFT_AFFINITY' | 'SOFT_ANTI_AFFINITY';

export interface ClientServerGroup {
  id: number;
  userId: number;
  name: string;
  externalId: string;
  policy: ServerGroupPolicy;
  createdAt: string;
}

export interface CreateServerGroupRequest {
  userId: number;
  name: string;
  policy: ServerGroupPolicy;
}

export interface ServerGroupDetails {
  id: string;
  name: string;
  policies: string[];
  members: string[];
}

export type VolumeSnapshotStatus = 'CREATING' | 'AVAILABLE' | 'ERROR' | 'DELETED';

export interface VpsVolumeSnapshot {
  id: number;
  userId: number;
  sourceVolumeId: number;
  name: string;
  description: string | null;
  externalId: string | null;
  sizeGb: number | null;
  status: VolumeSnapshotStatus;
  createdAt: string;
}

export interface CreateVolumeSnapshotRequest {
  userId: number;
  volumeId: number;
  name: string;
  description?: string;
}

export interface QuotaResponse {
  quota: UserQuota;
  usage: QuotaUsage;
}

export interface MetricsSummary {
  vmCount: number;
  runningCount: number;
  totalVcpu: number;
  totalRamMb: number;
  totalStorageGb: number;
  avgCpuUtil: number | null;
  dataAvailable: boolean;
}

/** Usage vs. project quota; `limit` is -1 when OpenStack reports "unlimited". */
export interface QuotaUsage {
  used: number;
  limit: number;
}

/** Live ground-truth usage read directly from OpenStack (same numbers as Horizon's Overview). */
export interface PlatformTotals {
  runningInstances: number;
  instances: QuotaUsage;
  vcpus: QuotaUsage;
  ramMb: QuotaUsage;
  volumes: QuotaUsage;
  volumeGb: QuotaUsage;
  snapshots: QuotaUsage;
  floatingIps: QuotaUsage;
  securityGroups: QuotaUsage;
  securityGroupRules: QuotaUsage;
  networks: QuotaUsage;
  ports: QuotaUsage;
  routers: QuotaUsage;
}

export interface ClientFloatingIp {
  id: number;
  userId: number;
  floatingIpAddress: string;
  pool: string | null;
  associatedVpsId: number | null;
  createdAt: string;
}

export interface AllocateFloatingIpRequest {
  userId: number;
  pool?: string;
}

export interface ClientRouter {
  id: number;
  userId: number;
  name: string;
  externalId: string;
  externalGatewayNetworkId: string | null;
  createdAt: string;
}

export interface CreateRouterRequest {
  userId: number;
  name: string;
  externalNetworkId?: string;
}

export interface RouterInterfaceDetails {
  portId: string;
  subnetId: string | null;
  networkId: string;
}

export interface InterfaceDetails {
  attachmentId: string;
  portId: string;
  networkId: string;
  macAddress: string | null;
  fixedIpAddress: string | null;
}