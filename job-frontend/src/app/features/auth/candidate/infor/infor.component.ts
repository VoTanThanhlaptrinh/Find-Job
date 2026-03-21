import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

interface InforMenuItem {
  name: string;
  icon: string;
  route: string;
  exact?: boolean;
}

@Component({
  selector: 'app-infor',
  imports: [CommonModule, RouterModule],
  templateUrl: './infor.component.html',
  styleUrl: './infor.component.css'
})
export class InforComponent {
  isOpen = false;
  isDarkMode = false;

  readonly userAvatar = 'assets/images/avatar.jpg';
  readonly userName = 'Tên';

  readonly menuItems: InforMenuItem[] = [
    { name: 'Hồ sơ', icon: 'fas fa-user-circle', route: '/infor/profile', exact: true },
    { name: 'Đổi mật khẩu', icon: 'fas fa-key', route: '/infor/change-password' },
    { name: 'CV của bạn', icon: 'fas fa-file-alt', route: '/infor/cv' },
    { name: 'Danh sách các job đã ứng tuyển', icon: 'fas fa-briefcase', route: '/infor/history-apply' }
  ];

  toggleSidebar(): void {
    this.isOpen = !this.isOpen;
  }
}
