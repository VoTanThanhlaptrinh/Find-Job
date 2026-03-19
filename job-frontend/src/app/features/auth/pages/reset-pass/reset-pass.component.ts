import {Component, OnInit} from '@angular/core';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {ActivatedRoute, Router} from '@angular/router';
import {AuthService} from '../../../../core/services/auth.service';
import {AccountService} from '../../../../core/services/account.service';
import {take} from 'rxjs';

@Component({
  selector: 'app-reset-pass',
  imports: [
    ReactiveFormsModule,
  ],
  templateUrl: './reset-pass.component.html',
  styleUrl: './reset-pass.component.css'
})
export class ResetPassComponent implements OnInit{
  isError : boolean = false
  message: string = ''
  random: string = ''
  formGroup = new FormGroup({
    newPass: new FormControl('', Validators.required)
    ,confirmPass: new FormControl('', Validators.required)
    ,random: new FormControl('', Validators.required)
  })
  constructor(private route: ActivatedRoute
             ,private authService: AuthService
             ,private accountService: AccountService
             ,private router: Router ) {
  }

  ngOnInit(): void {
    this.route.params.pipe(take(1)).subscribe(
      params =>{
        this.random = params['random'];
        this.checkRandom(this.random)
      }
    )
  }
  checkRandom(random:string){
    if(random === ''){
      this.router.navigate(['login'])
      return;
    }
    this.accountService.checkRandom(random).subscribe({
      error: err => {
        this.router.navigate(['login'])
        return;
      }
    });
  }

  onSubmit() {
    this.formGroup.patchValue({
      random: this.random
    })
    if(this.formGroup.invalid){
       this.formGroup.markAllAsTouched();
       this.isError = true;
       return
    }
    this.authService.resetPass(this.formGroup.value).subscribe({
      next: res =>{
        this.router.navigate(['login'])
      },error: err => {
        this.message = err?.error?.message || 'errors';
      }
    })
  }
}
