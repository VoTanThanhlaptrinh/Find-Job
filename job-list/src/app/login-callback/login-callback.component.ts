import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-login-callback',
  templateUrl: './login-callback.component.html',
  styleUrls: ['./login-callback.component.css'],
})
export class LoginCallbackComponent implements OnInit {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // Lấy token ngay khi component khởi tạo
    const token = this.route.snapshot.queryParamMap.get('token');
    if (token) {
      // Lưu JWT vào localStorage
      this.auth.fetchToken(token);

      // Chuyển hướng ở bên client
      if (isPlatformBrowser(this.platformId)) {
        // Chuyển về homepage hoặc trang trước login
        this.router.navigateByUrl('/').then(() => {
          // Reload trang để các service và interceptor mới hoạt động
          window.location.reload();
        });
      }
    } else {
      // Nếu không có token, chuyển về trang login
      this.router.navigate(['/login']);
    }
  }
}
