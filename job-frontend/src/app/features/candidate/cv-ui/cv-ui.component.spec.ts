import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ToastrService } from 'ngx-toastr';
import { CvUiComponent } from './cv-ui.component';
import { CvUploadModalComponent } from '../cv-upload-modal/cv-upload-modal.component';
import { SseService } from '../../../core/services/sse.service';

describe('CvUiComponent', () => {
  let component: CvUiComponent;
  let fixture: ComponentFixture<CvUiComponent>;
  let httpMock: HttpTestingController;

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

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CvUiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    httpMock.expectOne(req => req.url.endsWith('/user/resumes')).flush({ status: 200, message: 'OK', data: [] });
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should upload CV to cloud temp on modal file selection and call complete only when submitting modal', () => {
    component.openUploadModal();
    fixture.detectChanges();

    const modalDebug = fixture.debugElement.query(By.directive(CvUploadModalComponent));
    const modal = modalDebug.componentInstance as CvUploadModalComponent;

    const file = new File(['cv-content'], 'profile.pdf', { type: 'application/pdf', lastModified: 1700000020000 });
    const fakeInput = { files: [file], value: 'profile.pdf' } as unknown as HTMLInputElement;

    modal.onFileInputChange({ target: fakeInput } as unknown as Event);

    const initReq = httpMock.expectOne(req => req.url.endsWith('/user/resume-uploads/initiate'));
    initReq.flush({
      status: 201,
      message: 'Created',
      data: {
        uploadId: 'modal-up-1',
        uploadUrl: 'https://r2.example.com/modal-temp',
        method: 'PUT',
        requiredHeaders: { 'Content-Type': 'application/pdf' },
        expiresAt: new Date(Date.now() + 600000).toISOString()
      }
    });

    const putReq = httpMock.expectOne('https://r2.example.com/modal-temp');
    expect(putReq.request.method).toBe('PUT');
    fixture.detectChanges();
    expect(modalDebug.nativeElement.textContent).not.toContain('profile.pdf');

    putReq.flush({}, { status: 200, statusText: 'OK' });
    fixture.detectChanges();

    httpMock.expectNone(req => req.url.endsWith('/complete'));
    expect(modal.stagedUploadId).toBe('modal-up-1');
    expect(modal.isUploadingToTemp).toBeFalse();
    expect(modalDebug.nativeElement.textContent).toContain('profile.pdf');

    modal.onSubmit();

    const completeReq = httpMock.expectOne(req => req.url.endsWith('/user/resume-uploads/modal-up-1/complete'));
    expect(completeReq.request.method).toBe('POST');
    completeReq.flush({
      status: 200,
      message: 'OK',
      data: { id: 888, fileName: 'profile.pdf', createDate: new Date().toISOString(), status: 'UPLOADED' }
    });

    expect(component.resumes()[0].id).toBe(888);
    expect(component.isUploadModalOpen).toBeFalse();
  });
});
