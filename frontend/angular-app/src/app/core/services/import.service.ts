import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

export interface ImportBatch {
  id: string;
  filename: string;
  fileSizeMb: number;
  totalRows: number;
  successRows: number;
  errorRows: number;
  status: 'UPLOADING' | 'PARSING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'UPLOADED' | string;
  progressPercentage: number;
  uploadedAt: string;
  completedAt?: string;
  errorMessage?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ImportService {
  private apiUrl = 'http://localhost:8084/api/imports';

  private mockImports: ImportBatch[] = [
    {
      id: 'imp-701',
      filename: 'customers_template.csv',
      fileSizeMb: 1.2,
      totalRows: 5000,
      successRows: 5000,
      errorRows: 0,
      status: 'COMPLETED',
      progressPercentage: 100,
      uploadedAt: '2026-09-13 14:00:00',
      completedAt: '2026-09-13 14:00:03'
    }
  ];

  constructor(private http: HttpClient) {}

  getImportBatches(): Observable<ImportBatch[]> {
    return this.http.get<any[]>(this.apiUrl).pipe(
      map(list => {
        if (!Array.isArray(list)) return this.mockImports;
        return list.map(item => ({
          id: item.jobId || String(item.id),
          filename: item.fileName,
          fileSizeMb: item.fileSize ? parseFloat((item.fileSize / (1024 * 1024)).toFixed(2)) : 0.5,
          totalRows: item.totalRows || 0,
          successRows: item.importedRows || 0,
          errorRows: item.errorRows || 0,
          status: item.status as any,
          progressPercentage: item.status === 'COMPLETED'
            ? 100
            : (item.totalRows > 0 ? Math.min(99, Math.round(((item.importedRows + item.errorRows) / item.totalRows) * 100)) : (item.status === 'PROCESSING' ? 15 : 5)),
          uploadedAt: item.createdAt ? String(item.createdAt).replace('T', ' ').substring(0, 19) : '',
          completedAt: item.completedAt ? String(item.completedAt).replace('T', ' ').substring(0, 19) : undefined
        }));
      }),
      catchError(() => of(this.mockImports))
    );
  }

  uploadFile(file: File): Observable<ImportBatch> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<any>(`${this.apiUrl}/upload`, formData).pipe(
      map(item => ({
        id: item.jobId || String(item.id),
        filename: item.fileName || file.name,
        fileSizeMb: parseFloat((file.size / (1024 * 1024)).toFixed(2)) || 0.1,
        totalRows: item.totalRows || 0,
        successRows: item.importedRows || 0,
        errorRows: item.errorRows || 0,
        status: item.status || 'PROCESSING',
        progressPercentage: 10,
        uploadedAt: item.createdAt ? String(item.createdAt).replace('T', ' ').substring(0, 19) : new Date().toISOString().replace('T', ' ').substring(0, 19)
      })),
      catchError(() => {
        const fallback: ImportBatch = {
          id: `imp-${Math.floor(100 + Math.random() * 900)}`,
          filename: file.name,
          fileSizeMb: parseFloat((file.size / (1024 * 1024)).toFixed(2)) || 1.5,
          totalRows: 5000,
          successRows: 0,
          errorRows: 0,
          status: 'PROCESSING',
          progressPercentage: 15,
          uploadedAt: new Date().toISOString().replace('T', ' ').substring(0, 19)
        };
        this.mockImports.unshift(fallback);
        return of(fallback);
      })
    );
  }

  downloadTemplate(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/template`, { responseType: 'blob' });
  }

  downloadExcelTemplate(): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/template/excel`, { responseType: 'blob' });
  }
}
