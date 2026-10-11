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

  authForm!: FormGroup;

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

    this.authForm = this.fb.group({
      fullName: [''],
      email: [''],
      username: ['admin', [Validators.required, Validators.minLength(3)]],
      password: ['Admin@123', [Validators.required, Validators.minLength(6)]],
      confirmPassword: [''],
      rememberMe: [true],
      agreeTerms: [true]
    });
  }

  toggleMode(): void {
    this.isSignUpMode = !this.isSignUpMode;
    if (this.isSignUpMode) {
      if (!this.authForm.get('fullName')?.value) {
        this.authForm.patchValue({
          fullName: 'Alex Johnson',
          email: 'name@domain.com',
          username: 'alexjohnson',
          password: '',
          confirmPassword: ''
        });
      }
    } else {
      if (this.authForm.get('username')?.value === 'alexjohnson') {
        this.authForm.patchValue({
          username: 'admin',
          password: 'Admin@123'
        });
      }
    }
  }

  onSubmit(): void {
    const { fullName, email, username, password, confirmPassword, agreeTerms } = this.authForm.value;

    if (!this.isSignUpMode) {
      // Sign In Flow
      if (!username || !password) {
        this.message.error('Vui lòng nhập tên đăng nhập và mật khẩu!');
        return;
      }

      this.isLoading = true;
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
    } else {
      // Sign Up Flow
      if (!fullName || !fullName.trim()) {
        this.message.error('Vui lòng nhập họ và tên!');
        return;
      }
      if (!email || !email.includes('@')) {
        this.message.error('Vui lòng nhập email hợp lệ!');
        return;
      }
      if (!username || username.length < 3) {
        this.message.error('Tên đăng nhập tối thiểu 3 ký tự!');
        return;
      }
      if (!password || password.length < 6) {
        this.message.error('Mật khẩu tối thiểu 6 ký tự!');
        return;
      }
      if (password !== confirmPassword) {
        this.message.error('Mật khẩu xác nhận không trùng khớp!');
        return;
      }
      if (!agreeTerms) {
        this.message.warning('Vui lòng đồng ý với Điều khoản & Chính sách!');
        return;
      }

      this.isLoading = true;
      this.authService.register(username, email, password).subscribe({
        next: () => {
          this.isLoading = false;
          this.message.success('Đăng ký tài khoản thành công! Đang tự động đăng nhập...');
          
          this.authService.login(username, password).subscribe({
            next: () => {
              this.router.navigate([this.returnUrl]);
            },
            error: () => {
              this.isSignUpMode = false;
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
  }

  onSocialLogin(provider: string): void {
    this.message.info(`Đang kết nối xác thực ${provider} SSO...`);
  }
}
