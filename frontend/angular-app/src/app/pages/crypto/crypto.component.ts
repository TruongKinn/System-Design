import { Component, OnInit } from '@angular/core';
import { CryptoService, VaultStatus, EncryptResult, DecryptResult } from '../../core/services/crypto.service';
import { NzMessageService } from 'ng-zorro-antd/message';

@Component({
  selector: 'app-crypto',
  templateUrl: './crypto.component.html',
  styleUrls: ['./crypto.component.css']
})
export class CryptoComponent implements OnInit {

  // Trạng thái Vault
  vaultStatus: VaultStatus | null = null;
  isLoadingStatus = false;

  // Dữ liệu Mã hóa
  plainTextInput = 'DataFlow Enterprise Secret Data 2026! User: admin, Role: ROLE_ADMIN';
  isEncrypting = false;
  encryptResult: EncryptResult | null = null;

  // Dữ liệu Giải mã
  cipherTextInput = '';
  isDecrypting = false;
  decryptResult: DecryptResult | null = null;

  constructor(
    private cryptoService: CryptoService,
    private message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.loadVaultStatus();
    // Tự động mã hóa và giải mã chuỗi mẫu ban đầu để giao diện hiển thị sống động
    setTimeout(() => {
      this.autoDemoRun();
    }, 300);
  }


  loadVaultStatus(): void {
    this.isLoadingStatus = true;
    this.cryptoService.getCryptoStatus().subscribe({
      next: (status) => {
        this.vaultStatus = status;
        this.isLoadingStatus = false;
      },
      error: (err) => {
        console.error('Lỗi khi tải trạng thái Vault:', err);
        this.isLoadingStatus = false;
        this.message.error('Không thể kết nối đến dịch vụ mã hóa / Vault');
      }
    });
  }

  onEncrypt(autoPopulateDecrypt: boolean = false): void {
    if (!this.plainTextInput || !this.plainTextInput.trim()) {
      this.message.warning('Vui lòng nhập nội dung cần mã hóa!');
      return;
    }

    this.isEncrypting = true;
    this.cryptoService.encryptData(this.plainTextInput).subscribe({
      next: (res) => {
        this.encryptResult = res;
        this.isEncrypting = false;
        if (autoPopulateDecrypt) {
          this.cipherTextInput = res.cipherText;
          this.onDecrypt(true);
        } else {
          this.message.success('Mã hóa dữ liệu với AES-256-GCM thành công!');
        }
      },
      error: (err) => {
        this.isEncrypting = false;
        this.message.error('Mã hóa thất bại: ' + (err.error?.message || err.message));
      }
    });
  }

  autoDemoRun(): void {
    this.onEncrypt(true);
  }

  copyToClipboard(text: string, label: string = 'Nội dung'): void {
    if (!text) return;
    navigator.clipboard.writeText(text).then(() => {
      this.message.success(`Đã sao chép ${label} vào Clipboard!`);
    }).catch(() => {
      this.message.warning('Không thể truy cập Clipboard');
    });
  }

  sendToDecrypt(): void {
    if (this.encryptResult?.cipherText) {
      this.cipherTextInput = this.encryptResult.cipherText;
      this.decryptResult = null;
      this.message.info('Đã chuyển chuỗi mã hóa sang khung Giải mã!');
    }
  }

  onDecrypt(silent: boolean = false): void {
    if (!this.cipherTextInput || !this.cipherTextInput.trim()) {
      if (!silent) this.message.warning('Vui lòng nhập chuỗi mã hóa cần giải mã!');
      return;
    }

    this.isDecrypting = true;
    this.cryptoService.decryptData(this.cipherTextInput.trim()).subscribe({
      next: (res) => {
        this.decryptResult = res;
        this.isDecrypting = false;
        if (!silent) {
          if (res.success) {
            this.message.success('Giải mã và xác thực toàn vẹn thành công!');
          } else if (res.tampered) {
            this.message.error('CẢNH BÁO BẢO MẬT: Phát hiện dữ liệu bị giả mạo / Auth Tag mismatch!');
          } else {
            this.message.warning(res.message);
          }
        }
      },
      error: (err) => {
        this.isDecrypting = false;
        if (!silent) {
          this.message.error('Lỗi khi gọi API giải mã: ' + (err.error?.message || err.message));
        }
      }
    });
  }

  simulateTamper(): void {
    if (!this.cipherTextInput) {
      if (this.encryptResult?.cipherText) {
        this.cipherTextInput = this.encryptResult.cipherText;
      } else {
        this.message.warning('Vui lòng nhập hoặc mã hóa một chuỗi trước khi thử nghiệm giả mạo!');
        return;
      }
    }

    // Thay đổi 1 ký tự bất kỳ ở giữa chuỗi để mô phỏng kẻ tấn công sửa đổi byte trong database
    const text = this.cipherTextInput;
    if (text.length > 15) {
      const idx = text.length - 8;
      const originalChar = text.charAt(idx);
      const fakeChar = originalChar === 'A' ? 'B' : 'A';
      this.cipherTextInput = text.substring(0, idx) + fakeChar + text.substring(idx + 1);
      this.decryptResult = null;
      this.message.warning(`Đã cố tình giả mạo 1 ký tự tại vị trí ${idx} ('${originalChar}' -> '${fakeChar}'). Hãy bấm "Giải Mã Dữ Liệu" để kiểm tra tính toàn vẹn!`);
    }
  }

  fillSamplePassword(): void {
    this.plainTextInput = 'Admin@SecureMaster2026!';
  }

  fillSampleJson(): void {
    this.plainTextInput = JSON.stringify({
      apiKey: 'df_live_sec_994a82f01c9b4e77',
      databaseUrl: 'jdbc:postgresql://db.internal:5432/finance',
      timestamp: new Date().toISOString()
    }, null, 2);
  }

  getPayloadLength(): number {
    return this.encryptResult && this.encryptResult.cipherPayloadHex 
      ? Math.floor(this.encryptResult.cipherPayloadHex.length / 2) 
      : 0;
  }
}
