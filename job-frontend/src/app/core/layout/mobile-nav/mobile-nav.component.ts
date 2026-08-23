import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { I18nService } from '../../i18n/i18n.service';
import { AppLanguage } from '../../i18n/translations';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { AuthModalComponent } from '../../../features/auth/components/auth-modal/auth-modal.component';

@Component({
  selector: 'app-mobile-nav',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, TranslatePipe, MatDialogModule],
  templateUrl: './mobile-nav.component.html',
  styleUrl: './mobile-nav.component.css',
})
export class MobileNavComponent {
  isMoreOpen = false;

  constructor(
    private auth: AuthService,
    private i18nService: I18nService,
    private router: Router,
    private dialog: MatDialog
  ) {}

  openAuthModal(tab: 'login' | 'register'): void {
    this.closeMore();
    this.dialog.open(AuthModalComponent, {
      width: 'auto',
      maxWidth: '95vw',
      panelClass: 'auth-modal-panel',
      data: { initialTab: tab }
    });
  }

  get isLoggedIn(): boolean {
    return this.auth.isLoggedIn();
  }

  get isAuthReady(): boolean {
    return this.auth.isAuthReady();
  }

  get currentLanguage(): AppLanguage {
    return this.i18nService.currentLanguage;
  }

  switchLanguage(language: AppLanguage): void {
    this.i18nService.setLanguage(language);
  }

  toggleMore(): void {
    this.isMoreOpen = !this.isMoreOpen;
  }

  closeMore(): void {
    this.isMoreOpen = false;
  }

  goAccount(): void {
    this.closeMore();
    if (this.isLoggedIn) {
      this.router.navigate(['/infor']);
    } else {
      this.openAuthModal('login');
    }
  }

  navigateToSection(sectionId: string): void {
    this.closeMore();
    const isHome = this.router.url === '/' || this.router.url.startsWith('/#') || this.router.url.startsWith('/?');
    if (isHome) {
      const el = document.getElementById(sectionId);
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    } else {
      this.router.navigate(['/'], { fragment: sectionId });
    }
  }

  logout(): void {
    this.closeMore();
    this.auth.logout('/login');
  }
}
