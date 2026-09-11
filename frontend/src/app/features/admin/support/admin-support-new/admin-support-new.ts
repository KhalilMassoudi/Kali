import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { AdminService } from '../../../../core/services/admin.service';
import { SupportService } from '../../../../core/services/support.service';
import { TicketCategory, TicketPriority } from '../../../../core/models/support.model';

const PRIORITY_OPTIONS: { label: string; value: TicketPriority }[] = [
  { label: 'Faible', value: 'LOW' },
  { label: 'Moyenne', value: 'MEDIUM' },
  { label: 'Haute', value: 'HIGH' },
  { label: 'Critique', value: 'CRITICAL' },
];

const CATEGORY_OPTIONS: { label: string; value: TicketCategory }[] = [
  { label: 'Facturation', value: 'BILLING' },
  { label: 'Technique', value: 'TECHNICAL' },
  { label: 'VPS', value: 'VPS' },
  { label: 'Domaine', value: 'DOMAIN' },
  { label: 'Kubernetes', value: 'KUBERNETES' },
  { label: 'Compte', value: 'ACCOUNT' },
  { label: 'Autre', value: 'OTHER' },
];

@Component({
  selector: 'app-admin-support-new',
  imports: [CommonModule, RouterLink, ReactiveFormsModule, InputTextModule, TextareaModule, SelectModule, ButtonModule],
  templateUrl: './admin-support-new.html',
  styleUrl: './admin-support-new.scss',
})
export class AdminSupportNew {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly support = inject(SupportService);
  readonly admin = inject(AdminService);

  readonly priorityOptions = PRIORITY_OPTIONS;
  readonly categoryOptions = CATEGORY_OPTIONS;

  readonly submitting = signal(false);
  readonly formError = signal('');
  readonly selectedFile = signal<File | null>(null);

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile.set(input.files?.[0] ?? null);
  }

  readonly clientOptions = () =>
    this.admin.users().map((u) => ({ label: `${u.email}${u.firstName ? ' — ' + u.firstName : ''}`, value: u.id }));

  readonly form = this.fb.nonNullable.group({
    userId: [null as number | null, [Validators.required]],
    title: ['', [Validators.required]],
    description: ['', [Validators.required]],
    priority: ['MEDIUM' as TicketPriority, [Validators.required]],
    category: ['OTHER' as TicketCategory, [Validators.required]],
  });

  constructor() {
    if (this.admin.users().length === 0) {
      this.admin.loadUsers();
    }
  }

  async submit(): Promise<void> {
    this.formError.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    try {
      const { userId, ...rest } = this.form.getRawValue();
      const ticket = await this.admin.createTicketForClient({ userId: userId!, ...rest });
      const file = this.selectedFile();
      if (file) {
        await this.support.uploadAttachment(ticket.id, file);
      }
      this.router.navigate(['/admin/support', ticket.id]);
    } catch {
      this.formError.set(this.admin.error() || 'Erreur lors de la création du ticket');
    } finally {
      this.submitting.set(false);
    }
  }
}
