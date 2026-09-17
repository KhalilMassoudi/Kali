import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { SelectModule } from 'primeng/select';
import { InputTextModule } from 'primeng/inputtext';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { AdminService } from '../../../core/services/admin.service';

@Component({
  selector: 'app-admin-logs',
  imports: [CommonModule, FormsModule, ButtonModule, TableModule, TagModule, SelectModule, InputTextModule, ToggleSwitchModule],
  templateUrl: './logs.html',
  styleUrl: './logs.scss',
})
export class AdminLogs {
  readonly admin = inject(AdminService);

  readonly loading = signal(true);
  readonly error = this.admin.error;
  readonly page = this.admin.logs;
  readonly services = this.admin.logServices;

  readonly serviceFilter = signal<string | null>(null);
  readonly errorsOnly = signal(false);
  readonly search = signal('');

  readonly pageSize = 50;
  private currentPage = 0;
  private searchDebounce?: ReturnType<typeof setTimeout>;

  get serviceOptions(): { label: string; value: string | null }[] {
    return [{ label: 'Tous les services', value: null }, ...this.services().map((s) => ({ label: s, value: s }))];
  }

  constructor() {
    this.admin.loadLogServices();
    this.fetch(0);
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    const page = Math.floor((event.first ?? 0) / this.pageSize);
    this.fetch(page);
  }

  onFilterChange(): void {
    this.fetch(0);
  }

  onSearchInput(): void {
    clearTimeout(this.searchDebounce);
    this.searchDebounce = setTimeout(() => this.fetch(0), 400);
  }

  async refresh(): Promise<void> {
    await this.fetch(this.currentPage);
  }

  statusSeverity(status: number | null): 'success' | 'warn' | 'danger' | 'secondary' {
    if (status == null) return 'secondary';
    if (status >= 500) return 'danger';
    if (status >= 400) return 'warn';
    return 'success';
  }

  private async fetch(page: number): Promise<void> {
    this.currentPage = page;
    this.loading.set(true);
    try {
      await this.admin.loadApiLogs({
        page,
        size: this.pageSize,
        service: this.serviceFilter() ?? undefined,
        errorsOnly: this.errorsOnly(),
        search: this.search() || undefined,
      });
    } finally {
      this.loading.set(false);
    }
  }
}
