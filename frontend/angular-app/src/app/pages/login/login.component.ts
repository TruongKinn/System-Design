import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { NzMessageService } from 'ng-zorro-antd/message';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {

  loginForm!: FormGroup;
  isLoading = false;
  passwordVisible = false;
  returnUrl = '/admin/dashboard';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private message: NzMessageService
  ) {}

  ngOnInit(): void {
    // Nếu đã đăng nhập, redirect sang dashboard
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/admin/dashboard']);
      return;
    }

    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/admin/dashboard';

    this.loginForm = this.fb.group({
      username: ['admin', [Validators.required, Validators.minLength(3)]],
      password: ['Admin@123', [Validators.required, Validators.minLength(6)]],
      rememberMe: [true]
    });
  }

  onSubmit(): void {
    if (this.loginForm.invalid) {
      Object.values(this.loginForm.controls).forEach(ctrl => {
        ctrl.markAsDirty();
        ctrl.updateValueAndValidity({ onlySelf: true });
      });
      return;
    }

    this.isLoading = true;
    const { username, password } = this.loginForm.value;

    this.authService.login(username, password).subscribe({
      next: () => {
        this.isLoading = false;
        this.message.success(`Xin chào, ${username}! Chào mừng đến DataFlow Platform Admin.`);
        this.router.navigate([this.returnUrl]);
      },
      error: (err) => {
        this.isLoading = false;
        // Demo mode: simulate login success nếu backend offline
        if (err.status === 0 || err.status === 503) {
          this.simulateDemoLogin(username);
        } else {
          this.message.error('Sai tên đăng nhập hoặc mật khẩu. Vui lòng thử lại.');
        }
      }
    });
  }

  private simulateDemoLogin(username: string): void {
    const demoToken = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MSwicm9sZXMiOlsiUk9MRV9BRE1JTiJdLCJwZXJtaXNzaW9ucyI6WyJKT0JfUkVBRCIsIkpPQl9FWEVDVVRFIiwiSk9CX01BTkFHRSIsIkVYUE9SVF9EQVRBIiwiSU1QT1JUX0RBVEEiXX0.DEMO';
    localStorage.setItem('access_token', demoToken);
    localStorage.setItem('current_user', JSON.stringify({
      userId: 1,
      username,
      email: `${username}@babysystem.com`,
      roles: ['ROLE_ADMIN'],
      permissions: ['JOB_READ', 'JOB_EXECUTE', 'JOB_MANAGE', 'EXPORT_DATA', 'IMPORT_DATA']
    }));
    this.message.success(`[DEMO MODE] Đăng nhập thành công với tài khoản: ${username}`);
    this.router.navigate([this.returnUrl]);
  }
}
