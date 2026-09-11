import { Component, OnInit } from '@angular/core';
import { ImportService, ImportBatch } from '../../core/services/import.service';
import { NzMessageService } from 'ng-zorro-antd/message';
import { NzUploadFile } from 'ng-zorro-antd/upload';

@Component({
  selector: 'app-imports',
  templateUrl: './imports.component.html',
  styleUrls: ['./imports.component.css']
})
export class ImportsComponent implements OnInit {
  importBatches: ImportBatch[] = [];
  loading = false;
  uploading = false;

  constructor(
    private importService: ImportService,
    private message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.loadBatches();
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
    this.message.loading(`Đang tải lên file [${file.name}] vào hệ thống batch ingestion...`);
    
    // Simulate upload & backend processing
    setTimeout(() => {
      const mockFile = new File([file as any], file.name);
      this.importService.uploadFile(mockFile).subscribe(batch => {
        this.message.success(`Đã nhận file [${file.name}]. Hệ thống đang xử lý batch import!`);
        this.uploading = false;
        this.loadBatches();
      });
    }, 1500);

    return false; // Prevent automatic upload by ng-zorro
  };

  getStatusBadge(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'success';
      case 'PROCESSING': return 'processing';
      case 'PARSING': return 'warning';
      case 'FAILED': return 'error';
      default: return 'default';
    }
  }
}
