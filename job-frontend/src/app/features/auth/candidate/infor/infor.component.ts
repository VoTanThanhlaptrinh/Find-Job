import { Component, CUSTOM_ELEMENTS_SCHEMA, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-infor',
  imports: [RouterModule],
  templateUrl: './infor.component.html',
  styleUrl: './infor.component.css'
})
export class InforComponent {
}
