import {ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA, NgZone, OnInit} from '@angular/core';
import {AsyncPipe, CommonModule} from '@angular/common';
import {Router, RouterLink} from '@angular/router';
import { AuthService } from '../services/auth.service';
import {
  CollapseDirective,
  ContainerComponent,
  DropdownComponent,
  DropdownItemDirective,
  DropdownMenuDirective,
  DropdownToggleDirective,
  NavbarComponent,
  NavbarNavComponent,
  NavbarTogglerDirective,
  NavItemComponent,
  NavLinkDirective
} from '@coreui/angular';
import {Observable} from 'rxjs';
@Component({
  selector: 'app-header',
  imports: [
    NavbarComponent,
    ContainerComponent,
    NavbarTogglerDirective,
    CollapseDirective,
    NavbarNavComponent,
    NavItemComponent,
    DropdownComponent,
    DropdownToggleDirective,
    NavLinkDirective,
    DropdownMenuDirective,
    DropdownItemDirective,
    CommonModule,
    RouterLink
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  standalone: true
})
export class HeaderComponent implements OnInit {
  isDropdownOpen = false;
  loggedIn: boolean | undefined  = undefined;

  constructor(private auth: AuthService
              ,private router: Router
              ,private zone: NgZone,
  private cd: ChangeDetectorRef) { }


  ngOnInit(): void {
    this.loggedIn = this.auth.checkLogin();
  }


  logout(): void {
    this.auth.logout().subscribe({
      next: res =>{
        this.router.navigate(['/']).then(window.location.reload)
        this.loggedIn = false;
      },
      error: err => {
        this.router.navigate(['/']).then(window.location.reload)
        this.loggedIn = false;
      }
    });
  }

  toggleDropdown() {
    this.isDropdownOpen = !this.isDropdownOpen;
  }
  goInfor(): void{
    window.location.href = '/infor';
  }
  goVerify(): void{
    window.location.href = '/verify';
  }
}
