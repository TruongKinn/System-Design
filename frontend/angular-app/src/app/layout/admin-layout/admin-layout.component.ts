import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService, AuthUser } from '../../core/services/auth.service';

@Component({
  selector: 'app-admin-layout',
  templateUrl: './admin-layout.component.html',
  styleUrls: ['./admin-layout.component.css']
})
export class AdminLayoutComponent implements OnInit {

  isCollapsed = false;
  isDarkMode = true;
  currentUser: AuthUser | null = null;
  notificationCount = 3;

  menuItems = [
    {
      key: 'dashboard',
      icon: 'dashboard',
      label: 'Bảng Điều Khiển',
      route: '/admin/dashboard',
      badge: null
    },
    {
      key: 'jobs',
      icon: 'schedule',
      label: 'Quản Lý Cron Jobs',
      route: '/admin/jobs',
      badge: { count: 3, color: '#6366f1' }
    },
    {
      key: 'exports',
      icon: 'export',
      label: 'Xuất Dữ Liệu Lớn',
      route: '/admin/exports',
      badge: null
    },
    {
      key: 'imports',
      icon: 'import',
      label: 'Nhập Excel Streaming',
      route: '/admin/imports',
      badge: null
    },
    {
      key: 'users',
      icon: 'team',
      label: 'Quản Lý Người Dùng',
      route: '/admin/users',
      badge: null
    },
    {
      key: 'rbac',
      icon: 'safety',
      label: 'Phân Quyền RBAC',
      route: '/admin/rbac',
      badge: null
    }
  ];

  breadcrumbs: string[] = ['Admin', 'Dashboard'];

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.currentUser;
  }

  logout(): void {
    this.authService.logout();
  }

  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
  }

  navigateTo(route: string, breadcrumb: string): void {
    this.router.navigate([route]);
    this.breadcrumbs = ['Admin', breadcrumb];
  }

  get avatarUrl(): string {
    const username = this.currentUser?.username || 'admin';
    return `https://api.dicebear.com/7.x/avataaars/svg?seed=${username}`;
  }
}
