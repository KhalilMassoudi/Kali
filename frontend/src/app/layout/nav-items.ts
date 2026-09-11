export interface NavItem {
  to: string;
  icon: string;
  label: string;
  desc: string;
  /** Insert a section divider above this item (visually separates nav groups). */
  divider?: boolean;
}

export const NAV_ITEMS: NavItem[] = [
  { to: '/dashboard', icon: 'pi pi-th-large', label: 'Dashboard', desc: 'Vue générale' },
  { to: '/vms', icon: 'pi pi-server', label: 'Mes VMs', desc: 'Gérer les serveurs' },
  { to: '/volumes', icon: 'pi pi-database', label: 'Volumes', desc: 'Stockage bloc' },
  { to: '/networks', icon: 'pi pi-share-alt', label: 'Réseau', desc: 'Réseaux, routeurs, sécurité, IP flottantes' },
  { to: '/projects', icon: 'pi pi-folder', label: 'Projets', desc: 'Regrouper mes ressources' },
  { to: '/chat', icon: 'pi pi-comments', label: 'Assistant IA', desc: 'Support intelligent' },
  { to: '/support', icon: 'pi pi-ticket', label: 'Support', desc: 'Mes tickets' },
  { to: '/settings', icon: 'pi pi-cog', label: 'Paramètres', desc: 'Compte & sécurité' },
];

export const ADMIN_NAV_ITEMS: NavItem[] = [
  // Infrastructure group — Télémétrie no longer stands alone, it's a shortcut card on Dashboard now.
  { to: '/dashboard', icon: 'pi pi-th-large', label: 'Dashboard', desc: "Vue d'ensemble infrastructure" },
  { to: '/admin/compute', icon: 'pi pi-server', label: 'Compute', desc: 'Instances, images, clés, groupes' },
  { to: '/admin/volumes', icon: 'pi pi-database', label: 'Volumes', desc: 'Stockage bloc & snapshots' },
  { to: '/admin/network', icon: 'pi pi-share-alt', label: 'Réseau', desc: 'Réseaux & sécurité' },
  { to: '/admin/domains', icon: 'pi pi-globe', label: 'Domaines', desc: 'Noms de domaine' },

  // Clients group — kept on its own
  { to: '/admin/users', icon: 'pi pi-users', label: 'Clients', desc: 'Comptes, projets & usage', divider: true },

  // Admin/system group
  { to: '/settings', icon: 'pi pi-cog', label: 'Paramètres', desc: 'Compte & sécurité', divider: true },
  { to: '/admin/smtp', icon: 'pi pi-envelope', label: 'SMTP', desc: 'Configuration email' },
  { to: '/admin/logs', icon: 'pi pi-list', label: 'Logs API', desc: 'Consommation des API' },
  { to: '/admin/alerts', icon: 'pi pi-bell', label: 'Alertes', desc: 'Alertes actives' },

  // Other
  { to: '/chat', icon: 'pi pi-comments', label: 'Assistant IA', desc: 'Support intelligent', divider: true },
  { to: '/admin/support', icon: 'pi pi-ticket', label: 'Tickets support', desc: 'Gestion des demandes' },
];
