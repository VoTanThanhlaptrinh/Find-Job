import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ToastrService } from 'ngx-toastr';
import { ResumeService } from './resume.service';
import { SseService } from './sse.service';

describe('ResumeService Presigned Upload & Double-click', () => {
  let service: ResumeService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ResumeService,
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
    });

    service = TestBed.inject(ResumeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create only one upload execution and one optimistic card on double-click', () => {
    const file = new File(['dummy-content'], 'resume.pdf', { type: 'application/pdf', lastModified: 1700000000000 });

    // Simulate double-click
    service.uploadResumePresigned(file, false);
    service.uploadResumePresigned(file, false);

    // Verify optimistic resumes: only 1 entry was added, not 2
    expect(service.resumes$().length).toBe(1);
    expect(service.resumes$()[0].fileName).toBe('resume.pdf');
    expect(service.isUploadInFlight(file)).toBeTrue();

    // Verify only 1 initiate HTTP POST request was made
    const initiateRequests = httpMock.match(req => req.url.endsWith('/user/resume-uploads/initiate'));
    expect(initiateRequests.length).toBe(1);

    const initReq = initiateRequests[0];
    expect(initReq.request.method).toBe('POST');
    const idempotencyKey = initReq.request.headers.get('Idempotency-Key');
    expect(idempotencyKey).toBeTruthy();

    // Respond to initiate
    initReq.flush({
      status: 200,
      message: 'Success',
      data: {
        uploadId: 'test-upload-id',
        uploadUrl: 'https://s3.example.com/presigned-put',
        httpMethod: 'PUT',
        requiredHeaders: { 'Content-Type': 'application/pdf' },
        expiresAt: new Date(Date.now() + 900000).toISOString()
      }
    });

    // Verify PUT request
    const putReq = httpMock.expectOne('https://s3.example.com/presigned-put');
    expect(putReq.request.method).toBe('PUT');
    putReq.flush({}, { status: 200, statusText: 'OK' });

    // Verify complete request
    const completeReq = httpMock.expectOne(req => req.url.endsWith('/user/resume-uploads/test-upload-id/complete'));
    expect(completeReq.request.method).toBe('POST');
    completeReq.flush({
      status: 200,
      message: 'Completed',
      data: {
        id: 123,
        fileName: 'resume.pdf',
        createdAt: new Date().toISOString(),
        status: 'COMPLETED'
      }
    });

    // Workflow finished
    expect(service.isUploadInFlight(file)).toBeFalse();
    expect(service.resumes$()[0].id).toBe(123);
  });

  it('should reuse the same idempotency key on retry after network error', () => {
    const file = new File(['dummy-content'], 'resume-retry.pdf', { type: 'application/pdf', lastModified: 1700000001000 });

    // First attempt
    service.uploadResumePresigned(file, false);
    const firstInitReq = httpMock.expectOne(req => req.url.endsWith('/user/resume-uploads/initiate'));
    const firstKey = firstInitReq.request.headers.get('Idempotency-Key');
    expect(firstKey).toBeTruthy();

    // Simulate network failure on initiate
    firstInitReq.error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });

    // After failure, in-flight state is cleared, but idempotency key is preserved
    expect(service.isUploadInFlight(file)).toBeFalse();

    // Retry upload with same file
    service.uploadResumePresigned(file, false);
    const retryInitReq = httpMock.expectOne(req => req.url.endsWith('/user/resume-uploads/initiate'));
    const retryKey = retryInitReq.request.headers.get('Idempotency-Key');

    // Must reuse exact same idempotency key
    expect(retryKey).toBe(firstKey);

    // Cancel pending retry req for cleanup
    retryInitReq.error(new ProgressEvent('error'), { status: 500, statusText: 'Server Error' });
  });

  it('should clear idempotency key on success so subsequent new upload gets fresh key', () => {
    const file = new File(['dummy-content'], 'resume-success.pdf', { type: 'application/pdf', lastModified: 1700000002000 });

    service.uploadResumePresigned(file, false);
    const initReq = httpMock.expectOne(req => req.url.endsWith('/user/resume-uploads/initiate'));
    const firstKey = initReq.request.headers.get('Idempotency-Key');

    initReq.flush({
      status: 200,
      message: 'Success',
      data: {
        uploadId: 'up-1',
        uploadUrl: 'https://s3.example.com/put-1',
        httpMethod: 'PUT',
        requiredHeaders: {},
        expiresAt: new Date().toISOString()
      }
    });

    const putReq = httpMock.expectOne('https://s3.example.com/put-1');
    putReq.flush({}, { status: 200, statusText: 'OK' });

    const completeReq = httpMock.expectOne(req => req.url.endsWith('/user/resume-uploads/up-1/complete'));
    completeReq.flush({
      status: 200,
      message: 'OK',
      data: { id: 456, fileName: 'resume-success.pdf', createdAt: new Date().toISOString(), status: 'COMPLETED' }
    });

    // Next time after successful completion, a new intent key is generated
    const newKey = service.getOrCreateIdempotencyKey(file);
    expect(newKey).not.toBe(firstKey);
  });
});
