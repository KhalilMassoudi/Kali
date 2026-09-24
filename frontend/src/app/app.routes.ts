import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/login/login').then((m) => m.Login),
  },
  {
    path: 'register',
    loadComponent: () => import('./features/register/register').then((m) => m.Register),
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/forgot-password/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./features/reset-password/reset-password').then((m) => m.ResetPassword),
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell/shell').then((m) => m.Shell),
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard) },
      { path: 'vms', loadComponent: () => import('./features/vms/vms').then((m) => m.Vms) },
      { path: 'vms/:id', loadComponent: () => import('./features/vms/vm-detail/vm-detail').then((m) => m.VmDetail) },
      { path: 'volumes', loadComponent: () => import('./features/volumes/volumes').then((m) => m.Volumes) },
      { path: 'networks', loadComponent: () => import('./features/network-hub/network-hub').then((m) => m.NetworkHub) },
      { path: 'security-groups', redirectTo: 'networks', pathMatch: 'full' },
      { path: 'projects', loadComponent: () => import('./features/projects/projects').then((m) => m.Projects) },
      { path: 'keypairs', loadComponent: () => import('./features/keypairs/keypairs').then((m) => m.Keypairs) },
      {
        path: 'server-groups',
        loadComponent: () => import('./features/server-groups/server-groups').then((m) => m.ServerGroups),
      },
      { path: 'chat', loadComponent: () => import('./features/chat/chat').then((m) => m.Chat) },
      { path: 'support', loadComponent: () => import('./features/support/support-list/support-list').then((m) => m.SupportList) },
      { path: 'support/new', loadComponent: () => import('./features/support/support-new/support-new').then((m) => m.SupportNew) },
      { path: 'support/:id', loadComponent: () => import('./features/support/support-detail/support-detail').then((m) => m.SupportDetail) },
      { path: 'settings', loadComponent: () => import('./features/settings/settings').then((m) => m.Settings) },
      {
        path: 'admin/smtp',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/smtp-config/smtp-config').then((m) => m.SmtpConfigPage),
      },
      {
        path: 'admin/users',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/users/users').then((m) => m.AdminUsers),
      },
      {
        path: 'admin/telemetry',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/telemetry/telemetry').then((m) => m.AdminTelemetry),
      },
      {
        path: 'admin/compute',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/compute/compute').then((m) => m.AdminCompute),
      },
      {
        path: 'admin/volumes',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/volumes/volumes').then((m) => m.AdminVolumes),
      },
      {
        path: 'admin/network',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/network/network').then((m) => m.AdminNetwork),
      },
      {
        path: 'admin/logs',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/logs/logs').then((m) => m.AdminLogs),
      },
      {
        path: 'admin/alerts',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/alerts/alerts').then((m) => m.AdminAlerts),
      },
      {
        path: 'admin/support/agents',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/support/admin-support-agents/admin-support-agents').then((m) => m.AdminSupportAgents),
      },
      {
        path: 'admin/support/new',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/support/admin-support-new/admin-support-new').then((m) => m.AdminSupportNew),
      },
      {
        path: 'admin/support/:id',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/support/admin-support-detail/admin-support-detail').then((m) => m.AdminSupportDetail),
      },
      {
        path: 'admin/support',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/admin/support/admin-support-list/admin-support-list').then((m) => m.AdminSupportList),
      },
      {
        path: 'admin/images',
        canActivate: [adminGuard],
        loadComponent: () => import('./features/admin/images/images').then((m) => m.AdminImages),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];