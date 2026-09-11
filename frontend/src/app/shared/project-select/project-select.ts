import { Component, input, model } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SelectModule } from 'primeng/select';
import { ClientProject } from '../../core/models/project.model';

interface ProjectOption {
  label: string;
  value: number | null;
}

@Component({
  selector: 'app-project-select',
  imports: [CommonModule, FormsModule, SelectModule],
  template: `
    <p-select
      [options]="options()"
      optionLabel="label"
      optionValue="value"
      [ngModel]="value()"
      (ngModelChange)="value.set($event)"
      placeholder="Sans projet"
      styleClass="project-select"
      [fluid]="true"
    />
  `,
})
export class ProjectSelect {
  readonly projects = input.required<ClientProject[]>();
  readonly value = model<number | null>(null);

  options(): ProjectOption[] {
    return [{ label: 'Sans projet', value: null }, ...this.projects().map((p) => ({ label: p.name, value: p.id }))];
  }
}
