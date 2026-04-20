import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AdminPageHeaderComponent } from '../../components/shared/page-header/page-header.component';
import { AdminMetricCardComponent } from '../../components/shared/metric-card/metric-card.component';

@Component({
  selector: 'app-employers',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    AdminPageHeaderComponent,
    AdminMetricCardComponent,
  ],
  templateUrl: './employers.component.html'
})
export class EmployersComponent {}
