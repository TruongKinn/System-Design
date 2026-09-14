import { Component, OnInit, OnDestroy } from '@angular/core';
import { ImportService, ImportBatch } from '../../core/services/import.service';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzUploadFile } from 'ng-zorro-antd/upload';

@Component({
  selector: 'app-imports',
  templateUrl: './imports.component.html',
  styleUrls: ['./imports.component.css']
})
export class ImportsComponent implements OnInit, OnDestroy {
  importBatches: ImportBatch[] = [];
  loading = false;
  uploading = false;
  timerInterval: any;
  activePollingCountdown = 0;

  constructor(
    private importService: ImportService,
    private message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.loadBatches();

    // Auto-poll active import batches every 2 seconds
    this.timerInterval = setInterval(() => {
      const hasActive = this.activePollingCountdown > 0 || this.importBatches.some(b => 
        b.status === 'PROCESSING' || 
        b.status === 'UPLOADING' || 
        b.status === 'PARSING' || 
        b.status === 'UPLOADED'
      );

      if (hasActive) {
        if (this.activePollingCountdown > 0) {
          this.activePollingCountdown--;
        }
        this.importService.getImportBatches().subscribe(data => {
          this.importBatches = data;
        });
      }
    }, 2000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) clearInterval(this.timerInterval);
  }

  loadBatches(): void {
    this.loading = true;
    this.importService.getImportBatches().subscribe({
      next: (data) => {
        this.importBatches = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  beforeUpload = (file: NzUploadFile): boolean => {
    this.uploading = true;
    this.message.loading(`Đang nạp file [${file.name}] vào hệ thống...`);

    const rawFile = (file as any) instanceof File ? (file as any) : ((file as any).originFileObj || new File([file as any], file.name));
    this.importService.uploadFile(rawFile).subscribe({
      next: (batch) => {
        this.message.success(`Đã nhận file [${file.name}]. Hệ thống đang xử lý streaming batch import!`);
        this.uploading = false;
        this.activePollingCountdown = 15; // Actively poll every 2s for at least 30s
        this.loadBatches();
      },
      error: (err) => {
        this.message.error(`Lỗi khi nạp file: ${err.message || 'Thất bại'}`);
        this.uploading = false;
      }
    });

    return false; // Prevent default upload by ng-zorro
  };

  downloadSampleTemplate(): void {
    this.importService.downloadTemplate().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'sample_customers_1000.csv';
        a.click();
        window.URL.revokeObjectURL(url);
        this.message.success('Đã tải xuống file CSV mẫu (1,000 dòng) thành công!');
      },
      error: () => {
        this.message.error('Không thể tải file CSV mẫu!');
      }
    });
  }

  downloadSampleExcelTemplate(): void {
    this.importService.downloadExcelTemplate().subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'sample_customers_1000.xlsx';
        a.click();
        window.URL.revokeObjectURL(url);
        this.message.success('Đã tải xuống file Excel mẫu (1,000 dòng) thành công!');
      },
      error: () => {
        this.message.error('Không thể tải file Excel mẫu!');
      }
    });
  }

  getStatusBadge(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'PROCESSING': return 'processing';
      case 'UPLOADED': return 'processing';
      case 'UPLOADING': return 'processing';
      case 'PARSING': return 'warning';
      case 'FAILED': return 'error';
      default: return 'default';
    }
  }
}
