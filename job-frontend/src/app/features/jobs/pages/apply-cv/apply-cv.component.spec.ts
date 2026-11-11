import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { signal } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ApplyCvComponent } from './apply-cv.component';
import { SseService } from '../../../../core/services/sse.service';

describe('ApplyCvComponent', () => {
  let component: ApplyCvComponent;
  let fixture: ComponentFixture<ApplyCvComponent>;
  let httpMock: HttpTestingController;

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

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ApplyCvComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    httpMock.expectOne(req => req.url.endsWith('/user/resumes')).flush({ status: 200, message: 'OK', data: [] });
    httpMock.expectOne(req => req.url.endsWith('/user/applications/1/status')).flush({ status: 200, message: 'OK', data: false });
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should upload CV to cloud temp on file selection and confirm only when submitting application', () => {
    const file = new File(['cv-bytes'], 'my-cv.pdf', { type: 'application/pdf', lastModified: 1700000010000 });
    const fakeInput = { files: [file], value: 'my-cv.pdf' } as unknown as HTMLInputElement;

    component.onFileInputChange({ target: fakeInput } as unknown as Event);

    const initReq = httpMock.expectOne(req => req.url.endsWith('/user/resume-uploads/initiate'));
    initReq.flush({
      status: 201,
      message: 'Created',
      data: {
        uploadId: 'apply-up-1',
        uploadUrl: 'https://r2.example.com/apply-temp',
        method: 'PUT',
        requiredHeaders: { 'Content-Type': 'application/pdf' },
        expiresAt: new Date(Date.now() + 600000).toISOString()
      }
    });

    const putReq = httpMock.expectOne('https://r2.example.com/apply-temp');
    expect(putReq.request.method).toBe('PUT');
    putReq.flush({}, { status: 200, statusText: 'OK' });

    // Confirm (/complete) must NOT have been called yet
    httpMock.expectNone(req => req.url.endsWith('/complete'));
    httpMock.expectNone(req => req.url.endsWith('/user/applications/submit-existing'));
    expect(component.stagedUploadId).toBe('apply-up-1');
    expect(component.isUploadingCv).toBeFalse();

    // Now click Submit
    component.onSubmit();

    const completeReq = httpMock.expectOne(req => req.url.endsWith('/user/resume-uploads/apply-up-1/complete'));
    expect(completeReq.request.method).toBe('POST');
    completeReq.flush({
      status: 200,
      message: 'OK',
      data: { id: 555, fileName: 'my-cv.pdf', createDate: new Date().toISOString(), status: 'UPLOADED' }
    });

    const applyReq = httpMock.expectOne(req => req.url.endsWith('/user/applications/submit-existing'));
    expect(applyReq.request.method).toBe('POST');
    expect(applyReq.request.body).toEqual({
      jobId: 1,
      existingCvId: 555,
      coverLetter: ''
    });
    applyReq.flush({ status: 200, message: 'Applied', data: null });
  });
});
