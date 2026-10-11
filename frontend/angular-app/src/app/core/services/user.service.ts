import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
  private rolesUrl = 'http://localhost:8080/api/roles';

  constructor(private http: HttpClient) {}

  getUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.apiUrl);
  }

  createUser(userData: Partial<User>): Observable<User> {
    return this.http.post<User>(this.apiUrl, userData);
  }

  toggleUserStatus(id: number): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/toggle-status`, {});
  }

  resetPassword(id: number, newPassword: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}/reset-password`, { newPassword });
  }

  changeMyPassword(oldPassword: string, newPassword: string): Observable<any> {
    return this.http.post<any>('http://localhost:8080/api/auth/change-password', { oldPassword, newPassword });
  }

  getRoles(): Observable<RolePermission[]> {
    return this.http.get<RolePermission[]>(this.rolesUrl);
  }

  updateRolePermissions(roleId: number, permissions: string[]): Observable<RolePermission> {
    return this.http.put<RolePermission>(`${this.rolesUrl}/${roleId}/permissions`, permissions);
  }
}
