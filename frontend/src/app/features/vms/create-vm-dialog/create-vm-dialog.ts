import { Component, EventEmitter, Output, computed, inject, model, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { MultiSelectModule } from 'primeng/multiselect';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { AdminService } from '../../../core/services/admin.service';
import { VmService } from '../../../core/services/vm.service';
import { ImageService } from '../../../core/services/image.service';
import { KeypairService } from '../../../core/services/keypair.service';
import { ServerGroupService } from '../../../core/services/server-group.service';
import { ProjectService } from '../../../core/services/project.service';
import { ImageOption, NetworkOption, SecurityGroupOption } from '../../../core/models/vm.model';

const OS_OPTIONS = [
  { value: 'ubuntu-22.04', label: 'Ubuntu 22.04 LTS — Recommandé' },
  { value: 'debian-11', label: 'Debian 11 Bullseye — Stable' },
  { value: 'centos-stream-9', label: 'CentOS Stream 9 — Enterprise' },
  { value: 'cirros', label: 'Cirros — Test léger' },
];

const RAM_OPTIONS = [
  { value: 512, label: '512 MB' },
  { value: 1024, label: '1 GB' },
  { value: 2048, label: '2 GB' },
  { value: 4096, label: '4 GB' },
  { value: 8192, label: '8 GB' },
];

const CPU_OPTIONS = [1, 2, 4, 8].map((v) => ({ value: v, label: `${v} vCPU${v > 1 ? 's' : ''}` }));
const STORAGE_OPTIONS = [10, 20, 50, 100, 200].map((v) => ({ value: v, label: `${v} GB` }));

type Source = 'catalog' | 'backup';

@Component({
  selector: 'app-create-vm-dialog',
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    ReactiveFormsModule,
    DialogModule,
    InputTextModule,
    SelectModule,
    MultiSelectModule,
    ButtonModule,
    MessageModule,
  ],
  templateUrl: './create-vm-dialog.html',
  styleUrl: './create-vm-dialog.scss',
})
export class CreateVmDialog {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly admin = inject(AdminService);
  private readonly vmService = inject(VmService);
  private readonly imageService = inject(ImageService);
  readonly keypairService = inject(KeypairService);
  readonly serverGroupService = inject(ServerGroupService);
  private readonly projectService = inject(ProjectService);

  readonly isAdmin = computed(() => this.auth.user()?.role === 'ADMIN');
  readonly clientOptions = computed(() =>
    this.admin.users().map((u) => ({ label: `${u.email}${u.firstName ? ' — ' + u.firstName : ''}`, value: u.id })),
  );
  readonly targetUserId = signal<number | null>(null);
  /** Admin only: the chosen client's projects, and which one to file the VM under. */
  readonly targetProjectId = signal<number | null>(null);
  readonly projectOptions = signal<{ label: string; value: number | null }[]>([{ label: 'Aucun projet', value: null }]);

  readonly visible = model.required<boolean>();
  @Output() readonly created = new EventEmitter<void>();

  // Catalog images (admin-managed) take priority; falls back to the legacy fixed list only
  // if the admin hasn't published anything yet, so VM creation never dead-ends.
  readonly catalogImages = this.imageService.publishedImages;
  readonly usingCatalog = computed(() => this.catalogImages().length > 0);
  readonly osOptions = computed(() =>
    this.usingCatalog()
      ? this.catalogImages().map((i) => ({ value: i.externalId, label: i.displayName }))
      : OS_OPTIONS,
  );
  readonly ramOptions = RAM_OPTIONS;
  readonly cpuOptions = CPU_OPTIONS;
  readonly storageOptions = STORAGE_OPTIONS;

  readonly source = signal<Source>('catalog');
  readonly myVms = this.vmService.vms;
  readonly backupSourceVmId = signal<number | null>(null);
  readonly backupOptions = signal<ImageOption[]>([]);
  readonly selectedBackupId = signal<string | null>(null);
  readonly backupsLoading = signal(false);

  readonly networkOptions = signal<NetworkOption[]>([]);
  readonly securityGroupOptions = signal<SecurityGroupOption[]>([]);
  readonly catalogLoading = signal(false);

  readonly loading = this.vmService.loading;
  readonly error = signal('');

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-z0-9-]+$/)]],
    os: ['', [Validators.required]],
    ram: [2048, [Validators.required]],
    cpu: [2, [Validators.required]],
    storage: [20, [Validators.required]],
    networkId: [''],
    securityGroups: this.fb.nonNullable.control<string[]>([]),
    keypairId: this.fb.control<number | null>(null),
    serverGroupId: this.fb.control<number | null>(null),
  });

  readonly keypairOptions = computed(() => [
    { label: 'Aucune', value: null },
    ...this.keypairService.keypairs().map((k) => ({ label: k.displayName, value: k.id })),
  ]);
  readonly serverGroupSelectOptions = computed(() => [
    { label: 'Aucun', value: null },
    ...this.serverGroupService.groups().map((g) => ({ label: g.name, value: g.id })),
  ]);

  constructor() {
    this.loadCatalog();
    this.imageService.fetchPublished().then(() => {
      if (this.usingCatalog() && !this.form.controls.os.value) {
        this.form.controls.os.setValue(this.osOptions()[0].value);
      }
    });
    this.form.controls.os.setValue(OS_OPTIONS[0].value);
    this.targetUserId.set(this.auth.user()?.id ?? null);
    this.loadKeypairsAndServerGroups();
    if (this.isAdmin() && this.admin.users().length === 0) {
      this.admin.loadUsers();
    }
    const initialUser = this.targetUserId();
    if (this.isAdmin() && initialUser) {
      this.loadTargetProjects(initialUser);
    }
  }

  private loadKeypairsAndServerGroups(): void {
    const userId = this.targetUserId() ?? this.auth.user()?.id;
    if (!userId) return;
    this.keypairService.fetchKeypairs(userId);
    this.serverGroupService.fetchGroups(userId);
  }

  onTargetUserChange(userId: number): void {
    this.targetUserId.set(userId);
    this.loadTargetProjects(userId);
    this.form.controls.keypairId.setValue(null);
    this.form.controls.serverGroupId.setValue(null);
    this.loadKeypairsAndServerGroups();
  }

  private async loadTargetProjects(userId: number): Promise<void> {
    this.targetProjectId.set(null);
    this.projectOptions.set([{ label: 'Aucun projet', value: null }]);
    const projects = await this.projectService.listForUser(userId);
    if (this.targetUserId() !== userId) return; // client changed while loading
    this.projectOptions.set([
      { label: 'Aucun projet', value: null },
      ...projects.map((p) => ({ label: p.name, value: p.id })),
    ]);
  }

  private async loadCatalog(): Promise<void> {
    this.catalogLoading.set(true);
    try {
      const [networks, groups] = await Promise.all([
        this.vmService.fetchNetworks(),
        this.vmService.fetchSecurityGroups(),
      ]);
      this.networkOptions.set(networks);
      this.securityGroupOptions.set(groups);
    } catch {
      // OpenStack unreachable — the network/security-group pickers just stay empty
      // and fall back to auto-pick behavior; not a blocking error for the dialog.
    } finally {
      this.catalogLoading.set(false);
    }
  }

  async selectBackupSourceVm(vpsId: number): Promise<void> {
    this.backupSourceVmId.set(vpsId);
    this.selectedBackupId.set(null);
    this.backupsLoading.set(true);
    try {
      this.backupOptions.set(await this.vmService.listBackups(vpsId));
    } catch {
      this.backupOptions.set([]);
    } finally {
      this.backupsLoading.set(false);
    }
  }

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

    if (this.source() === 'backup') {
      if (!this.selectedBackupId()) {
        this.error.set('Choisissez un snapshot/backup à redéployer');
        return;
      }
    }

    const { name } = this.form.getRawValue();
    if (!name || name.trim().length < 3 || !/^[a-z0-9-]+$/.test(name.trim())) {
      this.form.controls.name.markAsTouched();
      return;
    }
    if (this.source() === 'catalog' && this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const user = this.auth.user();
    const { os, ram, cpu, storage, networkId, securityGroups, keypairId, serverGroupId } = this.form.getRawValue();

    const imageId = this.source() === 'backup' ? this.selectedBackupId()! : this.usingCatalog() ? os : undefined;
    const osLabel = this.source() === 'backup'
      ? 'Redéploiement depuis snapshot'
      : this.usingCatalog()
        ? (this.osOptions().find((o) => o.value === os)?.label ?? os)
        : os;

    try {
      await this.vmService.createVm({
        userId: this.targetUserId() ?? user?.id ?? 1,
        name: name.trim(),
        os: osLabel,
        ram,
        cpu,
        storage,
        networkId: networkId || undefined,
        securityGroups: securityGroups.length ? securityGroups : undefined,
        imageId,
        keypairId: keypairId ?? undefined,
        serverGroupId: serverGroupId ?? undefined,
        projectId: this.isAdmin() ? (this.targetProjectId() ?? undefined) : undefined,
      });
      this.created.emit();
      this.close();
      this.form.reset({
        name: '',
        os: this.osOptions()[0]?.value ?? '',
        ram: 2048,
        cpu: 2,
        storage: 20,
        networkId: '',
        securityGroups: [],
        keypairId: null,
        serverGroupId: null,
      });
      this.targetUserId.set(this.auth.user()?.id ?? null);
      if (this.isAdmin() && this.targetUserId()) {
        this.loadTargetProjects(this.targetUserId()!);
      }
      this.source.set('catalog');
      this.selectedBackupId.set(null);
    } catch {
      this.error.set(this.vmService.error() || 'Erreur lors de la création');
    }
  }
}
