import { Component, EventEmitter, Output, inject, input, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { InputNumberModule } from 'primeng/inputnumber';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { VolumeService } from '../../../core/services/volume.service';

@Component({
  selector: 'app-create-volume-dialog',
  imports: [CommonModule, ReactiveFormsModule, DialogModule, InputTextModule, InputNumberModule, ButtonModule, MessageModule],
  templateUrl: './create-volume-dialog.html',
  styleUrl: './create-volume-dialog.scss',
})
export class CreateVolumeDialog {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly volumeService = inject(VolumeService);

  readonly visible = model.required<boolean>();
  readonly targetUserId = input<number | null>(null);
  @Output() readonly created = new EventEmitter<void>();

  readonly loading = this.volumeService.loading;
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-z0-9-]+$/)]],
    sizeGb: [20, [Validators.required, Validators.min(1)]],
  });

  get nameError(): string | null {
    const c = this.form.controls.name;
    if (!c.dirty || !c.errors) return null;
    if (c.errors['required']) return 'Le nom est obligatoire';
    if (c.errors['minlength']) return 'Minimum 3 caractères';
    if (c.errors['pattern']) return 'Lettres minuscules, chiffres et tirets uniquement';
    return null;
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
    const { name, sizeGb } = this.form.getRawValue();
    try {
      await this.volumeService.createVolume({ userId: this.targetUserId() ?? user?.id ?? 1, name: name.trim(), sizeGb });
      this.created.emit();
      this.close();
      this.form.reset({ name: '', sizeGb: 20 });
    } catch {
      this.error.set(this.volumeService.error() || 'Erreur lors de la création du volume');
    }
  }
}
