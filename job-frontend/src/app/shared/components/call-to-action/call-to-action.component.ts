import { Component, Input } from '@angular/core';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-call-to-action',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './call-to-action.component.html',
  styleUrl: './call-to-action.component.css',
})
export class CallToActionComponent {
  @Input() sectionId = 'join';
  @Input() withTopMargin = false;
  @Input() title?: string;
  @Input() description?: string;
  @Input() candidateHref = '/login';
  @Input() employerHref = '/login';
  @Input() candidateText?: string;
  @Input() employerText?: string;
}
