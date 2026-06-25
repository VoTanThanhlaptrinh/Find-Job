import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { AuthService } from '../../../core/services/auth.service';

interface InforMenuItem {
  nameKey: string;
  icon: string;
  route: string;
  exact?: boolean;
}

@Component({
  selector: 'app-infor',
  standalone: true,
  imports: [CommonModule, RouterModule, TranslatePipe],
  templateUrl: './infor.component.html',
  styleUrl: './infor.component.css'
})
export class InforComponent {
  private router = inject(Router);
  isOpen = true;
  readonly userAvatar = 'assets/images/avatar.jpg';
  readonly userName = 'Candidate Name';

  readonly menuItems: InforMenuItem[] = [
    { nameKey: 'profile.title', icon: 'person', route: '/infor/profile', exact: true },
    { nameKey: 'changePassword.title', icon: 'lock', route: '/infor/change-password' },
    { nameKey: 'cvList.title', icon: 'description', route: '/infor/cv' },
    { nameKey: 'recommendedJobs.title', icon: 'work', route: '/infor/recommended-jobs' },
    { nameKey: 'candidateList.applied', icon: 'history', route: '/infor/history-apply' }
  ];
  constructor(private auth: AuthService) {

  }

  toggleSidebar(): void {
    this.isOpen = !this.isOpen;
  }

  onMenuItemClick(): void {
    // Mobile optimization: close sidebar on click if it's an overlay (implementation depends on design)
  }

  isActive(route: string, exact: boolean = false): boolean {
    return this.router.isActive(route, {
      paths: exact ? 'exact' : 'subset',
      queryParams: 'ignored',
      fragment: 'ignored',
      matrixParams: 'ignored'
    });
  }
  onLogout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
