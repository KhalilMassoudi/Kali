import { Component, effect, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';
import { AuthService } from '../../core/services/auth.service';
import { KeypairService } from '../../core/services/keypair.service';
import { ClientKeypair } from '../../core/models/vm.model';
import { CreateKeypairDialog } from './create-keypair-dialog/create-keypair-dialog';

interface RowState {
  deleting: boolean;
}

@Component({
  selector: 'app-keypairs',
  imports: [CommonModule, ButtonModule, TableModule, TooltipModule, CreateKeypairDialog],
  templateUrl: './keypairs.html',
  styleUrl: './keypairs.scss',
})
export class Keypairs {
  private readonly auth = inject(AuthService);
  readonly keypairService = inject(KeypairService);

  readonly user = this.auth.user;
  readonly keypairs = this.keypairService.keypairs;
  readonly loading = this.keypairService.loading;
  readonly error = this.keypairService.error;

  readonly showCreateDialog = signal(false);
  private readonly rowStates = signal<Record<number, RowState>>({});

  constructor() {
    effect(() => {
      const user = this.auth.user();
      if (user) this.keypairService.fetchKeypairs(user.id);
    });
  }

  refresh(): void {
    const user = this.user();
    if (user) this.keypairService.fetchKeypairs(user.id);
  }

  clearError(): void {
    this.keypairService.clearError();
  }

  rowState(keypair: ClientKeypair): RowState {
    return this.rowStates()[keypair.id] ?? { deleting: false };
  }

  async remove(keypair: ClientKeypair): Promise<void> {
    this.rowStates.update((s) => ({ ...s, [keypair.id]: { deleting: true } }));
    try {
      await this.keypairService.deleteKeypair(keypair.id);
    } finally {
      this.rowStates.update((s) => ({ ...s, [keypair.id]: { deleting: false } }));
    }
  }
}
