import { Component, OnInit } from '@angular/core';
import { UserService, User } from '../../core/services/user.service';
import { NzMessageService } from 'ng-zorro-antd/message';

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.css']
})
export class UsersComponent implements OnInit {
  users: User[] = [];
  loading = false;

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
    private message: NzMessageService
  ) {}

  ngOnInit(): void {
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
        this.message.error(err?.error?.message || 'Có lỗi khi tạo tài khoản. Vui lòng thử lại!');
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
}
