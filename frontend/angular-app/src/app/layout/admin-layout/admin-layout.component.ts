import { Component, OnInit, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService, AuthUser } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
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

  // Trạng thái thu gọn/mở rộng Sidebar theo nút toggle
  isCollapsed = false;

  // Trạng thái hover mở rộng tạm thời khi sidebar đang thu gọn
  isHoverExpanded = false;
  hoverTimeout: any;

  isDarkMode = true;
  currentUser: AuthUser | null = null;
  notificationCount = 3;

  // Profile Modal State
  isProfileModalVisible = false;

  // Change Password State (User đổi mật khẩu của chính mình)
  isChangePasswordVisible = false;
  oldPassword = '';
  myNewPassword = '';
  myConfirmPassword = '';
  isPasswordSubmitting = false;
  oldPasswordVisible = false;
  newPasswordVisible = false;

  // Settings Modal State
  isSettingsModalVisible = false;
  refreshInterval = '5';
  enableNotificationSound = true;

  // Quick Search
  searchKeyword = '';

  @HostListener('window:resize')
  onResize(): void {
    if (window.innerWidth < 1024) {
      this.isCollapsed = true;
    }
  }

  // Cấu trúc Menu chuẩn: Menu Đơn & Menu Cha có Submenu con mở rộng (Đã bỏ group title ngoài cùng)
  menuItems: MenuItem[] = [
    {
      key: 'dashboard',
      icon: 'dashboard',
      label: 'Bảng Điều Khiển',
      route: '/admin/dashboard'
    },
    {
      key: 'jobs-parent',
      icon: 'schedule',
      label: 'Quản Lý Tác Vụ',
      children: [
        {
          key: 'jobs',
          icon: 'clock-circle',
          label: 'Danh Sách Cron Jobs',
          route: '/admin/jobs',
          badge: { count: 4, color: '#38bdf8' }
        }
      ]
    },
    {
      key: 'data-parent',
      icon: 'cloud-server',
      label: 'Xử Lý Dữ Liệu Lớn',
      children: [
        {
          key: 'exports',
          icon: 'export',
          label: 'Xuất Dữ Liệu (1M Rows)',
          route: '/admin/exports'
        },
        {
          key: 'imports',
          icon: 'import',
          label: 'Nhập Dữ Liệu (Streaming)',
          route: '/admin/imports'
        }
      ]
    },
    {
      key: 'admin-parent',
      icon: 'setting',
      label: 'Quản Trị & Bảo Mật',
      children: [
        {
          key: 'users',
          icon: 'team',
          label: 'Quản Lý Người Dùng',
          route: '/admin/users'
        },
        {
          key: 'rbac',
          icon: 'safety-certificate',
          label: 'Phân Quyền RBAC',
          route: '/admin/rbac'
        },
        {
          key: 'crypto',
          icon: 'lock',
          label: 'Mã Hóa & Giải Mã (Vault)',
          route: '/admin/crypto'
        }
      ]
    }
  ];

  breadcrumbs: string[] = ['Admin', 'Dashboard'];

  constructor(
    private authService: AuthService,
    private userService: UserService,
    private router: Router,
    private message: NzMessageService
  ) {}

  ngOnInit(): void {
    // Mặc định mở rộng menu để làm việc thuận tiện
    this.isCollapsed = false;

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
      this.breadcrumbs = ['Admin', 'Quản Lý Tác Vụ', 'Danh Sách Cron Jobs'];
    } else if (url.includes('/admin/exports')) {
      this.breadcrumbs = ['Admin', 'Xử Lý Dữ Liệu Lớn', 'Xuất Dữ Liệu (1M Rows)'];
    } else if (url.includes('/admin/imports')) {
      this.breadcrumbs = ['Admin', 'Xử Lý Dữ Liệu Lớn', 'Nhập Dữ Liệu (Streaming)'];
    } else if (url.includes('/admin/users')) {
      this.breadcrumbs = ['Admin', 'Quản Trị & Bảo Mật', 'Quản Lý Người Dùng'];
    } else if (url.includes('/admin/rbac')) {
      this.breadcrumbs = ['Admin', 'Quản Trị & Bảo Mật', 'Phân Quyền RBAC'];
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

  openChangePasswordModal(): void {
    this.oldPassword = '';
    this.myNewPassword = '';
    this.myConfirmPassword = '';
    this.oldPasswordVisible = false;
    this.newPasswordVisible = false;
    this.isChangePasswordVisible = true;
  }

  submitChangePassword(): void {
    if (!this.oldPassword) {
      this.message.error('Vui lòng nhập mật khẩu hiện tại!');
      return;
    }
    if (!this.myNewPassword || this.myNewPassword.length < 6) {
      this.message.error('Mật khẩu mới phải có ít nhất 6 ký tự!');
      return;
    }
    if (this.myNewPassword !== this.myConfirmPassword) {
      this.message.error('Mật khẩu xác nhận không trùng khớp!');
      return;
    }

    this.isPasswordSubmitting = true;
    this.userService.changeMyPassword(this.oldPassword, this.myNewPassword).subscribe({
      next: () => {
        this.isPasswordSubmitting = false;
        this.message.success('Đổi mật khẩu thành công! Vui lòng ghi nhớ mật khẩu mới.');
        this.isChangePasswordVisible = false;
        this.isProfileModalVisible = false;
      },
      error: (err) => {
        this.isPasswordSubmitting = false;
        console.error('Error changing password:', err);
        const errMsg = err?.error?.message || 'Có lỗi xảy ra khi đổi mật khẩu!';
        this.message.error(errMsg);
      }
    });
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

  // Trạng thái thu gọn thực tế (nếu đang hover mở rộng thì effectiveCollapsed = false)
  get effectiveCollapsed(): boolean {
    return this.isCollapsed && !this.isHoverExpanded;
  }

  // Tương tác rê chuột vào tự động mở rộng khi sidebar đang ở trạng thái thu gọn
  onSidebarHover(hovering: boolean): void {
    if (!this.isCollapsed) return; // Nếu sidebar đã được mở cố định thì không cần hover logic

    if (hovering) {
      if (this.hoverTimeout) clearTimeout(this.hoverTimeout);
      this.isHoverExpanded = true;
    } else {
      // Delay nhẹ 120ms khi rời chuột để không bị đóng giật
      this.hoverTimeout = setTimeout(() => {
        this.isHoverExpanded = false;
      }, 120);
    }
  }

  // Đóng/mở sidebar chuẩn theo click toggle button trên Header
  toggleCollapse(): void {
    this.isCollapsed = !this.isCollapsed;
    this.isHoverExpanded = false;
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

