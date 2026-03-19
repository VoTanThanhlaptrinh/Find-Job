import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-call-to-action',
  standalone: true,
  imports: [],
  templateUrl: './call-to-action.component.html',
  styleUrl: './call-to-action.component.css',
})
export class CallToActionComponent {
  @Input() sectionId = 'join';
  @Input() withTopMargin = false;
  @Input() title = 'Tham gia vào chúng tôi mà không có bất kỳ do dự nào';
  @Input() description =
    'Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation.';
  @Input() candidateHref = '/login';
  @Input() employerHref = '/login';
  @Input() candidateText = 'Tôi là ứng viên';
  @Input() employerText = 'Tôi là nhà tuyển dụng';
}
