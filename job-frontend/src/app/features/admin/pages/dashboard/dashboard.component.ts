import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminAuthService } from '../../services/admin-auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent {
  constructor(private readonly adminAuthService: AdminAuthService) {}

  get adminName(): string {
    return this.adminAuthService.adminProfile()?.fullName || 'Admin User';
  }

  get adminRole(): string {
    return this.adminAuthService.adminProfile()?.role || 'Super Administrator';
  }

  onLogout(): void {
    this.adminAuthService.logout();
  }
}
