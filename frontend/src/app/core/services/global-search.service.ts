import { Injectable, inject } from '@angular/core';
import { AuthService } from './auth.service';
import { AdminService } from './admin.service';
import { VmService } from './vm.service';
import { NAV_ITEMS, ADMIN_NAV_ITEMS } from '../../layout/nav-items';
import { SearchResultItem, SearchResults } from '../models/search.model';

const MAX_PER_GROUP = 8;

@Injectable({ providedIn: 'root' })
export class GlobalSearchService {
  private readonly auth = inject(AuthService);
  private readonly admin = inject(AdminService);
  private readonly vmService = inject(VmService);

  private hydrated = false;

  /** Kicks off background loads so instance/client data is searchable app-wide, not just
   * on the pages that normally load it. Safe to call repeatedly — only fires once. */
  hydrate(): void {
    if (this.hydrated) return;
    this.hydrated = true;
    const user = this.auth.user();
    if (!user) return;
    if (user.role === 'ADMIN') {
      this.admin.loadUsers();
      this.admin.loadAllVps();
    } else {
      this.vmService.fetchVms(user.id);
    }
  }

  search(query: string): SearchResults {
    const q = query.trim().toLowerCase();
    if (q.length === 0) return { pages: [], clients: [], instances: [] };

    const isAdmin = this.auth.user()?.role === 'ADMIN';

    const pages: SearchResultItem[] = (isAdmin ? ADMIN_NAV_ITEMS : NAV_ITEMS)
      .filter((item) => item.label.toLowerCase().includes(q) || item.desc.toLowerCase().includes(q))
      .map((item) => ({
        id: `page-${item.to}`,
        label: item.label,
        sublabel: item.desc,
        icon: item.icon,
        route: [item.to],
      }))
      .slice(0, MAX_PER_GROUP);

    const clients: SearchResultItem[] = isAdmin
      ? this.admin
          .users()
          .filter(
            (u) =>
              u.email.toLowerCase().includes(q) ||
              `${u.firstName ?? ''} ${u.lastName ?? ''}`.toLowerCase().includes(q) ||
              String(u.id) === q,
          )
          .map((u) => ({
            id: `client-${u.id}`,
            label: `${u.firstName ?? ''} ${u.lastName ?? ''}`.trim() || u.email,
            sublabel: u.email,
            icon: 'pi pi-user',
            route: ['/admin/users'],
          }))
          .slice(0, MAX_PER_GROUP)
      : [];

    const vmSource = isAdmin ? this.admin.allVps() : this.vmService.vms();
    const instances: SearchResultItem[] = vmSource
      .filter(
        (v) =>
          v.name.toLowerCase().includes(q) ||
          (v.externalId ?? '').toLowerCase().includes(q) ||
          String(v.id) === q,
      )
      .map((v) => ({
        id: `vm-${v.id}`,
        label: v.name,
        sublabel: v.externalId ?? `#${v.id}`,
        icon: 'pi pi-server',
        route: ['/vms', String(v.id)],
      }))
      .slice(0, MAX_PER_GROUP);

    return { pages, clients, instances };
  }
}
