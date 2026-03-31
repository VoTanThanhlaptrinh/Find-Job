import { Component, signal, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';

export interface ApplySuccessData {
  jobTitle: string;
  companyName: string;
  applyDate: string;
}

@Component({
  selector: 'app-apply-success',
  imports: [],
  templateUrl: './apply-success.component.html',
  styleUrl: './apply-success.component.css',
})
export class ApplySuccessComponent implements OnInit {
  private router = inject(Router);

  data = signal<ApplySuccessData>({
    jobTitle: 'Senior Frontend Developer (Angular)',
    companyName: 'TechNova Solutions Vietnam',
    applyDate: this.formatDate(new Date())
  });

  ngOnInit() {
    // Optionally read from router history state if data is passed during navigation
    const state = history.state as { applyData?: ApplySuccessData };
    if (state?.applyData) {
      this.data.set(state.applyData);
    }
  }

  private formatDate(date: Date): string {
    return date.toLocaleDateString('vi-VN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  goBackToJobs() {
    this.router.navigate(['/category']);
  }

  goHome() {
    this.router.navigate(['/']);
  }
}
