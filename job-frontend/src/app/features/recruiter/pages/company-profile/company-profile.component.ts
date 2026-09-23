import { CommonModule } from '@angular/common';
import { Component, OnInit, effect } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
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
  selector: 'app-company-profile',
  standalone: true,
  imports: [CommonModule, TranslatePipe, AddAddressModalComponent, SkeletonAddressCardComponent, ReactiveFormsModule],
  templateUrl: './company-profile.component.html',
  styleUrl: './company-profile.component.css',
})
export class CompanyProfileComponent implements OnInit {
  activeTab: 'info' | 'addresses' = 'info';
  companyInfoForm: FormGroup;
  isLoading = false;
  addresses: CompanyAddress[] = [];
  openMenuId: number | null = null;
  isAddModalOpen = false;
  isEditMode = false;
  editingAddressId: number | null = null;
  currentEditingAddress: AddressFormData | null = null;

  constructor(
    private readonly addressService: RecruiterAddressService,
    private readonly notifyService: NotifyMessageService,
    private readonly fb: FormBuilder
  ) {
    this.companyInfoForm = this.fb.group({
      companyName: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      size: [''],
      industry: [''],
      taxCode: ['', Validators.required],
      website: ['', Validators.pattern(/^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([/\w .-]*)*\/?$/)],
      phone: ['', [Validators.required, Validators.pattern(/^[0-9+\-\s]+$/)]],
      logo: ['']
    });
    effect(() => {
      this.addresses = this.addressService.addresses$();
      this.isLoading = this.addressService.isLoading$();
    });
  }

  ngOnInit(): void {
    this.addressService.loadAddresses();
  }

  setTab(tab: 'info' | 'addresses'): void {
    this.activeTab = tab;
  }

  onSaveCompanyInfo(): void {
    if (this.companyInfoForm.valid) {
      console.log('Company Info Submitted (Mock):', this.companyInfoForm.value);
      this.notifyService.success('Cập nhật thông tin công ty thành công (MOCK)');
    } else {
      this.companyInfoForm.markAllAsTouched();
    }
  }

  loadAddresses(): void {
    this.addressService.loadAddresses();
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
