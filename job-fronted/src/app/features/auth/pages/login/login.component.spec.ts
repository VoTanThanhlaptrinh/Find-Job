import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../../../core/services/auth.service';
import { NotifyMessageService } from '../../../../core/services/notify-message.service';

import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;

  const authServiceMock = {
    getGoogleLoginUrl: jasmine
      .createSpy('getGoogleLoginUrl')
      .and.returnValue(of({ data: 'https://accounts.google.com' })),
    login: jasmine.createSpy('login').and.returnValue(of({ data: {} })),
  };

  const routerMock = {
    navigate: jasmine.createSpy('navigate').and.returnValue(Promise.resolve(true)),
  };

  const notifyMock = {
    showMessage: jasmine.createSpy('showMessage'),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: NotifyMessageService, useValue: notifyMock },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParams: of({}),
          },
        },
      ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
