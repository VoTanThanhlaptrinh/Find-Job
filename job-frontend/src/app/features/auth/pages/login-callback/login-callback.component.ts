import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { TokenService } from '../../../../core/services/token.service';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-login-callback',
  imports: [TranslatePipe, RouterLink],
  standalone: true,
  templateUrl: './login-callback.component.html',
  styleUrls: ['./login-callback.component.css'],
})
export class LoginCallbackComponent implements OnInit {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private tokenService: TokenService,
    private authService: AuthService,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe((params) => {
      const token = params['token'] || params['accessToken'] || this.route.snapshot.fragment;
      
      if (!token) {
        if (isPlatformBrowser(this.platformId)) {
          setTimeout(() => this.router.navigate(['/login']), 100);
        }
        return;
      }

      // Handle the case where the token is inside a fragment string e.g. token=abc&...
      let parsedToken = token;
      if (typeof token === 'string' && token.includes('token=')) {
        const urlParams = new URLSearchParams(token);
        parsedToken = urlParams.get('token') || urlParams.get('accessToken') || token;
      }

      this.tokenService.setToken(parsedToken);
      this.authService.setLoggedIn(true);

      if (isPlatformBrowser(this.platformId)) {
        setTimeout(() => this.router.navigate(['/']), 100);
      }
    });
  }
}
