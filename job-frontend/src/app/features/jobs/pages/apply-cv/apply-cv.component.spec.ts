import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ApplyCvComponent } from './apply-cv.component';
import { SseService } from '../../../../core/services/sse.service';

describe('ApplyCvComponent', () => {
  let component: ApplyCvComponent;
  let fixture: ComponentFixture<ApplyCvComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplyCvComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            params: of({ id: '1' }),
            snapshot: { params: { id: '1' } }
          }
        },
        {
          provide: ToastrService,
          useValue: jasmine.createSpyObj('ToastrService', ['success', 'error', 'info', 'warning', 'clear'])
        },
        {
          provide: SseService,
          useValue: {
            fromEvent: jasmine.createSpy('fromEvent').and.returnValue(signal(null)),
            clearEvent: jasmine.createSpy('clearEvent')
          }
        }
      ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ApplyCvComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
