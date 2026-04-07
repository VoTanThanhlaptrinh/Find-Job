import {
  Component,
  CUSTOM_ELEMENTS_SCHEMA,
  HostListener,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import {MatMenuModule} from '@angular/material/menu';
import {MatButtonModule} from '@angular/material/button';
import { RouterLink } from "@angular/router";
import { I18nService } from '../../i18n/i18n.service';
import { AppLanguage } from '../../i18n/translations';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-header',
  imports: [CommonModule, MatButtonModule, MatMenuModule, RouterLink, TranslatePipe],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  standalone: true,
})
export class HeaderComponent {
  isMobileMenuOpen = false;
  isScrolled = false;

  constructor(
    private auth: AuthService,
    private i18nService: I18nService,
  ) {}

  logout(): void {
    this.auth.logout();
  }

  goInfor(): void {
    window.location.href = '/infor';
  }

  goVerify(): void {
    window.location.href = '/verify';
  }

  toggleMobileMenu(): void {
    this.isMobileMenuOpen = !this.isMobileMenuOpen;
  }

  closeMobileMenu(): void {
    this.isMobileMenuOpen = false;
  }

  @HostListener('window:scroll', [])
  onWindowScroll(): void {
    this.isScrolled = window.scrollY > 50;
  }

  get isLoginPage(): boolean {
    return this.auth.isLoginClicked();
  }

  get isRegisterPage(): boolean {
    return this.auth.isRegisterClicked();
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
}
