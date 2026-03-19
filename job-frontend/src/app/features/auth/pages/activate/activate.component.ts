import { Component, OnInit } from '@angular/core';
import { AccountService } from '../../../../core/services/account.service';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-activate',
  imports: [],
  templateUrl: './activate.component.html',
  styleUrl: './activate.component.css'
})
export class ActivateComponent implements OnInit {
  constructor(private accountService: AccountService, private activateRoute: ActivatedRoute) { }
  message: string = "";
  ngOnInit(): void {
    let token = this.activateRoute.snapshot.queryParams['token'];
    this.accountService.activate(token).subscribe({
      next: (res) => {
        this.message = "✔️ Xác thực thành công"
      },
      error: (err) => {
        this.message = err.error.message;
      }
    }

    );
  }
}
