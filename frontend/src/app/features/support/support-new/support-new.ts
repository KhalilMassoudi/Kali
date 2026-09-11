import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { SelectModule } from 'primeng/select';
import { ButtonModule } from 'primeng/button';
import { SupportService } from '../../../core/services/support.service';
import { TicketCategory, TicketPriority } from '../../../core/models/support.model';

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
  selector: 'app-support-new',
  imports: [CommonModule, RouterLink, ReactiveFormsModule, InputTextModule, TextareaModule, SelectModule, ButtonModule],
  templateUrl: './support-new.html',
  styleUrl: './support-new.scss',
})
export class SupportNew {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly support = inject(SupportService);

  readonly priorityOptions = PRIORITY_OPTIONS;
  readonly categoryOptions = CATEGORY_OPTIONS;

  readonly submitting = signal(false);
  readonly formError = signal('');
  readonly selectedFile = signal<File | null>(null);

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile.set(input.files?.[0] ?? null);
  }

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required]],
    description: ['', [Validators.required]],
    priority: ['MEDIUM' as TicketPriority, [Validators.required]],
    category: ['OTHER' as TicketCategory, [Validators.required]],
  });

  async submit(): Promise<void> {
    this.formError.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    try {
      const ticket = await this.support.createTicket(this.form.getRawValue());
      const file = this.selectedFile();
      if (file) {
        await this.support.uploadAttachment(ticket.id, file);
      }
      this.router.navigate(['/support', ticket.id]);
    } catch {
      this.formError.set(this.support.error() || 'Erreur lors de la création du ticket');
    } finally {
      this.submitting.set(false);
    }
  }
}
