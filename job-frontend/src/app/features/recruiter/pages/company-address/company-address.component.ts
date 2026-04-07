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
  addresses: CompanyAddress[] = [];

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
