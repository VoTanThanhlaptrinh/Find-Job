import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminPageHeaderComponent } from '../../components/shared/page-header/page-header.component';
import { AdminMetricCardComponent } from '../../components/shared/metric-card/metric-card.component';

@Component({
  selector: 'app-jobs-management',
  standalone: true,
  imports: [CommonModule, AdminPageHeaderComponent, AdminMetricCardComponent],
  templateUrl: './jobs-management.component.html'
})
export class JobsManagementComponent {}
