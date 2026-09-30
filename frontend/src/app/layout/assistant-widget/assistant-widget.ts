import { AfterViewChecked, Component, ElementRef, ViewChild, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../core/services/auth.service';
import { ChatService } from '../../core/services/chat.service';
import { BotIcon } from '../../shared/bot-icon/bot-icon';

/**
 * Floating "Assistant IA" button available on every page (except the full chat page itself).
 * Shares ChatService with the /chat page, so a conversation started here continues there.
 */
@Component({
  selector: 'app-assistant-widget',
  imports: [CommonModule, FormsModule, RouterLink, ButtonModule, BotIcon],
  templateUrl: './assistant-widget.html',
  styleUrl: './assistant-widget.scss',
})
export class AssistantWidget implements AfterViewChecked {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly chatService = inject(ChatService);

  @ViewChild('scrollAnchor') private scrollAnchor?: ElementRef<HTMLDivElement>;

  readonly open = signal(false);
  readonly draft = signal('');
  readonly messages = this.chatService.messages;
  readonly loading = this.chatService.loading;

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map((e) => e.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );
  readonly hidden = computed(() => this.url().startsWith('/chat'));

  private initialized = false;
  private lastLength = -1;

  ngAfterViewChecked(): void {
    if (this.open() && this.messages().length !== this.lastLength) {
      this.lastLength = this.messages().length;
      this.scrollAnchor?.nativeElement.scrollIntoView({ block: 'end' });
    }
  }

  toggle(): void {
    if (!this.open() && !this.initialized && this.auth.user()) {
      this.initialized = true;
      this.chatService.init();
    }
    this.lastLength = -1;
    this.open.update((o) => !o);
  }

  send(): void {
    const content = this.draft().trim();
    const user = this.auth.user();
    if (!content || !user || this.loading()) return;
    this.draft.set('');
    this.chatService.sendMessage(user.id, content);
  }

  onKeydown(ev: KeyboardEvent): void {
    if (ev.key === 'Enter' && !ev.shiftKey) {
      ev.preventDefault();
      this.send();
    }
  }

  newConversation(): void {
    this.chatService.startNewConversation();
  }
}
