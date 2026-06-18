import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { AddAddressModalComponent, AddressFormData } from '../../components/add-address-modal/add-address-modal.component';

interface CompanyAddress {
  id: number;
  locationName: string;
  contactName: string;
  phone: string;
  fullAddress: string;
  isDefault: boolean;
}

@Component({
  selector: 'app-company-address',
  standalone: true,
  imports: [CommonModule, TranslatePipe, AddAddressModalComponent],
  templateUrl: './company-address.component.html',
  styleUrl: './company-address.component.css',
})
export class CompanyAddressComponent {
  addresses: CompanyAddress[] = [
    {
      id: 1,
      locationName: 'Tru so chinh TP.HCM',
      contactName: 'Nguyen Van An',
      phone: '0909 111 222',
      fullAddress: 'Toa nha Bitexco, 2 Hai Trieu, Quan 1, TP.HCM',
      isDefault: true
    },
    {
      id: 2,
      locationName: 'Van phong Ha Noi',
      contactName: 'Tran Thi Mai',
      phone: '0912 345 678',
      fullAddress: '54 Lieu Giai, Ba Dinh, Ha Noi',
      isDefault: false
    },
    {
      id: 3,
      locationName: 'Van phong Da Nang',
      contactName: 'Le Quoc Viet',
      phone: '0935 888 999',
      fullAddress: 'Duong 2/9, Hai Chau, Da Nang',
      isDefault: false
    }
  ];

  openMenuId: number | null = null;
  isAddModalOpen = false;

  toggleMenu(id: number): void {
    this.openMenuId = this.openMenuId === id ? null : id;
  }

  openAddModal(): void {
    this.isAddModalOpen = true;
    this.closeMenu();
  }

  closeAddModal(): void {
    this.isAddModalOpen = false;
  }

  onSaveAddress(data: AddressFormData): void {
    const nextId = this.addresses.length > 0
      ? Math.max(...this.addresses.map((item) => item.id)) + 1
      : 1;

    // Handle setting default logic
    if (data.isDefault) {
      this.addresses = this.addresses.map(addr => ({ ...addr, isDefault: false }));
    }

    // Usually contact name and phone would be fetched from user profile API. 
    // Here we use placeholders as per previous mock data style.
    const newAddress: CompanyAddress = {
      id: nextId,
      locationName: data.locationName,
      fullAddress: data.fullAddress,
      isDefault: data.isDefault,
      contactName: 'Đang cập nhật',
      phone: 'Đang cập nhật'
    };

    this.addresses = [newAddress, ...this.addresses];
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
