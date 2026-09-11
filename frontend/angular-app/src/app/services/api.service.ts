import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {

  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) { }

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('access_token') || 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MSwicm9sZXMiOlsiUk9MRV9BRE1JTiJdLCJwZXJtaXNzaW9ucyI6WyJKT0JfUkVBRCIsIkpPQl9FWEVDVVRFIiwiSk9CX01BTkFHRSIsIkVYUE9SVF9EQVRBIiwiSU1QT1JUX0RBVEEiXX0';
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
  }

  login(credentials: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/auth/login`, credentials);
  }

  getCurrentUser(): Observable<any> {
    return this.http.get(`${this.baseUrl}/auth/me`, { headers: this.getAuthHeaders() });
  }

  getJobs(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/jobs`, { headers: this.getAuthHeaders() });
  }

  createJob(jobData: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/jobs`, jobData, { headers: this.getAuthHeaders() });
  }

  triggerJob(jobId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/jobs/${jobId}/execute`, {}, { headers: this.getAuthHeaders() });
  }

  pauseJob(jobId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/jobs/${jobId}/pause`, {}, { headers: this.getAuthHeaders() });
  }

  resumeJob(jobId: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/jobs/${jobId}/resume`, {}, { headers: this.getAuthHeaders() });
  }

  createExport(request: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/exports/create`, request, { headers: this.getAuthHeaders() });
  }

  getExportStatus(jobId: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/exports/${jobId}/status`, { headers: this.getAuthHeaders() });
  }

  startImport(fileName: string, totalRows: number): Observable<any> {
    return this.http.post(`${this.baseUrl}/imports/upload?fileName=${fileName}&totalRows=${totalRows}`, {}, { headers: this.getAuthHeaders() });
  }

  getImportStatus(jobId: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/imports/${jobId}/status`, { headers: this.getAuthHeaders() });
  }
}
