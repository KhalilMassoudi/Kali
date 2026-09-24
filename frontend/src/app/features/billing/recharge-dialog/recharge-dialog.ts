import { Component, EventEmitter, Output, inject, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { WalletService } from '../../../core/services/wallet.service';

const QUICK_AMOUNTS = [10, 25, 50, 100];

@Component({
  selector: 'app-recharge-dialog',
  imports: [CommonModule, ReactiveFormsModule, DialogModule, InputNumberModule, ButtonModule, MessageModule],
  templateUrl: './recharge-dialog.html',
  styleUrl: './recharge-dialog.scss',
})
export class RechargeDialog {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly walletService = inject(WalletService);

  readonly visible = model.required<boolean>();
  @Output() readonly recharged = new EventEmitter<void>();

  readonly loading = this.walletService.loading;
  readonly error = signal('');
  readonly quickAmounts = QUICK_AMOUNTS;

  readonly form = this.fb.nonNullable.group({
    amount: [25, [Validators.required, Validators.min(1)]],
  });

  setQuickAmount(amount: number): void {
    this.form.patchValue({ amount });
  }

  close(): void {
    this.visible.set(false);
  }

  async submit(): Promise<void> {
    this.error.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const user = this.auth.user();
    if (!user) return;
    const { amount } = this.form.getRawValue();
    try {
      await this.walletService.recharge(user.id, amount);
      this.recharged.emit();
      this.close();
      this.form.reset({ amount: 25 });
    } catch {
      this.error.set(this.walletService.error() || 'Erreur lors de la recharge');
    }
  }
}
