import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export interface Job {
  id: string;
  name: string;
  groupName: string;
  cronExpression: string;
  description: string;
  status: 'PAUSED' | 'RUNNING' | 'SCHEDULED' | 'FAILED';
  lastRunTime?: string;
  nextRunTime?: string;
  executionCount: number;
  successCount: number;
  failureCount: number;
}

export interface JobExecution {
  id: string;
  jobId: string;
  jobName: string;
  status: 'SUCCESS' | 'FAILED' | 'RUNNING';
  startTime: string;
  endTime?: string;
  durationMs?: number;
  errorMessage?: string;
  processedRecords: number;
}

@Injectable({
  providedIn: 'root'
})
export class JobService {
  private apiUrl = '/api/v1/jobs';

  private mockJobs: Job[] = [
    {
      id: 'job-001',
      name: 'daily-analytics-aggregation',
      groupName: 'ANALYTICS',
      cronExpression: '0 0 1 * * ?',
      description: 'Tổng hợp số liệu báo cáo hàng ngày vào 1:00 AM',
      status: 'SCHEDULED',
      lastRunTime: '2026-09-11 01:00:00',
      nextRunTime: '2026-09-12 01:00:00',
      executionCount: 142,
      successCount: 140,
      failureCount: 2
    },
    {
      id: 'job-002',
      name: 'sync-user-profiles-kafka',
      groupName: 'SYNC',
      cronExpression: '*/30 * * * * ?',
      description: 'Đồng bộ thông tin người dùng qua Kafka topic user-events mỗi 30s',
      status: 'RUNNING',
      lastRunTime: '2026-09-11 20:20:30',
      nextRunTime: '2026-09-11 20:21:00',
      executionCount: 8920,
      successCount: 8918,
      failureCount: 2
    },
    {
      id: 'job-003',
      name: 'cleanup-expired-export-files',
      groupName: 'MAINTENANCE',
      cronExpression: '0 0 3 * * ?',
      description: 'Xóa các file export tạm trong MinIO lưu quá 7 ngày',
      status: 'PAUSED',
      lastRunTime: '2026-09-10 03:00:00',
      nextRunTime: '2026-09-12 03:00:00',
      executionCount: 45,
      successCount: 45,
      failureCount: 0
    },
    {
      id: 'job-004',
      name: 'batch-reconcile-transactions',
      groupName: 'FINANCE',
      cronExpression: '0 30 23 * * ?',
      description: 'Đối soát giao dịch hệ thống tài chính cuối ngày',
      status: 'FAILED',
      lastRunTime: '2026-09-10 23:30:00',
      nextRunTime: '2026-09-11 23:30:00',
      executionCount: 68,
      successCount: 65,
      failureCount: 3
    }
  ];

  private mockExecutions: JobExecution[] = [
    {
      id: 'exec-1001',
      jobId: 'job-002',
      jobName: 'sync-user-profiles-kafka',
      status: 'SUCCESS',
      startTime: '2026-09-11 20:20:30',
      endTime: '2026-09-11 20:20:32',
      durationMs: 2340,
      processedRecords: 450
    },
    {
      id: 'exec-1000',
      jobId: 'job-004',
      jobName: 'batch-reconcile-transactions',
      status: 'FAILED',
      startTime: '2026-09-10 23:30:00',
      endTime: '2026-09-10 23:32:15',
      durationMs: 135000,
      errorMessage: 'Database connection timeout when accessing partition transaction_20260910',
      processedRecords: 12500
    },
    {
      id: 'exec-0999',
      jobId: 'job-001',
      jobName: 'daily-analytics-aggregation',
      status: 'SUCCESS',
      startTime: '2026-09-11 01:00:00',
      endTime: '2026-09-11 01:05:42',
      durationMs: 342000,
      processedRecords: 1250400
    }
  ];

  constructor(private http: HttpClient) {}

  getJobs(): Observable<Job[]> {
    return this.http.get<Job[]>(this.apiUrl).pipe(
      catchError(() => of(this.mockJobs))
    );
  }

  createJob(jobData: Partial<Job>): Observable<Job> {
    return this.http.post<Job>(this.apiUrl, jobData).pipe(
      catchError(() => {
        const newJob: Job = {
          id: `job-00${this.mockJobs.length + 1}`,
          name: jobData.name || 'new-job',
          groupName: jobData.groupName || 'DEFAULT',
          cronExpression: jobData.cronExpression || '0 */5 * * * ?',
          description: jobData.description || '',
          status: 'SCHEDULED',
          executionCount: 0,
          successCount: 0,
          failureCount: 0,
          nextRunTime: '2026-09-11 21:00:00'
        };
        this.mockJobs.unshift(newJob);
        return of(newJob);
      })
    );
  }

  triggerJob(id: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${id}/trigger`, {}).pipe(
      catchError(() => {
        const job = this.mockJobs.find(j => j.id === id);
        if (job) {
          job.lastRunTime = new Date().toISOString().replace('T', ' ').substring(0, 19);
          job.executionCount++;
          job.successCount++;
        }
        return of(true);
      })
    );
  }

  pauseJob(id: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${id}/pause`, {}).pipe(
      catchError(() => {
        const job = this.mockJobs.find(j => j.id === id);
        if (job) job.status = 'PAUSED';
        return of(true);
      })
    );
  }

  resumeJob(id: string): Observable<boolean> {
    return this.http.post<boolean>(`${this.apiUrl}/${id}/resume`, {}).pipe(
      catchError(() => {
        const job = this.mockJobs.find(j => j.id === id);
        if (job) job.status = 'SCHEDULED';
        return of(true);
      })
    );
  }

  getExecutions(jobId?: string): Observable<JobExecution[]> {
    const url = jobId ? `${this.apiUrl}/${jobId}/executions` : `${this.apiUrl}/executions`;
    return this.http.get<JobExecution[]>(url).pipe(
      catchError(() => {
        if (jobId) return of(this.mockExecutions.filter(e => e.jobId === jobId));
        return of(this.mockExecutions);
      })
    );
  }
}
