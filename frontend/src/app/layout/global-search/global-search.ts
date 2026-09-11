import { Component, ElementRef, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { GlobalSearchService } from '../../core/services/global-search.service';
import { SearchResultItem } from '../../core/models/search.model';

@Component({
  selector: 'app-global-search',
  imports: [CommonModule, FormsModule],
  templateUrl: './global-search.html',
  styleUrl: './global-search.scss',
  host: {
    '(document:click)': 'onDocumentClick($event)',
  },
})
export class GlobalSearch {
  private readonly search = inject(GlobalSearchService);
  private readonly router = inject(Router);
  private readonly host = inject(ElementRef<HTMLElement>);

  readonly query = signal('');
  readonly open = signal(false);

  readonly results = computed(() => this.search.search(this.query()));
  readonly hasResults = computed(() => {
    const r = this.results();
    return r.pages.length + r.clients.length + r.instances.length > 0;
  });

  constructor() {
    this.search.hydrate();
  }

  onFocus(): void {
    this.open.set(true);
  }

  onDocumentClick(event: MouseEvent): void {
    if (!this.host.nativeElement.contains(event.target as Node)) {
      this.open.set(false);
    }
  }

  select(item: SearchResultItem): void {
    this.router.navigate(item.route);
    this.query.set('');
    this.open.set(false);
  }

  clear(): void {
    this.query.set('');
  }
}
