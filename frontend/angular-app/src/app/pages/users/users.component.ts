import { Component, OnInit } from '@angular/core';
import { UserService, User } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { NzMessageService } from 'ng-zorro-antd/message';

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css']
})
export class UsersComponent implements OnInit {
  users: User[] = [];
  loading = false;
  isAdmin = false;

  // Map lưu trạng thái ẩn/hiện password cho từng user id
  passwordVisibilityMap: { [userId: number]: boolean } = {};

  // Reset Password Modal state (Dành cho Admin đổi pass cho user)
  isResetModalVisible = false;
  isResetSubmitting = false;
  resetUser: User | null = null;
  newPassword = '';
  confirmPassword = '';
  newPasswordVisible = false;

  // Create User Modal state
  isModalVisible = false;
  isSubmitting = false;
  passwordVisible = false;
  username = '';
  fullName = '';
  email = '';
  password = '';
  selectedRoles: string[] = ['ROLE_OPERATOR'];

  constructor(
    private userService: UserService,
    private authService: AuthService,
    private message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.hasRole('ROLE_ADMIN');
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getUsers().subscribe({
      next: (data) => {
        this.users = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  openCreateModal(): void {
    this.username = '';
    this.fullName = '';
    this.email = '';
    this.password = '';
    this.selectedRoles = ['ROLE_OPERATOR'];
    this.isModalVisible = true;
  }

  handleCreate(): void {
    if (!this.username.trim() || !this.email.trim()) {
      this.message.error('Vui lòng điền đầy đủ tên đăng nhập và email!');
      return;
    }
    if (!this.password || this.password.length < 6) {
      this.message.error('Mật khẩu phải có ít nhất 6 ký tự!');
      return;
    }

    this.isSubmitting = true;
    this.userService.createUser({
      username: this.username.trim(),
      fullName: this.fullName.trim() || this.username.trim(),
      email: this.email.trim(),
      password: this.password,
      roles: this.selectedRoles
    }).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.message.success('Tạo tài khoản người dùng thành công!');
        this.isModalVisible = false;
        this.username = '';
        this.fullName = '';
        this.email = '';
        this.password = '';
        this.loadUsers();
      },
      error: (err) => {
        this.isSubmitting = false;
        console.error('Error creating user:', err);
        const errMsg = err?.error?.message || (typeof err?.error === 'string' ? err.error : null) || err?.message || 'Có lỗi khi tạo tài khoản. Vui lòng kiểm tra lại!';
        this.message.error(errMsg);
      }
    });
  }

  toggleStatus(user: User): void {
    this.userService.toggleUserStatus(user.id).subscribe(() => {
      const action = user.status === 'ACTIVE' ? 'Khóa' : 'Mở khóa';
      this.message.success(`Đã ${action} tài khoản [${user.username}]`);
      this.loadUsers();
    });
  }

  getRoleTagColor(role: string): string {
    switch (role) {
      case 'ROLE_ADMIN': return 'magenta';
      case 'ROLE_OPERATOR': return 'blue';
      case 'ROLE_VIEWER': return 'cyan';
      default: return 'default';
    }
  }

  togglePasswordVisibility(user: User): void {
    this.passwordVisibilityMap[user.id] = !this.passwordVisibilityMap[user.id];
  }

  isPasswordVisible(user: User): boolean {
    return !!this.passwordVisibilityMap[user.id];
  }

  copyPassword(pwd: string | undefined): void {
    if (!pwd) {
      this.message.warning('Mật khẩu trống hoặc chưa được thiết lập');
      return;
    }
    navigator.clipboard.writeText(pwd).then(() => {
      this.message.success('Đã sao chép mật khẩu vào bộ nhớ tạm!');
    }).catch(() => {
      this.message.info('Mật khẩu: ' + pwd);
    });
  }

  openResetModal(user: User): void {
    this.resetUser = user;
    this.newPassword = '';
    this.confirmPassword = '';
    this.newPasswordVisible = false;
    this.isResetModalVisible = true;
  }

  handleResetPassword(): void {
    if (!this.resetUser) return;
    if (!this.newPassword || this.newPassword.length < 6) {
      this.message.error('Mật khẩu mới phải có ít nhất 6 ký tự!');
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.message.error('Mật khẩu xác nhận không trùng khớp!');
      return;
    }

    this.isResetSubmitting = true;
    this.userService.resetPassword(this.resetUser.id, this.newPassword).subscribe({
      next: () => {
        this.isResetSubmitting = false;
        this.message.success(`Đổi mật khẩu cho tài khoản [${this.resetUser?.username}] thành công!`);
        this.isResetModalVisible = false;
        this.loadUsers();
      },
      error: (err) => {
        this.isResetSubmitting = false;
        console.error('Error resetting password:', err);
        const errMsg = err?.error?.message || 'Có lỗi xảy ra khi đổi mật khẩu!';
        this.message.error(errMsg);
      }
    });
  }

  formatDate(val: string | undefined): string {
    if (!val) return 'Chưa có';
    try {
      const cleaned = val.replace(/\.(\d{3})\d+/, '.$1');
      const d = new Date(cleaned);
      if (isNaN(d.getTime())) return val;
      const pad = (n: number) => (n < 10 ? '0' + n : n);
      return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
    } catch {
      return val;
    }
  }
}
