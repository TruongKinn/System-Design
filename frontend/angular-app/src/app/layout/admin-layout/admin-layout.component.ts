import { Component, OnInit, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService, AuthUser } from '../../core/services/auth.service';
import { NzMessageService } from 'ng-zorro-antd/message';

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

  // Sidebar hover & collapse state (Default collapsed for hover-to-open interaction)
  isCollapsed = true;
  isPinned = false;
  hoverTimeout: any;

  isDarkMode = true;
  currentUser: AuthUser | null = null;
  notificationCount = 3;

  // Profile Modal State
  isProfileModalVisible = false;

  // Settings Modal State
  isSettingsModalVisible = false;
  refreshInterval = '5';
  enableNotificationSound = true;

  // Quick Search
  searchKeyword = '';

  @HostListener('window:resize')
  onResize(): void {
    if (window.innerWidth < 1024) {
      this.isPinned = false;
      this.isCollapsed = true;
    }
  }

  // MetisMenu Style Accordion Items
  menuGroups: MenuGroup[] = [
    {
      title: 'TỔNG QUAN & GIÁM SÁT',
      items: [
        {
          key: 'dashboard',
          icon: 'dashboard',
          label: 'Bảng Điều Khiển',
          route: '/admin/dashboard',
          badge: { count: 0, color: '#10b981' }
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
            { key: 'jobs', icon: 'schedule', label: 'Danh Sách Cron Jobs', route: '/admin/jobs', badge: { count: 4, color: '#6366f1' } }
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
      title: 'QUẢN TRỊ & BẢO MẬT',
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
    private router: Router,
    private message: NzMessageService
  ) {}

  ngOnInit(): void {
    // Mặc định thu gọn menu để rê chuột vào thì mở ra, không thì đóng lại luôn
    this.isCollapsed = true;
    this.isPinned = false;

    this.currentUser = this.authService.currentUser;
    const savedTheme = localStorage.getItem('dataflow_theme');
    if (savedTheme) {
      this.isDarkMode = savedTheme === 'dark';
    }
    this.applyTheme();

    this.updateBreadcrumbsByUrl(this.router.url);
    this.router.events.subscribe(() => {
      this.updateBreadcrumbsByUrl(this.router.url);
    });
  }

  private updateBreadcrumbsByUrl(url: string): void {
    if (url.includes('/admin/dashboard')) {
      this.breadcrumbs = ['Admin', 'Bảng Điều Khiển'];
    } else if (url.includes('/admin/jobs')) {
      this.breadcrumbs = ['Admin', 'Quản Lý Cron Jobs', 'Danh Sách Cron Jobs'];
    } else if (url.includes('/admin/exports')) {
      this.breadcrumbs = ['Admin', 'Xuất / Nhập Dữ Liệu', 'Xuất Dữ Liệu Lớn'];
    } else if (url.includes('/admin/imports')) {
      this.breadcrumbs = ['Admin', 'Xuất / Nhập Dữ Liệu', 'Nhập Excel Streaming'];
    } else if (url.includes('/admin/users')) {
      this.breadcrumbs = ['Admin', 'Người Dùng & Phân Quyền', 'Quản Lý Người Dùng'];
    } else if (url.includes('/admin/rbac')) {
      this.breadcrumbs = ['Admin', 'Người Dùng & Phân Quyền', 'Phân Quyền RBAC'];
    } else {
      this.breadcrumbs = ['Admin', 'Hệ Thống'];
    }
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

  clearNotifications(): void {
    this.notificationCount = 0;
    this.message.success('Đã đánh dấu đọc toàn bộ thông báo hệ thống!');
  }

  openProfileModal(): void {
    this.currentUser = this.authService.currentUser;
    this.isProfileModalVisible = true;
  }

  openSettingsModal(): void {
    this.isSettingsModalVisible = true;
  }

  saveSettings(): void {
    this.isSettingsModalVisible = false;
    this.message.success('Đã lưu cấu hình tùy chọn hệ thống!');
  }

  onQuickSearch(): void {
    const q = (this.searchKeyword || '').trim().toLowerCase();
    if (!q) return;

    if (q.includes('user') || q.includes('nguoi') || q.includes('tai khoan')) {
      this.router.navigate(['/admin/users']);
    } else if (q.includes('job') || q.includes('cron') || q.includes('tac vu')) {
      this.router.navigate(['/admin/jobs']);
    } else if (q.includes('exp') || q.includes('xuat')) {
      this.router.navigate(['/admin/exports']);
    } else if (q.includes('imp') || q.includes('nhap') || q.includes('excel')) {
      this.router.navigate(['/admin/imports']);
    } else if (q.includes('rbac') || q.includes('quyen') || q.includes('role')) {
      this.router.navigate(['/admin/rbac']);
    } else if (q.includes('dash') || q.includes('tong quan')) {
      this.router.navigate(['/admin/dashboard']);
    } else {
      this.message.info(`Không tìm thấy trang khớp với từ khóa "${this.searchKeyword}"`);
    }
    this.searchKeyword = '';
  }

  logout(): void {
    this.authService.logout();
  }

  // Hover to expand / leave to collapse
  onSidebarHover(hovering: boolean): void {
    if (this.isPinned) return;

    if (hovering) {
      if (this.hoverTimeout) clearTimeout(this.hoverTimeout);
      this.isCollapsed = false;
    } else {
      // Delay nhẹ 120ms khi rê chuột ra ngoài để trải nghiệm cực kỳ êm ái
      this.hoverTimeout = setTimeout(() => {
        if (!this.isPinned) {
          this.isCollapsed = true;
        }
      }, 120);
    }
  }

  togglePin(): void {
    this.isPinned = !this.isPinned;
    if (this.isPinned) {
      this.isCollapsed = false;
      this.message.success('Đã ghim cố định Sidebar');
    } else {
      this.isCollapsed = true;
      this.message.info('Đã bật chế độ tự động mở khi rê chuột (Hover to expand)');
    }
  }

  toggleCollapse(): void {
    this.togglePin();
  }

  navigateTo(route: string, breadcrumb: string): void {
    this.router.navigate([route]);
    this.updateBreadcrumbsByUrl(route);
  }

  get avatarUrl(): string {
    const username = this.currentUser?.username || 'admin';
    return `https://api.dicebear.com/7.x/avataaars/svg?seed=${username}`;
  }
}

