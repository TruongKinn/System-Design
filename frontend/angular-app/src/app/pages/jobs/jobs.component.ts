import { Component, OnInit } from '@angular/core';
import { JobService, Job, JobExecution } from '../../core/services/job.service';
import { NzMessageService } from 'ng-zorro-antd/message';

@Component({
  selector: 'app-jobs',
  templateUrl: './jobs.component.html',
  styleUrls: ['./jobs.component.css']
})
export class JobsComponent implements OnInit {
  jobs: Job[] = [];
  executions: JobExecution[] = [];
  loading = false;
  
  // Modal state
  isCreateModalVisible = false;
  newJobName = '';
  newJobGroup = 'ANALYTICS';
  newJobCron = '0 */15 * * * ?';
  newJobDesc = '';

  // Execution Drawer/Modal state
  isExecutionDrawerVisible = false;
  selectedJobForHistory?: Job;

  constructor(
    private jobService: JobService,
    private message: NzMessageService
  ) {}

  ngOnInit(): void {
    this.loadJobs();
    this.loadExecutions();
  }

  loadJobs(): void {
    this.loading = true;
    this.jobService.getJobs().subscribe({
      next: (data) => {
        this.jobs = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  loadExecutions(): void {
    this.jobService.getExecutions().subscribe(data => {
      this.executions = data;
    });
  }

  triggerJob(job: Job): void {
    this.message.loading(`Đang khởi chạy tác vụ [${job.name}]...`);
    this.jobService.triggerJob(job.id).subscribe(() => {
      this.message.success(`Kích hoạt thành công tác vụ [${job.name}]!`);
      this.loadJobs();
      this.loadExecutions();
    });
  }

  togglePauseResume(job: Job): void {
    if (job.status === 'PAUSED') {
      this.jobService.resumeJob(job.id).subscribe(() => {
        this.message.success(`Đã kích hoạt lại Job [${job.name}]`);
        job.status = 'SCHEDULED';
      });
    } else {
      this.jobService.pauseJob(job.id).subscribe(() => {
        this.message.warning(`Đã tạm dừng Job [${job.name}]`);
        job.status = 'PAUSED';
      });
    }
  }

  openCreateModal(): void {
    this.isCreateModalVisible = true;
  }

  handleCreateOk(): void {
    if (!this.newJobName.trim()) {
      this.message.error('Vui lòng nhập tên Cron Job!');
      return;
    }
    this.jobService.createJob({
      name: this.newJobName,
      groupName: this.newJobGroup,
      cronExpression: this.newJobCron,
      description: this.newJobDesc
    }).subscribe(() => {
      this.message.success('Thêm mới Cron Job thành công!');
      this.isCreateModalVisible = false;
      this.resetModalForm();
      this.loadJobs();
    });
  }

  handleCreateCancel(): void {
    this.isCreateModalVisible = false;
    this.resetModalForm();
  }

  resetModalForm(): void {
    this.newJobName = '';
    this.newJobGroup = 'ANALYTICS';
    this.newJobCron = '0 */15 * * * ?';
    this.newJobDesc = '';
  }

  viewHistory(job: Job): void {
    this.selectedJobForHistory = job;
    this.isExecutionDrawerVisible = true;
  }

  getFilteredExecutions(): JobExecution[] {
    if (!this.selectedJobForHistory) return this.executions;
    return this.executions.filter(e => e.jobId === this.selectedJobForHistory?.id);
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'RUNNING': return 'processing';
      case 'SCHEDULED': return 'success';
      case 'PAUSED': return 'warning';
      case 'FAILED': return 'error';
      default: return 'default';
    }
  }

  getActiveCount(): number {
    return this.jobs.filter(j => j.status === 'SCHEDULED' || j.status === 'RUNNING').length;
  }

  getPausedCount(): number {
    return this.jobs.filter(j => j.status === 'PAUSED').length;
  }

  getFailedCount(): number {
    return this.jobs.filter(j => j.status === 'FAILED').length;
  }
}
