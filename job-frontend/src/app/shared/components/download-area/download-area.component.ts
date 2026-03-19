import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-download-area',
  standalone: true,
  imports: [],
  templateUrl: './download-area.component.html',
  styleUrl: './download-area.component.css',
})
export class DownloadAreaComponent {
  @Input() sectionId = 'app';
  @Input() imageSrc = 'assets/web_css/img/d1.png';
  @Input() imageAlt = '';
  @Input() title = 'Tải app Job Listing';
  @Input() subtitle = 'ngay hôm nay!';
  @Input() description =
    'Lo lắng tìm việc không còn là vấn đề của bạn nữa. Chỉ cần bạn hợp tác với chúng tôi những công việc phù hợp với năng lực , kèm theo đãi ngộ và mức lương tốt đang đợi bạn. Hãy đến với Job Listing.';
  @Input() appStoreHref = '#';
  @Input() playStoreHref = '#';
}
