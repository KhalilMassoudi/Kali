import { AfterViewChecked, Component, ElementRef, ViewChild, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../core/services/auth.service';
import { ChatService } from '../../core/services/chat.service';

@Component({
  selector: 'app-chat',
  imports: [CommonModule, FormsModule, ButtonModule],
  templateUrl: './chat.html',
  styleUrl: './chat.scss',
})
export class Chat implements AfterViewChecked {
  private readonly auth = inject(AuthService);
  readonly chatService = inject(ChatService);

  @ViewChild('scrollAnchor') private scrollAnchor?: ElementRef<HTMLDivElement>;

  readonly messages = this.chatService.messages;
  readonly loading = this.chatService.loading;
  readonly conversations = this.chatService.conversations;
  readonly activeConversationId = this.chatService.activeConversationId;
  readonly draft = signal('');

  private lastLength = 0;
  private initialized = false;

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user && !this.initialized) {
        this.initialized = true;
        this.chatService.init();
      }
    });
  }

  ngAfterViewChecked(): void {
    if (this.messages().length !== this.lastLength) {
      this.lastLength = this.messages().length;
      this.scrollAnchor?.nativeElement.scrollIntoView({ behavior: 'smooth' });
    }
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

  selectConversation(id: number): void {
    if (id === this.activeConversationId()) return;
    this.chatService.openConversation(id);
  }
}
