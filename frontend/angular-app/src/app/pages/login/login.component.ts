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
        const msg = err?.error?.message || 'Sai tên đăng nhập hoặc mật khẩu. Vui lòng thử lại!';
        this.message.error(msg);
      }
    });
  }
}
