import {
  ChangeDetectorRef,
  Component,
  CUSTOM_ELEMENTS_SCHEMA,
  HostListener,
  NgZone,
  OnInit,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AccountService } from '../../services/account.service';

@Component({
  selector: 'app-header',
  imports: [
    CommonModule,
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  standalone: true,
})
export class HeaderComponent implements OnInit {
  isDropdownOpen = false;
  constructor(
    private auth: AuthService,
    private accountService: AccountService,
    private router: Router,
    private zone: NgZone,
    private cd: ChangeDetectorRef
  ) {

  }
  ngOnInit(): void {
    this.isLogin();
  }

  logout(): void {
    this.accountService.logout();
  }

  toggleDropdown() {
    this.isDropdownOpen = !this.isDropdownOpen;
  }
  goInfor(): void {
    window.location.href = '/infor';
  }
  goVerify(): void {
    window.location.href = '/verify';
  }
  isScrolled: boolean = false;

  // Lắng nghe sự kiện scroll trên toàn window
  @HostListener('window:scroll', [])
  onWindowScroll() {
    // Nếu cuộn xuống quá 50px thì đổi thành true, ngược lại là false
    this.isScrolled = window.scrollY > 50;
  }
  isPageLogin(): boolean {
    return this.auth.isLoginClicked();
  }
  isPageRegister(): boolean {
    return this.auth.isRegisterClicked();
  }
  isLogin(): boolean {
    return this.auth.isLogin();
  }
}
