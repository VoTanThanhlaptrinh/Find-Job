import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { CvUiComponent } from './cv-ui.component';
import { SseService } from '../../../core/services/sse.service';

describe('CvUiComponent', () => {
  let component: CvUiComponent;
  let fixture: ComponentFixture<CvUiComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CvUiComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
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

    fixture = TestBed.createComponent(CvUiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
