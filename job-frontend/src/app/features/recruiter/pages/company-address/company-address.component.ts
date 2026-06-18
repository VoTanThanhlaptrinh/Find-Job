import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { AddAddressModalComponent, AddressFormData } from '../../components/add-address-modal/add-address-modal.component';
import { RecruiterAddressService } from '../../services/recruiter-address.service';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';

export interface CompanyAddress {
  id: number;
  locationName: string;
  city: string;
  street: string;
  fullAddress: string;
  isDefault: boolean;
}

import { SkeletonAddressCardComponent } from '../../components/skeleton-address-card/skeleton-address-card.component';

@Component({
  selector: 'app-company-address',
  standalone: true,
  imports: [CommonModule, TranslatePipe, AddAddressModalComponent, SkeletonAddressCardComponent],
  templateUrl: './company-address.component.html',
  styleUrl: './company-address.component.css',
})
export class CompanyAddressComponent implements OnInit {
  isLoading = false;
  addresses: CompanyAddress[] = [];
  openMenuId: number | null = null;
  isAddModalOpen = false;
  isEditMode = false;
  editingAddressId: number | null = null;
  currentEditingAddress: AddressFormData | null = null;

  constructor(
    private readonly addressService: RecruiterAddressService,
    private readonly notifyService: NotifyMessageService
  ) {}

  ngOnInit(): void {
    this.loadAddresses();
  }

  loadAddresses(): void {
    this.isLoading = true;
    this.addressService.getAddresses().subscribe({
      next: (response) => {
        if (response.status === 200) {
          this.addresses = response.data;
        }
        this.isLoading = false;
      },
      error: (error) => {
        this.notifyService.error('Không thể tải danh sách địa chỉ');
        console.error(error);
        this.isLoading = false;
      }
    });
  }

  toggleMenu(id: number): void {
    this.openMenuId = this.openMenuId === id ? null : id;
  }

  openAddModal(): void {
    this.isEditMode = false;
    this.editingAddressId = null;
    this.currentEditingAddress = null;
    this.isAddModalOpen = true;
    this.closeMenu();
  }

  openEditModal(address: CompanyAddress): void {
    this.isEditMode = true;
    this.editingAddressId = address.id;
    this.currentEditingAddress = {
      locationName: address.locationName,
      city: address.city,
      street: address.street,
      isDefault: address.isDefault
    };
    this.isAddModalOpen = true;
    this.closeMenu();
  }

  closeAddModal(): void {
    this.isAddModalOpen = false;
  }

  onSaveAddress(data: AddressFormData): void {
    if (this.isEditMode && this.editingAddressId) {
      this.addressService.updateAddress(this.editingAddressId, data).subscribe({
        next: (response) => {
          if (response.status === 200) {
            this.notifyService.success('Cập nhật địa chỉ thành công');
            this.loadAddresses(); // Tải lại danh sách
          }
        },
        error: (error) => {
          this.notifyService.error('Cập nhật địa chỉ thất bại');
          console.error(error);
        }
      });
    } else {
      this.addressService.createAddress(data).subscribe({
        next: (response) => {
          if (response.status === 201 || response.status === 200) {
            this.notifyService.success('Thêm địa chỉ thành công');
            this.loadAddresses(); // Tải lại danh sách
          }
        },
        error: (error) => {
          this.notifyService.error('Thêm địa chỉ thất bại');
          console.error(error);
        }
      });
    }
  }

  deleteAddress(address: CompanyAddress): void {
    const shouldDelete = window.confirm(`Ban co chac chan muon xoa dia chi nay khong?`);
    if (!shouldDelete) {
      return;
    }

    this.addressService.deleteAddress(address.id).subscribe({
      next: (response) => {
        if (response.status === 200) {
          this.notifyService.success('Xóa địa chỉ thành công');
          this.loadAddresses(); // Tải lại danh sách
        }
      },
      error: (error) => {
        this.notifyService.error('Xóa địa chỉ thất bại');
        console.error(error);
      }
    });
    this.openMenuId = null;
  }

  closeMenu(): void {
    this.openMenuId = null;
  }

  trackByAddressId(_: number, address: CompanyAddress): number {
    return address.id;
  }
}
