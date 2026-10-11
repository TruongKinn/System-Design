import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

export interface ExportJob {
  id: string;
  title: string;
  exportType: 'CSV' | 'EXCEL' | 'PARQUET' | 'JSON';
  totalRecords: number;
  processedRecords: number;
  progressPercentage: number;
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
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
  private apiUrl = 'http://localhost:8080/api/exports';

  constructor(private http: HttpClient) {}

  getExportJobs(): Observable<ExportJob[]> {
    return this.http.get<any[]>(this.apiUrl).pipe(
      map(list => {
        if (!Array.isArray(list)) return [];
        return list.map(item => ({
          id: item.jobId || String(item.id),
          title: item.fileName || `Tác vụ xuất ${item.jobId}`,
          exportType: (item.fileName && item.fileName.toLowerCase().endsWith('.csv') ? 'CSV' : 'EXCEL') as any,
          totalRecords: item.totalRecords || 0,
          processedRecords: item.processedRecords || 0,
          progressPercentage: item.totalRecords > 0 ? Math.min(100, Math.round((item.processedRecords / item.totalRecords) * 100)) : 0,
          status: item.status || 'PENDING',
          createdAt: item.createdAt ? String(item.createdAt).replace('T', ' ').substring(0, 19) : '',
          completedAt: item.completedAt ? String(item.completedAt).replace('T', ' ').substring(0, 19) : undefined,
          downloadUrl: item.downloadUrl,
          requestedBy: item.createdBy || 'admin'
        }));
      })
    );
  }

  createExportJob(request: { title: string; exportType: string; requestedRecords?: number }): Observable<ExportJob> {
    const payload = {
      entityName: request.title || 'CUSTOMER',
      requestedRecords: request.requestedRecords || 10000,
      fileFormat: request.exportType === 'CSV' ? 'csv' : 'xlsx'
    };

    return this.http.post<any>(this.apiUrl, payload).pipe(
      map(item => ({
        id: item.jobId || String(item.id),
        title: item.fileName || request.title,
        exportType: request.exportType as any,
        totalRecords: item.totalRecords || payload.requestedRecords,
        processedRecords: item.processedRecords || 0,
        progressPercentage: 0,
        status: item.status || 'PROCESSING',
        createdAt: item.createdAt ? String(item.createdAt).replace('T', ' ').substring(0, 19) : new Date().toISOString().replace('T', ' ').substring(0, 19),
        downloadUrl: item.downloadUrl,
        requestedBy: item.createdBy || 'admin'
      }))
    );
  }

  getJobProgress(jobId: string): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${jobId}/status`);
  }

  cancelExport(id: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${id}/cancel`, {});
  }
}
