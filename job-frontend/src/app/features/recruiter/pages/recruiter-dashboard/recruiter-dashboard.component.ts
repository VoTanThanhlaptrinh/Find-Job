import { Component, inject } from '@angular/core';
import { NgClass } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-recruiter-dashboard',
  imports: [RouterOutlet, RouterLink, TranslatePipe],
  templateUrl: './recruiter-dashboard.component.html',
  styleUrl: './recruiter-dashboard.component.css',
})
export class RecruiterDashboardComponent {
  private readonly router = inject(Router);
  protected readonly openGroups: Record<string, boolean> = {
    jobs: true,
    company: true,
  };

  toggleGroup(group: string): void {
    this.openGroups[group] = !this.openGroups[group];
  }

  isGroupOpen(group: string): boolean {
    return !!this.openGroups[group];
  }

  isRouteGroupActive(prefix: string): boolean {
    return this.router.url.startsWith(prefix);
  }

  onLogout(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    this.router.navigate(['/recruiter/login']);
  }
}
