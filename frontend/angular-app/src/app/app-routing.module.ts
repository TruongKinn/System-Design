import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './pages/login/login.component';
import { AdminLayoutComponent } from './layout/admin-layout/admin-layout.component';
import { AuthGuard } from './core/guards/auth.guard';

const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [AuthGuard],
    children: [
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./pages/dashboard/dashboard.module').then(m => m.DashboardModule)
      },
      {
        path: 'jobs',
        loadChildren: () =>
          import('./pages/jobs/jobs.module').then(m => m.JobsModule)
      },
      {
        path: 'exports',
        loadChildren: () =>
          import('./pages/exports/exports.module').then(m => m.ExportsModule)
      },
      {
        path: 'imports',
        loadChildren: () =>
          import('./pages/imports/imports.module').then(m => m.ImportsModule)
      },
      {
        path: 'users',
        loadChildren: () =>
          import('./pages/users/users.module').then(m => m.UsersModule)
      },
      {
        path: 'rbac',
        loadChildren: () =>
          import('./pages/rbac/rbac.module').then(m => m.RbacModule)
      },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: '**', redirectTo: '/login' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
