import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, OnChanges, SimpleChanges } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';

export interface AddressFormData {
  locationName: string;
  city: string;
  street: string;
  isDefault: boolean;
}

@Component({
  selector: 'app-add-address-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, TranslatePipe],
  templateUrl: './add-address-modal.component.html',
  styleUrl: './add-address-modal.component.css'
})
export class AddAddressModalComponent implements OnChanges {
  @Input() isOpen = false;
  @Input() isEditMode = false;
  @Input() addressToEdit: AddressFormData | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() save = new EventEmitter<AddressFormData>();

  addressForm: FormGroup;

  constructor(private readonly fb: FormBuilder) {
    this.addressForm = this.fb.group({
      locationName: ['', [Validators.required]],
      city: ['', [Validators.required]],
      street: ['', [Validators.required]],
      isDefault: [false]
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen']?.currentValue && this.isEditMode && this.addressToEdit) {
      this.addressForm.patchValue({
        locationName: this.addressToEdit.locationName,
        city: this.addressToEdit.city,
        street: this.addressToEdit.street,
        isDefault: this.addressToEdit.isDefault
      });
    } else if (changes['isOpen']?.currentValue && !this.isEditMode) {
      this.addressForm.reset({ isDefault: false });
    }
  }

  onClose(): void {
    this.addressForm.reset({ isDefault: false });
    this.close.emit();
  }

  onSave(): void {
    if (this.addressForm.invalid) {
      this.addressForm.markAllAsTouched();
      return;
    }

    this.save.emit(this.addressForm.value);
    this.onClose();
  }
}
