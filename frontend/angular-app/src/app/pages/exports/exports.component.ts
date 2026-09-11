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

    // Simulating progress polling every 3 seconds for active exports
    this.timerInterval = setInterval(() => {
      this.exportJobs.forEach(job => {
        if (job.status === 'PROCESSING') {
          job.progressPercentage = Math.min(100, job.progressPercentage + 15);
          job.processedRecords = Math.min(job.totalRecords, Math.floor(job.totalRecords * (job.progressPercentage / 100)));
          if (job.progressPercentage >= 100) {
            job.status = 'COMPLETED';
            job.fileSizeMb = 85.4;
            job.downloadUrl = `http://localhost:9000/exports/${job.id}_data.${job.exportType.toLowerCase()}`;
            this.message.success(`Tác vụ xuất dữ liệu [${job.title}] đã hoàn tất!`);
          }
        }
      });
    }, 3000);
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
      exportType: this.exportType
    }).subscribe(job => {
      this.message.success('Đã gửi yêu cầu xuất dữ liệu vào hàng chờ Async!');
      this.isModalVisible = false;
      this.exportTitle = '';
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
