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

  isSignUpMode = false; // Toggle between Login and Register (Create Account)
  isLoading = false;

  // Form groups
  loginForm!: FormGroup;
  registerForm!: FormGroup;

  // Password visibility
  passwordVisible = false;
  confirmPasswordVisible = false;

  returnUrl = '/admin/dashboard';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private message: NzMessageService
  ) {}

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/admin/dashboard']);
      return;
    }

    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/admin/dashboard';

    // Init Login Form
    this.loginForm = this.fb.group({
      username: ['admin', [Validators.required, Validators.minLength(3)]],
      password: ['Admin@123', [Validators.required, Validators.minLength(6)]],
      rememberMe: [true]
    });

    // Init Register Form
    this.registerForm = this.fb.group({
      fullName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      confirmPassword: ['', [Validators.required, Validators.minLength(6)]],
      agreeTerms: [true, [Validators.requiredTrue]]
    });
  }

  toggleMode(): void {
    this.isSignUpMode = !this.isSignUpMode;
  }

  onLogin(): void {
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
        this.message.success(`Đăng nhập thành công! Chào mừng ${username} quay trở lại.`);
        this.router.navigate([this.returnUrl]);
      },
      error: (err) => {
        this.isLoading = false;
        const msg = err?.error?.message || 'Sai tên đăng nhập hoặc mật khẩu. Vui lòng thử lại!';
        this.message.error(msg);
      }
    });
  }

  onRegister(): void {
    if (this.registerForm.invalid) {
      Object.values(this.registerForm.controls).forEach(ctrl => {
        ctrl.markAsDirty();
        ctrl.updateValueAndValidity({ onlySelf: true });
      });
      return;
    }

    const { username, email, password, confirmPassword } = this.registerForm.value;

    if (password !== confirmPassword) {
      this.message.error('Mật khẩu xác nhận không trùng khớp!');
      return;
    }

    this.isLoading = true;
    this.authService.register(username, email, password).subscribe({
      next: () => {
        this.isLoading = false;
        this.message.success('Đăng ký tài khoản thành công! Đang tự động đăng nhập...');
        
        // Auto login after registration
        this.authService.login(username, password).subscribe({
          next: () => {
            this.router.navigate([this.returnUrl]);
          },
          error: () => {
            this.isSignUpMode = false;
            this.loginForm.patchValue({ username, password });
          }
        });
      },
      error: (err) => {
        this.isLoading = false;
        const msg = err?.error?.message || 'Đăng ký thất bại. Tên đăng nhập hoặc email đã tồn tại!';
        this.message.error(msg);
      }
    });
  }

  onSocialLogin(provider: string): void {
    this.message.info(`Đang kết nối xác thực ${provider} SSO...`);
  }
}

