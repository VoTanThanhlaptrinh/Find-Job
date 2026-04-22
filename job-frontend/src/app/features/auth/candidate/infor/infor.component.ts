import { CommonModule } from '@angular/common';
import { Component, inject, computed } from '@angular/core';
import { RouterModule } from '@angular/router';
import { I18nService } from '../../../../core/i18n/i18n.service';

interface InforMenuItem {
  nameKey: string;
  icon: string;
  route: string;
  exact?: boolean;
}

@Component({
  selector: 'app-infor',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './infor.component.html',
  styleUrl: './infor.component.css'
})
export class InforComponent {
  private readonly i18n = inject(I18nService);
  
  isOpen = true;
  readonly userAvatar = 'assets/images/avatar.jpg';
  readonly userName = 'Candidate Name';

  readonly menuItems: InforMenuItem[] = [
    { nameKey: 'profile.title', icon: 'person', route: '/infor/profile', exact: true },
    { nameKey: 'changePassword.title', icon: 'lock', route: '/infor/change-password' },
    { nameKey: 'cvList.title', icon: 'description', route: '/infor/cv' },
    { nameKey: 'recommendedJobs.title', icon: 'work', route: '/infor/recommended-jobs' },
    { nameKey: 'candidateList.interviewing', icon: 'history', route: '/infor/history-apply' }
  ];

  readonly translatedMenuItems = computed(() => 
    this.menuItems.map(item => ({
      ...item,
      name: this.i18n.translate(item.nameKey)
    }))
  );

  readonly logoutLabel = computed(() => this.i18n.translate('header.logout'));

  toggleSidebar(): void {
    this.isOpen = !this.isOpen;
  }

  onMenuItemClick(): void {
    // Mobile optimization: close sidebar on click if it's an overlay (implementation depends on design)
  }
}
