import { Component } from '@angular/core';
import { TranslatePipe } from '../../../../shared/pipes/translate.pipe';
import { CallToActionComponent } from '../../../../shared/components/call-to-action/call-to-action.component';

@Component({
  selector: 'app-about-us',
  imports: [TranslatePipe, CallToActionComponent],
  standalone: true,
  templateUrl: './about-us.component.html',
  styleUrl: './about-us.component.css'
})
export class AboutUsComponent {

}
