import { Component, EventEmitter, Output, inject, input, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { FloatingIpService } from '../../../core/services/floating-ip.service';

@Component({
  selector: 'app-allocate-floating-ip-dialog',
  imports: [CommonModule, FormsModule, DialogModule, SelectModule, ButtonModule, MessageModule],
  templateUrl: './allocate-floating-ip-dialog.html',
  styleUrl: './allocate-floating-ip-dialog.scss',
})
export class AllocateFloatingIpDialog {
  private readonly auth = inject(AuthService);
  private readonly floatingIpService = inject(FloatingIpService);

  readonly visible = model.required<boolean>();
  readonly pools = input<string[]>([]);
  readonly targetUserId = input<number | null>(null);
  @Output() readonly created = new EventEmitter<void>();

  readonly loading = this.floatingIpService.loading;
  readonly error = signal('');
  readonly selectedPool = signal<string | null>(null);

  close(): void {
    this.visible.set(false);
  }

  async submit(): Promise<void> {
    this.error.set('');
    const user = this.auth.user();
    try {
      await this.floatingIpService.allocate({
        userId: this.targetUserId() ?? user?.id ?? 1,
        pool: this.selectedPool() ?? undefined,
      });
      this.created.emit();
      this.close();
      this.selectedPool.set(null);
    } catch {
      this.error.set(this.floatingIpService.error() || "Erreur lors de l'allocation");
    }
  }
}
