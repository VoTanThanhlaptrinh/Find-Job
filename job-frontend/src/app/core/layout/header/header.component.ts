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

@Component({
  selector: 'app-header',
  imports: [CommonModule, MatButtonModule, MatMenuModule, RouterLink],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  standalone: true,
})
export class HeaderComponent {
  isMobileMenuOpen = false;
  isScrolled = false;

  constructor(
    private auth: AuthService
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
}
