import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

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
  private apiUrl = 'http://localhost:8080/api/jobs';

  constructor(private http: HttpClient) {}

  getJobs(): Observable<Job[]> {
    return this.http.get<any[]>(this.apiUrl).pipe(
      map(list => {
        if (!Array.isArray(list)) return [];
        return list.map(item => ({
          id: String(item.id),
          name: item.name,
          groupName: item.targetService || 'SYSTEM',
          cronExpression: item.cronExpression,
          description: `Target: ${item.targetEndpoint} (${item.httpMethod})`,
          status: item.active ? 'SCHEDULED' : 'PAUSED',
          lastRunTime: item.updatedAt ? String(item.updatedAt).replace('T', ' ').substring(0, 19) : '',
          nextRunTime: '',
          executionCount: 0,
          successCount: 0,
          failureCount: 0
        }));
      })
    );
  }

  createJob(jobData: Partial<Job>): Observable<Job> {
    const payload = {
      name: jobData.name,
      cronExpression: jobData.cronExpression || '0 */15 * * * ?',
      targetService: jobData.groupName || 'general-service',
      targetEndpoint: '/api/task/execute',
      httpMethod: 'POST',
      active: true,
      maxRetries: 3
    };

    return this.http.post<any>(this.apiUrl, payload).pipe(
      map(item => ({
        id: String(item.id),
        name: item.name,
        groupName: item.targetService || 'SYSTEM',
        cronExpression: item.cronExpression,
        description: `Target: ${item.targetEndpoint}`,
        status: item.active ? 'SCHEDULED' : 'PAUSED',
        executionCount: 0,
        successCount: 0,
        failureCount: 0
      }))
    );
  }

  triggerJob(id: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${id}/execute`, {});
  }

  pauseJob(id: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${id}/pause`, {});
  }

  resumeJob(id: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/${id}/resume`, {});
  }

  deleteJob(id: string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`);
  }

  getExecutions(jobId?: string): Observable<JobExecution[]> {
    const url = jobId ? `${this.apiUrl}/${jobId}/executions` : `${this.apiUrl}/executions`;
    return this.http.get<any[]>(url).pipe(
      map(list => {
        if (!Array.isArray(list)) return [];
        return list.map(item => ({
          id: item.executionId || String(item.id),
          jobId: String(item.jobId),
          jobName: `Job #${item.jobId}`,
          status: item.status || 'SUCCESS',
          startTime: item.startTime ? String(item.startTime).replace('T', ' ').substring(0, 19) : '',
          endTime: item.endTime ? String(item.endTime).replace('T', ' ').substring(0, 19) : undefined,
          durationMs: item.endTime && item.startTime ? (new Date(item.endTime).getTime() - new Date(item.startTime).getTime()) : 1200,
          errorMessage: item.resultSummary && item.status === 'FAILED' ? item.resultSummary : undefined,
          processedRecords: 1000
        }));
      })
    );
  }
}
