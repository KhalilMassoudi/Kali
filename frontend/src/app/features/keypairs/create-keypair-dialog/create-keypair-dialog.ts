import { Component, EventEmitter, Output, inject, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { KeypairService } from '../../../core/services/keypair.service';

type Mode = 'generate' | 'import';

@Component({
  selector: 'app-create-keypair-dialog',
  imports: [CommonModule, ReactiveFormsModule, DialogModule, InputTextModule, TextareaModule, ButtonModule, MessageModule],
  templateUrl: './create-keypair-dialog.html',
  styleUrl: './create-keypair-dialog.scss',
})
export class CreateKeypairDialog {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly keypairService = inject(KeypairService);

  readonly visible = model.required<boolean>();
  @Output() readonly created = new EventEmitter<void>();

  readonly loading = this.keypairService.loading;
  readonly error = signal('');
  readonly mode = signal<Mode>('generate');
  readonly revealedPrivateKey = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    publicKey: [''],
  });

  close(): void {
    this.visible.set(false);
    this.revealedPrivateKey.set(null);
    this.mode.set('generate');
    this.form.reset({ name: '', publicKey: '' });
  }

  async submit(): Promise<void> {
    this.error.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.mode() === 'import' && !this.form.controls.publicKey.value.trim()) {
      this.error.set('Collez votre clé publique ou choisissez "Générer une nouvelle clé"');
      return;
    }
    const user = this.auth.user();
    const { name, publicKey } = this.form.getRawValue();
    try {
      const result = await this.keypairService.createKeypair({
        userId: user?.id ?? 1,
        name: name.trim(),
        publicKey: this.mode() === 'import' ? publicKey.trim() : undefined,
      });
      this.created.emit();
      if (result.privateKey) {
        this.revealedPrivateKey.set(result.privateKey);
      } else {
        this.close();
      }
    } catch {
      this.error.set(this.keypairService.error() || 'Erreur lors de la création de la paire de clés');
    }
  }

  async copyPrivateKey(): Promise<void> {
    const key = this.revealedPrivateKey();
    if (key) await navigator.clipboard.writeText(key);
  }
}
