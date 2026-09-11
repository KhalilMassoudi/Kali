import { Component, EventEmitter, Output, inject, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { ProjectService } from '../../../core/services/project.service';

@Component({
  selector: 'app-create-project-dialog',
  imports: [CommonModule, ReactiveFormsModule, DialogModule, InputTextModule, ButtonModule, MessageModule],
  templateUrl: './create-project-dialog.html',
  styleUrl: './create-project-dialog.scss',
})
export class CreateProjectDialog {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly projectService = inject(ProjectService);

  readonly visible = model.required<boolean>();
  @Output() readonly created = new EventEmitter<void>();

  readonly loading = this.projectService.loading;
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    description: [''],
  });

  get nameError(): string | null {
    const c = this.form.controls.name;
    if (!c.dirty || !c.errors) return null;
    if (c.errors['required']) return 'Le nom est obligatoire';
    if (c.errors['minlength']) return 'Minimum 2 caractères';
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
    const { name, description } = this.form.getRawValue();
    try {
      await this.projectService.createProject({ userId: user?.id ?? 1, name: name.trim(), description: description.trim() || undefined });
      this.created.emit();
      this.close();
      this.form.reset({ name: '', description: '' });
    } catch {
      this.error.set(this.projectService.error() || 'Erreur lors de la création du projet');
    }
  }
}
