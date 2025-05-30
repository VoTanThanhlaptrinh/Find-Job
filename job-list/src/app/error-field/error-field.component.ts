import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { AbstractControl, FormControl, FormGroupDirective } from '@angular/forms';
import { ValidationPipe } from '../validation.pipe';

@Component({
  selector: 'app-error-field',
  imports: [CommonModule, ValidationPipe],
  templateUrl: './error-field.component.html',
  styleUrl: './error-field.component.css',
  standalone: true,
})
export class ErrorFieldComponent {
  @Input() control!: FormControl | AbstractControl;
  @Input() errorMessages!: Object;

  constructor(public formDirective: FormGroupDirective) {}
  
}
