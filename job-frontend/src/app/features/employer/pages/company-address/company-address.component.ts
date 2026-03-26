import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

interface CompanyAddress {
  id: number;
  locationName: string;
  // contactName: string;
  // phone: string;
  fullAddress: string;
}

@Component({
  selector: 'app-company-address',
  imports: [CommonModule],
  templateUrl: './company-address.component.html',
  styleUrl: './company-address.component.css',
})
export class CompanyAddressComponent {
  addresses: CompanyAddress[] = [
    {
      id: 1,
      locationName: 'Tru so Ha Noi',
      // contactName: 'Nguyen Minh Anh',
      // phone: '0901 234 567',
      fullAddress: 'Tang 8, Toa nha Green Office, 12 Duy Tan, Cau Giay, Ha Noi'
    },
    {
      id: 2,
      locationName: 'Van phong Da Nang',
      // contactName: 'Tran Hoang Long',
      // phone: '0912 888 222',
      fullAddress: 'Lo A23, Khu cong nghe cao, Quan Ngu Hanh Son, Da Nang'
    },
    {
      id: 3,
      locationName: 'Chi nhanh TP.HCM',
      // contactName: 'Le Bao Nhi',
      // phone: '0987 111 333',
      fullAddress: '15 Nguyen Thi Minh Khai, Phuong Da Kao, Quan 1, TP.HCM'
    }
  ];

  openMenuId: number | null = null;

  toggleMenu(id: number): void {
    this.openMenuId = this.openMenuId === id ? null : id;
  }

  deleteAddress(address: CompanyAddress): void {
    const shouldDelete = window.confirm(`Ban co chac chan muon xoa dia chi "${address.locationName}"?`);
    if (!shouldDelete) {
      return;
    }

    this.addresses = this.addresses.filter((item) => item.id !== address.id);
    this.openMenuId = null;
  }

  closeMenu(): void {
    this.openMenuId = null;
  }

  trackByAddressId(_: number, address: CompanyAddress): number {
    return address.id;
  }
}
