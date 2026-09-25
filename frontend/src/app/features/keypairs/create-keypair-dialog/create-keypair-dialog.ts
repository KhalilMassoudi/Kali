import { Component, EventEmitter, Output, computed, inject, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { AdminService } from '../../../core/services/admin.service';
import { KeypairService } from '../../../core/services/keypair.service';

type Mode = 'generate' | 'import';

@Component({
  selector: 'app-create-keypair-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    DialogModule,
    InputTextModule,
    TextareaModule,
    SelectModule,
    ButtonModule,
    MessageModule,
  ],
  templateUrl: './create-keypair-dialog.html',
  styleUrl: './create-keypair-dialog.scss',
})
export class CreateKeypairDialog {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly admin = inject(AdminService);
  private readonly keypairService = inject(KeypairService);

  readonly visible = model.required<boolean>();
  @Output() readonly created = new EventEmitter<void>();

  readonly isAdmin = computed(() => this.auth.user()?.role === 'ADMIN');
  readonly clientOptions = computed(() =>
    this.admin.users().map((u) => ({ label: `${u.email}${u.firstName ? ' — ' + u.firstName : ''}`, value: u.id })),
  );
  readonly targetUserId = signal<number | null>(null);

  readonly loading = this.keypairService.loading;
  readonly error = signal('');
  readonly mode = signal<Mode>('generate');
  readonly revealedPrivateKey = signal<string | null>(null);

  constructor() {
    this.targetUserId.set(this.auth.user()?.id ?? null);
    if (this.isAdmin() && this.admin.users().length === 0) {
      this.admin.loadUsers();
    }
  }

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    publicKey: [''],
  });

  onTargetUserChange(userId: number): void {
    this.targetUserId.set(userId);
  }

  close(): void {
    this.visible.set(false);
    this.revealedPrivateKey.set(null);
    this.mode.set('generate');
    this.form.reset({ name: '', publicKey: '' });
    this.targetUserId.set(this.auth.user()?.id ?? null);
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
        userId: this.targetUserId() ?? user?.id ?? 1,
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
