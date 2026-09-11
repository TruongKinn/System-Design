import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface ExportJob {
  id: string;
  title: string;
  exportType: 'CSV' | 'EXCEL' | 'PARQUET' | 'JSON';
  totalRecords: number;
  processedRecords: number;
  progressPercentage: number;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  createdAt: string;
  completedAt?: string;
  fileSizeMb?: number;
  downloadUrl?: string;
  requestedBy: string;
}

@Injectable({
  providedIn: 'root'
})
export class ExportService {
  private apiUrl = '/api/v1/exports';

  private mockExports: ExportJob[] = [
    {
      id: 'exp-9081',
      title: 'Xuất toàn bộ lịch sử giao dịch Q3/2026 (1.5 triệu dòng)',
      exportType: 'CSV',
      totalRecords: 1500000,
      processedRecords: 1500000,
      progressPercentage: 100,
      status: 'COMPLETED',
      createdAt: '2026-09-11 18:30:00',
      completedAt: '2026-09-11 18:34:12',
      fileSizeMb: 142.5,
      downloadUrl: 'http://localhost:9000/exports/transactions_q3_2026.csv',
      requestedBy: 'admin'
    },
    {
      id: 'exp-9082',
      title: 'Báo cáo tổng hợp doanh thu theo vùng miền',
      exportType: 'EXCEL',
      totalRecords: 450000,
      processedRecords: 320000,
      progressPercentage: 71,
      status: 'PROCESSING',
      createdAt: '2026-09-11 20:15:00',
      requestedBy: 'operator'
    },
    {
      id: 'exp-9083',
      title: 'Dump log truy cập hệ thống tháng 8/2026',
      exportType: 'PARQUET',
      totalRecords: 5000000,
      processedRecords: 0,
      progressPercentage: 0,
      status: 'PENDING',
      createdAt: '2026-09-11 20:20:00',
      requestedBy: 'auditor'
    }
  ];

  constructor(private http: HttpClient) {}

  getExportJobs(): Observable<ExportJob[]> {
    return this.http.get<ExportJob[]>(this.apiUrl).pipe(
      catchError(() => of(this.mockExports))
    );
  }

  createExportJob(request: { title: string; exportType: string; filterCriteria?: any }): Observable<ExportJob> {
    return this.http.post<ExportJob>(this.apiUrl, request).pipe(
      catchError(() => {
        const newExport: ExportJob = {
          id: `exp-${Math.floor(1000 + Math.random() * 9000)}`,
          title: request.title,
          exportType: request.exportType as any,
          totalRecords: 1000000,
          processedRecords: 0,
          progressPercentage: 0,
          status: 'PROCESSING',
          createdAt: new Date().toISOString().replace('T', ' ').substring(0, 19),
          requestedBy: 'admin'
        };
        this.mockExports.unshift(newExport);
        return of(newExport);
      })
    );
  }

  cancelExport(id: string): Observable<boolean> {
    return this.http.delete<boolean>(`${this.apiUrl}/${id}`).pipe(
      catchError(() => {
        const item = this.mockExports.find(x => x.id === id);
        if (item) item.status = 'FAILED';
        return of(true);
      })
    );
  }
}
