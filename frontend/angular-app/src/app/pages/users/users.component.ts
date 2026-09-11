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
  username = '';
  fullName = '';
  email = '';
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
    this.isModalVisible = true;
  }

  handleCreate(): void {
    if (!this.username.trim() || !this.email.trim()) {
      this.message.error('Vui lòng điền đầy đủ tên đăng nhập và email!');
      return;
    }
    this.userService.createUser({
      username: this.username,
      fullName: this.fullName,
      email: this.email,
      roles: this.selectedRoles
    }).subscribe(() => {
      this.message.success('Tạo tài khoản người dùng thành công!');
      this.isModalVisible = false;
      this.username = '';
      this.fullName = '';
      this.email = '';
      this.loadUsers();
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
