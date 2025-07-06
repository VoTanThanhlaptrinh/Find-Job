import {ChangeDetectorRef, Component, CUSTOM_ELEMENTS_SCHEMA, NgZone, OnInit} from '@angular/core';
import { CommonModule} from '@angular/common';
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
import {concatMap, of} from 'rxjs';
import {map} from 'rxjs/operators';
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
  ],
  templateUrl: './header.component.html',
  styleUrl: './header.component.css',
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  standalone: true
})
export class HeaderComponent implements OnInit {
  isDropdownOpen = false;
  loggedIn: boolean = false;
  userLoggedIn: boolean = true;
  hirerLoggedIn: boolean = false;
  constructor(private auth: AuthService
              ,private router: Router
              ,private zone: NgZone,
  private cd: ChangeDetectorRef) { }


  ngOnInit(): void {
    this.checkLogin();
  }
  checkLogin(){
    this.auth.checkUserLogin().pipe(
      concatMap(userOk => {
        if (userOk) {
          return of({ role: 'USER', ok: userOk });
        }
        return this.auth.checkHirerLogin().pipe(
          map(hirerOk => ({ role: 'HIRER', ok: hirerOk }))
        );
      })
    ).subscribe({
      next: value => {
        this.loggedIn = value.ok;
        this.userLoggedIn = !(value.role === 'HIRER' && value.ok);
        this.hirerLoggedIn = value.role === 'HIRER' && value.ok;
      },
      error: () => {
        this.loggedIn = this.hirerLoggedIn = false;
        this.userLoggedIn = true;
      }
    });
  }
  logout(): void {
    this.auth.logout().subscribe({
      next: () => this.onLogoutComplete(),
      error: () => this.onLogoutComplete()
    });
  }

  private onLogoutComplete(): void {
    this.hirerLoggedIn = false;
    this.userLoggedIn = true;
    this.loggedIn = false;
    this.cd.detectChanges();
    this.router.navigate(['/login']);
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
