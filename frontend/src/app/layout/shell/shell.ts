import { Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { ToolbarModule } from 'primeng/toolbar';
import { ButtonModule } from 'primeng/button';
import { AvatarModule } from 'primeng/avatar';
import { MenuModule } from 'primeng/menu';
import type { MenuItem } from 'primeng/api';
import { AuthService } from '../../core/services/auth.service';
import { SupportService } from '../../core/services/support.service';
import { AdminService } from '../../core/services/admin.service';
import { WalletService } from '../../core/services/wallet.service';
import { NAV_ITEMS, ADMIN_NAV_ITEMS } from '../nav-items';
import { GlobalSearch } from '../global-search/global-search';

const TICKET_BADGE_ROUTES = new Set(['/support', '/admin/support']);
const TICKET_BADGE_REFRESH_MS = 30000;

const PAGE_TITLES: Record<string, string> = {
  '/dashboard': 'Dashboard',
  '/vms': 'Machines Virtuelles',
  '/volumes': 'Volumes',
  '/networks': 'Réseau',
  '/projects': 'Mes projets',
  '/keypairs': 'Paires de clés SSH',
  '/server-groups': 'Groupes de serveurs',
  '/chat': 'Assistant IA',
  '/support': 'Mes tickets',
  '/support/new': 'Nouveau ticket',
  '/settings': 'Paramètres',
  '/admin/smtp': 'Configuration SMTP',
  '/admin/users': 'Gestion des clients',
  '/admin/telemetry': 'Télémétrie infrastructure',
  '/admin/compute': 'Compute',
  '/admin/volumes': 'Volumes',
  '/admin/network': 'Réseau',
  '/admin/logs': 'Logs API',
  '/admin/alerts': 'Alertes',
  '/admin/support': 'Tickets support',
  '/admin/support/new': 'Nouveau ticket (client)',
  '/admin/support/agents': 'Gestion des agents',
  '/admin/images': "Catalogue d'images",
  '/billing': 'Facturation',
  '/admin/billing': 'Facturation & tarification',
};

@Component({
  selector: 'app-shell',
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, ToolbarModule, ButtonModule, AvatarModule, MenuModule, GlobalSearch],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly auth = inject(AuthService);
  private readonly support = inject(SupportService);
  private readonly admin = inject(AdminService);
  private readonly walletService = inject(WalletService);
  private readonly destroyRef = inject(DestroyRef);
  readonly router = inject(Router);

  readonly mobileOpen = signal(false);
  readonly user = this.auth.user;
  readonly wallet = this.walletService.wallet;

  readonly navItems = computed(() => (this.user()?.role === 'ADMIN' ? ADMIN_NAV_ITEMS : NAV_ITEMS));

  /** New/replied-to tickets needing this user's attention — shown as a badge on the ticket nav item. */
  readonly ticketBadgeCount = computed(() =>
    this.user()?.role === 'ADMIN' ? this.admin.openTicketCount() : this.support.pendingCount(),
  );

  isTicketBadgeRoute(to: string): boolean {
    return TICKET_BADGE_ROUTES.has(to);
  }

  private readonly currentUrl = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  readonly pageTitle = computed(() => PAGE_TITLES[this.currentUrl()] ?? 'Safozi Cloud');

  readonly displayName = computed(() => {
    const u = this.user();
    if (!u) return 'Utilisateur';
    const full = `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim();
    return full || u.email;
  });

  readonly initials = computed(() =>
    this.displayName()
      .split(' ')
      .map((w) => w[0])
      .join('')
      .toUpperCase()
      .slice(0, 2),
  );

  readonly userMenuItems: MenuItem[] = [
    { label: 'Paramètres du compte', icon: 'pi pi-cog', command: () => this.router.navigate(['/settings']) },
    { separator: true },
    { label: 'Déconnexion', icon: 'pi pi-sign-out', command: () => this.logout() },
  ];

  constructor() {
    effect(() => {
      const u = this.user();
      if (!u) return;
      this.refreshTicketBadge();
      if (u.role !== 'ADMIN') this.walletService.fetchWallet(u.id);
    });
    const intervalId = setInterval(() => this.refreshTicketBadge(), TICKET_BADGE_REFRESH_MS);
    this.destroyRef.onDestroy(() => clearInterval(intervalId));
  }

  private refreshTicketBadge(): void {
    const u = this.user();
    if (!u) return;
    if (u.role === 'ADMIN') {
      this.admin.refreshOpenTicketCount();
    } else {
      this.support.refreshPendingCount(u.id);
    }
  }

  toggleMobile(): void {
    this.mobileOpen.update((v) => !v);
  }

  closeMobile(): void {
    this.mobileOpen.set(false);
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/']);
  }
}
