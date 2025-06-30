import {Component, OnInit} from '@angular/core';
import {NgIf} from "@angular/common";
import {FormControl, FormControlName, FormGroup, ReactiveFormsModule, Validators} from "@angular/forms";
import {ActivatedRoute, Router, RouterLink} from '@angular/router';
import {AuthService} from '../services/auth.service';
import {map} from 'rxjs/operators';
import {take} from 'rxjs';

@Component({
  selector: 'app-reset-pass',
  imports: [
    NgIf,
    ReactiveFormsModule,
    RouterLink
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
  })
  constructor(private route: ActivatedRoute
             ,private authService: AuthService
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
    if(!this.authService.checkRandom(random)){
      this.router.navigate(['login'])
      return;
    }

  }

  onSubmit() {
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
