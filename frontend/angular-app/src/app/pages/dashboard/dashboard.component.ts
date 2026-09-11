import { Component, OnInit, OnDestroy } from '@angular/core';

interface ServiceNode {
  name: string;
  port: number;
  status: 'online' | 'warning' | 'offline';
  cpu: number;
  memory: number;
  requests: number;
}

interface RecentActivity {
  time: string;
  event: string;
  type: 'success' | 'warning' | 'info';
}

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit, OnDestroy {

  private timer: any;

  services: ServiceNode[] = [
    { name: 'API Gateway', port: 8080, status: 'online', cpu: 12, memory: 45, requests: 1250 },
    { name: 'Auth Service', port: 8081, status: 'online', cpu: 8, memory: 38, requests: 320 },
    { name: 'Job Service', port: 8082, status: 'online', cpu: 25, memory: 62, requests: 87 },
    { name: 'Export Service', port: 8083, status: 'warning', cpu: 78, memory: 85, requests: 15 },
    { name: 'Import Service', port: 8084, status: 'online', cpu: 18, memory: 55, requests: 43 },
  ];

  stats = [
    { label: 'Tổng Jobs Đang Chạy', value: 12, suffix: 'active', icon: 'schedule', color: '#6366f1', trend: +3 },
    { label: 'Records Export Hôm Nay', value: 4200000, suffix: 'records', icon: 'export', color: '#06b6d4', trend: +12 },
    { label: 'Records Import Hôm Nay', value: 1850000, suffix: 'records', icon: 'import', color: '#10b981', trend: +8 },
    { label: 'Người Dùng Active', value: 47, suffix: 'users', icon: 'team', color: '#f59e0b', trend: +2 },
  ];

  activities: RecentActivity[] = [
    { time: '19:32:10', event: 'Job SYNC_CUSTOMER_DATA hoàn thành (1,250 records)', type: 'success' },
    { time: '19:28:45', event: 'Export job #245 tạo file 850MB trên MinIO', type: 'success' },
    { time: '19:25:30', event: 'Kafka consumer lag tăng lên 15,000 messages', type: 'warning' },
    { time: '19:20:00', event: 'Import 50,000 records từ customer_data.xlsx', type: 'info' },
    { time: '19:15:12', event: 'Redis Lock acquired cho Job CLEANUP_AUDIT_LOGS', type: 'info' },
    { time: '19:10:00', event: 'Job DAILY_REVENUE_REPORT chạy định kỳ 01:00', type: 'success' },
  ];

  kafkaTopics = [
    { name: 'data.export.request', partitions: 10, messages: 1250, lag: 45 },
    { name: 'data.export.result', partitions: 10, messages: 1205, lag: 0 },
    { name: 'data.import.request', partitions: 5, messages: 320, lag: 12 },
    { name: 'job.execution.event', partitions: 3, messages: 87, lag: 0 },
  ];

  ngOnInit(): void {
    this.startSimulation();
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }

  private startSimulation(): void {
    this.timer = setInterval(() => {
      this.services = this.services.map(s => ({
        ...s,
        cpu: Math.max(5, Math.min(95, s.cpu + (Math.random() * 10 - 5))),
        requests: Math.max(0, s.requests + Math.floor(Math.random() * 20 - 5))
      }));
    }, 2000);
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'online': return '#10b981';
      case 'warning': return '#f59e0b';
      case 'offline': return '#ef4444';
      default: return '#64748b';
    }
  }

  getCpuColor(cpu: number): string {
    if (cpu > 80) return '#ef4444';
    if (cpu > 60) return '#f59e0b';
    return '#10b981';
  }

  formatNumber(num: number): string {
    if (num >= 1000000) return (num / 1000000).toFixed(1) + 'M';
    if (num >= 1000) return (num / 1000).toFixed(1) + 'K';
    return num.toString();
  }
}
