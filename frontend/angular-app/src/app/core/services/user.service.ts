import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface User {
  id: number;
  username: string;
  email: string;
  fullName: string;
  password?: string;
  roles: string[];
  status: 'ACTIVE' | 'LOCKED' | 'INACTIVE';
  lastLoginAt?: string;
  createdAt: string;
}

export interface RolePermission {
  id: number;
  roleName: string;
  description: string;
  permissions: string[];
  userCount: number;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = 'http://localhost:8080/api/users';

  private mockUsers: User[] = [
    {
      id: 1,
      username: 'admin',
      email: 'admin@dataflow.io',
      fullName: 'System Administrator',
      roles: ['ROLE_ADMIN', 'ROLE_OPERATOR'],
      status: 'ACTIVE',
      lastLoginAt: '2026-09-11 20:15:00',
      createdAt: '2026-01-01 00:00:00'
    },
    {
      id: 2,
      username: 'operator01',
      email: 'operator01@dataflow.io',
      fullName: 'Trần Văn Vận Hành',
      roles: ['ROLE_OPERATOR'],
      status: 'ACTIVE',
      lastLoginAt: '2026-09-11 19:40:00',
      createdAt: '2026-03-15 09:30:00'
    },
    {
      id: 3,
      username: 'auditor_guest',
      email: 'auditor@external.com',
      fullName: 'Nguyễn Kiêm Kiểm Toán',
      roles: ['ROLE_VIEWER'],
      status: 'LOCKED',
      lastLoginAt: '2026-08-20 10:11:00',
      createdAt: '2026-06-01 11:20:00'
    }
  ];

  private mockRoles: RolePermission[] = [
    {
      id: 1,
      roleName: 'ROLE_ADMIN',
      description: 'Quyền Quản trị tối cao toàn bộ hệ thống DataFlow',
      permissions: ['JOB_CREATE', 'JOB_EDIT', 'JOB_TRIGGER', 'USER_MANAGE', 'SYSTEM_CONFIG', 'EXPORT_EXECUTE', 'IMPORT_EXECUTE'],
      userCount: 2
    },
    {
      id: 2,
      roleName: 'ROLE_OPERATOR',
      description: 'Quyền Vận hành tác vụ Job, Import/Export dữ liệu',
      permissions: ['JOB_TRIGGER', 'JOB_PAUSE', 'EXPORT_EXECUTE', 'IMPORT_EXECUTE'],
      userCount: 8
    },
    {
      id: 3,
      roleName: 'ROLE_VIEWER',
      description: 'Quyền Chỉ xem báo cáo và giám sát Dashboard',
      permissions: ['DASHBOARD_VIEW', 'LOG_VIEW'],
      userCount: 15
    }
  ];

  constructor(private http: HttpClient) {}

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl).pipe(
      catchError(() => of(this.mockUsers))
    );
  }

  createUser(userData: Partial<User>): Observable<User> {
    return this.http.post<User>(this.apiUrl, userData).pipe(
      catchError(() => {
        const newUser: User = {
          id: this.mockUsers.length + 1,
          username: userData.username || 'newuser',
          email: userData.email || 'user@dataflow.io',
          fullName: userData.fullName || 'New User',
          roles: userData.roles || ['ROLE_VIEWER'],
          status: 'ACTIVE',
          createdAt: new Date().toISOString().replace('T', ' ').substring(0, 19)
        };
        this.mockUsers.unshift(newUser);
        return of(newUser);
      })
    );
  }

  toggleUserStatus(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/toggle-status`, {}).pipe(
      catchError(() => {
        const user = this.mockUsers.find(u => u.id === id);
        if (user) {
          user.status = user.status === 'ACTIVE' ? 'LOCKED' : 'ACTIVE';
        }
        return of(true);
      })
    );
  }

  getRoles(): Observable<RolePermission[]> {
    return this.http.get<RolePermission[]>('/api/v1/roles').pipe(
      catchError(() => of(this.mockRoles))
    );
  }
}
