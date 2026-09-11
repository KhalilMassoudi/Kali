import { VolumeStatus, VpsStatus } from '../../core/models/vm.model';

const LABELS: Record<VpsStatus, string> = {
  RUNNING: 'En marche',
  STOPPED: 'Arrêtée',
  PENDING: 'En cours',
  ERROR: 'Erreur',
  DELETED: 'Supprimée',
  RESIZING: 'Redimensionnement...',
  VERIFY_RESIZE: 'À confirmer',
  RESCUE: 'Mode rescue',
  PAUSED: 'En pause',
  SUSPENDED: 'Suspendue',
  SHELVED: 'Placée en réserve',
  SHELVED_OFFLOADED: 'Réserve (déchargée)',
};

const SEVERITIES: Record<VpsStatus, 'success' | 'warn' | 'info' | 'danger' | 'secondary'> = {
  RUNNING: 'success',
  STOPPED: 'warn',
  PENDING: 'info',
  ERROR: 'danger',
  DELETED: 'secondary',
  RESIZING: 'info',
  VERIFY_RESIZE: 'warn',
  RESCUE: 'danger',
  PAUSED: 'secondary',
  SUSPENDED: 'secondary',
  SHELVED: 'secondary',
  SHELVED_OFFLOADED: 'secondary',
};

export function vmStatusLabel(status: string): string {
  return LABELS[status as VpsStatus] ?? status;
}

export function vmStatusSeverity(status: string): 'success' | 'warn' | 'info' | 'danger' | 'secondary' {
  return SEVERITIES[status as VpsStatus] ?? 'info';
}

export function formatRam(ram: number | null): string {
  const v = ram ?? 0;
  return v >= 1024 ? `${v / 1024} GB` : `${v} MB`;
}

const VOLUME_LABELS: Record<VolumeStatus, string> = {
  AVAILABLE: 'Disponible',
  IN_USE: 'Attaché',
  CREATING: 'Création...',
  ERROR: 'Erreur',
  DELETED: 'Supprimé',
};

const VOLUME_SEVERITIES: Record<VolumeStatus, 'success' | 'warn' | 'info' | 'danger' | 'secondary'> = {
  AVAILABLE: 'info',
  IN_USE: 'success',
  CREATING: 'warn',
  ERROR: 'danger',
  DELETED: 'secondary',
};

export function volumeStatusLabel(status: VolumeStatus): string {
  return VOLUME_LABELS[status] ?? status;
}

export function volumeStatusSeverity(status: VolumeStatus): 'success' | 'warn' | 'info' | 'danger' | 'secondary' {
  return VOLUME_SEVERITIES[status] ?? 'info';
}
