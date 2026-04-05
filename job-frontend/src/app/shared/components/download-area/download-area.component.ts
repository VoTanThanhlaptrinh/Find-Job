import { Component, Input } from '@angular/core';
import { TranslatePipe } from '../../pipes/translate.pipe';

@Component({
  selector: 'app-download-area',
  standalone: true,
  imports: [TranslatePipe],
  templateUrl: './download-area.component.html',
  styleUrl: './download-area.component.css',
})
export class DownloadAreaComponent {
  @Input() sectionId = 'app';
  @Input() imageSrc = 'assets/web_css/img/d1.png';
  @Input() imageAlt = 'Job Listing mobile app';
  @Input() title?: string;
  @Input() subtitle?: string;
  @Input() description?: string;
  @Input() appStoreHref = '#';
  @Input() playStoreHref = '#';
}
