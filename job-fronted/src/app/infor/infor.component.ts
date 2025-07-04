import { Component, CUSTOM_ELEMENTS_SCHEMA, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-infor',
  imports: [RouterModule],
  templateUrl: './infor.component.html',
  styleUrl: './infor.component.css'
})
export class InforComponent implements OnInit {
  constructor(private auth: AuthService, private router:Router) { }
  ngOnInit(): void {
    if(!this.auth.checkLogin()){
      this.router.navigate(['/login'])
    }
  }
}
