import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-recruiter-dashboard',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './recruiter-dashboard.component.html',
  styleUrl: './recruiter-dashboard.component.css',
})
export class RecruiterDashboardComponent {
  private readonly router = inject(Router);

  onLogout(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    this.router.navigate(['/recruiter/login']);
  }
}
