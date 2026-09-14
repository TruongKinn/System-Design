import { Component, OnInit, OnDestroy } from '@angular/core';
import { ExportService, ExportJob } from '../../core/services/export.service';
import { NzMessageService } from 'ng-zorro-antd/message';

@Component({
  selector: 'app-exports',
  templateUrl: './exports.component.html',
  styleUrls: ['./exports.component.css']
})
export class ExportsComponent implements OnInit, OnDestroy {
  exportJobs: ExportJob[] = [];
  loading = false;
  timerInterval: any;
  activePollingCountdown = 0;

  // New Export Modal state
  isModalVisible = false;
  exportTitle = '';
  exportType: 'CSV' | 'EXCEL' | 'PARQUET' | 'JSON' = 'CSV';
  recordCountEstimate = 1000000;

  constructor(
    private exportService: ExportService,
    private message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.loadExports();

    // Polling active exports every 2 seconds
    this.timerInterval = setInterval(() => {
      const hasActive = this.activePollingCountdown > 0 || this.exportJobs.some(j => j.status === 'PROCESSING' || j.status === 'PENDING');
      if (hasActive) {
        if (this.activePollingCountdown > 0) {
          this.activePollingCountdown--;
        }
        this.exportService.getExportJobs().subscribe(data => {
          this.exportJobs = data;
        });
      }
    }, 2000);
  }

  ngOnDestroy(): void {
    if (this.timerInterval) clearInterval(this.timerInterval);
  }

  loadExports(): void {
    this.loading = true;
    this.exportService.getExportJobs().subscribe({
      next: (data) => {
        this.exportJobs = data;
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
    if (!this.exportTitle.trim()) {
      this.message.error('Vui lòng nhập tên báo cáo export!');
      return;
    }
    this.exportService.createExportJob({
      title: this.exportTitle,
      exportType: this.exportType,
      requestedRecords: this.recordCountEstimate || 10000
    }).subscribe(job => {
      this.message.success('Đã gửi yêu cầu xuất dữ liệu vào hàng chờ Async!');
      this.isModalVisible = false;
      this.exportTitle = '';
      this.activePollingCountdown = 15;
      this.loadExports();
    });
  }

  cancelExport(id: string): void {
    this.exportService.cancelExport(id).subscribe(() => {
      this.message.warning('Đã hủy tác vụ xuất dữ liệu');
      this.loadExports();
    });
  }

  downloadFile(url?: string): void {
    if (!url) {
      this.message.error('File chưa sẵn sàng hoặc đã bị hết hạn!');
      return;
    }
    this.message.info('Đang tải file từ MinIO Storage...');
    window.open(url, '_blank');
  }

  getTypeTagColor(type: string): string {
    switch (type) {
      case 'CSV': return 'blue';
      case 'EXCEL': return 'green';
      case 'PARQUET': return 'purple';
      case 'JSON': return 'orange';
      default: return 'default';
    }
  }
}
