import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface ImportBatch {
  id: string;
  filename: string;
  fileSizeMb: number;
  totalRows: number;
  successRows: number;
  errorRows: number;
  status: 'UPLOADING' | 'PARSING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  progressPercentage: number;
  uploadedAt: string;
  completedAt?: string;
  errorMessage?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ImportService {
  private apiUrl = '/api/v1/imports';

  private mockImports: ImportBatch[] = [
    {
      id: 'imp-701',
      filename: 'customers_master_2026.xlsx',
      fileSizeMb: 45.2,
      totalRows: 120000,
      successRows: 119850,
      errorRows: 150,
      status: 'COMPLETED',
      progressPercentage: 100,
      uploadedAt: '2026-09-11 14:00:00',
      completedAt: '2026-09-11 14:03:12'
    },
    {
      id: 'imp-702',
      filename: 'product_catalog_v2.csv',
      fileSizeMb: 12.8,
      totalRows: 35000,
      successRows: 21000,
      errorRows: 0,
      status: 'PROCESSING',
      progressPercentage: 60,
      uploadedAt: '2026-09-11 20:10:00'
    }
  ];

  constructor(private http: HttpClient) {}

  getImportBatches(): Observable<ImportBatch[]> {
    return this.http.get<ImportBatch[]>(this.apiUrl).pipe(
      catchError(() => of(this.mockImports))
    );
  }

  uploadFile(file: File): Observable<ImportBatch> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<ImportBatch>(`${this.apiUrl}/upload`, formData).pipe(
      catchError(() => {
        const newBatch: ImportBatch = {
          id: `imp-${Math.floor(100 + Math.random() * 900)}`,
          filename: file.name,
          fileSizeMb: parseFloat((file.size / (1024 * 1024)).toFixed(2)) || 1.5,
          totalRows: 50000,
          successRows: 0,
          errorRows: 0,
          status: 'PROCESSING',
          progressPercentage: 15,
          uploadedAt: new Date().toISOString().replace('T', ' ').substring(0, 19)
        };
        this.mockImports.unshift(newBatch);
        return of(newBatch);
      })
    );
  }
}
