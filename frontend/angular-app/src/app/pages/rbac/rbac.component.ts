import { Component, OnInit } from '@angular/core';
import { UserService, RolePermission } from '../../core/services/user.service';
import { NzMessageService } from 'ng-zorro-antd/message';

interface PermissionItem {
  key: string;
  label: string;
  group: 'JOB' | 'USER' | 'DATA_EXPORT' | 'DATA_IMPORT' | 'SYSTEM';
}

@Component({
  selector: 'app-rbac',
  templateUrl: './rbac.component.html',
  styleUrls: ['./rbac.component.css']
})
export class RbacComponent implements OnInit {
  roles: RolePermission[] = [];
  loading = false;

  allPermissions: PermissionItem[] = [
    { key: 'JOB_CREATE', label: 'Tạo mới Quartz Cron Job', group: 'JOB' },
    { key: 'JOB_EDIT', label: 'Sửa / Cập nhật cấu hình Job', group: 'JOB' },
    { key: 'JOB_TRIGGER', label: 'Chạy thủ công (Trigger Run Now)', group: 'JOB' },
    { key: 'JOB_PAUSE', label: 'Tạm dừng / Kích hoạt Job', group: 'JOB' },
    { key: 'USER_MANAGE', label: 'Quản lý tài khoản & phân quyền', group: 'USER' },
    { key: 'EXPORT_EXECUTE', label: 'Khởi tạo tác vụ Export Big Data', group: 'DATA_EXPORT' },
    { key: 'IMPORT_EXECUTE', label: 'Upload & Nạp batch dữ liệu', group: 'DATA_IMPORT' },
    { key: 'SYSTEM_CONFIG', label: 'Thay đổi tham số cấu hình hệ thống', group: 'SYSTEM' }
  ];

  // Edit Role Modal state
  isModalVisible = false;
  selectedRole?: RolePermission;
  editedPermissions: string[] = [];

  constructor(
    private userService: UserService,
    private message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.loadRoles();
  }

  loadRoles(): void {
    this.loading = true;
    this.userService.getRoles().subscribe({
      next: (data) => {
        this.roles = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  openEditModal(role: RolePermission): void {
    this.selectedRole = role;
    this.editedPermissions = [...role.permissions];
    this.isModalVisible = true;
  }

  hasPermission(permKey: string): boolean {
    return this.editedPermissions.includes(permKey);
  }

  togglePermission(permKey: string): void {
    if (this.hasPermission(permKey)) {
      this.editedPermissions = this.editedPermissions.filter(p => p !== permKey);
    } else {
      this.editedPermissions.push(permKey);
    }
  }

  handleSavePermissions(): void {
    if (this.selectedRole) {
      this.selectedRole.permissions = [...this.editedPermissions];
      this.message.success(`Đã cập nhật danh sách quyền hạn cho vai trò [${this.selectedRole.roleName}]!`);
      this.isModalVisible = false;
    }
  }
}
