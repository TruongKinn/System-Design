import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService, AuthUser } from '../../core/services/auth.service';

export interface MenuBadge {
  count: number;
  color: string;
}

export interface MenuItem {
  key: string;
  icon: string;
  label: string;
  route?: string;
  badge?: MenuBadge | null;
  children?: MenuItem[];
}

export interface MenuGroup {
  title: string;
  items: MenuItem[];
}

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

  // MetisMenu Style Accordion Items
  menuGroups: MenuGroup[] = [
    {
      title: 'CHÍNH',
      items: [
        {
          key: 'dashboard',
          icon: 'dashboard',
          label: 'Bảng Điều Khiển',
          route: '/admin/dashboard',
          badge: null
        }
      ]
    },
    {
      title: 'TÁC VỤ & LẬP LỊCH',
      items: [
        {
          key: 'job-group',
          icon: 'schedule',
          label: 'Quản Lý Cron Jobs',
          children: [
            { key: 'jobs', icon: 'schedule', label: 'Danh Sách Cron Jobs', route: '/admin/jobs', badge: { count: 3, color: '#6366f1' } }
          ]
        }
      ]
    },
    {
      title: 'XỬ LÝ DỮ LIỆU LỚN',
      items: [
        {
          key: 'data-group',
          icon: 'cloud-server',
          label: 'Xuất / Nhập Dữ Liệu',
          children: [
            { key: 'exports', icon: 'export', label: 'Xuất Dữ Liệu Lớn (Async)', route: '/admin/exports' },
            { key: 'imports', icon: 'import', label: 'Nhập Excel Streaming', route: '/admin/imports' }
          ]
        }
      ]
    },
    {
      title: 'QUẢN TRỊ HỆ THỐNG',
      items: [
        {
          key: 'system-group',
          icon: 'team',
          label: 'Người Dùng & Phân Quyền',
          children: [
            { key: 'users', icon: 'user', label: 'Quản Lý Người Dùng', route: '/admin/users' },
            { key: 'rbac', icon: 'safety', label: 'Phân Quyền RBAC', route: '/admin/rbac' }
          ]
        }
      ]
    }
  ];

  breadcrumbs: string[] = ['Admin', 'Dashboard'];

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.currentUser = this.authService.currentUser;
    const savedTheme = localStorage.getItem('dataflow_theme');
    if (savedTheme) {
      this.isDarkMode = savedTheme === 'dark';
    }
    this.applyTheme();
  }

  toggleTheme(): void {
    this.isDarkMode = !this.isDarkMode;
    localStorage.setItem('dataflow_theme', this.isDarkMode ? 'dark' : 'light');
    this.applyTheme();
  }

  private applyTheme(): void {
    const theme = this.isDarkMode ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', theme);
    document.body.setAttribute('data-theme', theme);
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
