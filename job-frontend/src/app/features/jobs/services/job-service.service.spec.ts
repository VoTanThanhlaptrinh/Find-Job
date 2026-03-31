import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { JobService } from './job.service';
import { UtilitiesService } from '../../../core/services/utilities.service';
import { JobListApiResponse } from '../../../shared/models/jobs/job-api-response.model';

describe('JobService', () => {
  let service: JobService;
  let httpMock: HttpTestingController;
  let mockUtilitiesService: jasmine.SpyObj<UtilitiesService>;

  beforeEach(() => {
    mockUtilitiesService = jasmine.createSpyObj('UtilitiesService', ['getURLDev']);
    mockUtilitiesService.getURLDev.and.returnValue('http://localhost:8080/api');

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        JobService,
        { provide: UtilitiesService, useValue: mockUtilitiesService }
      ]
    });

    service = TestBed.inject(JobService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('listJobUserApplied', () => {
    it('should call GET with correct URL and credentials', () => {
      const pageIndex = 0;
      const pageSize = 10;
      const mockResponse: JobListApiResponse = {
        message: 'success',
        status: 200,
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0
        }
      };

      service.listJobUserApplied(pageIndex, pageSize).subscribe(res => {
        expect(res).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`http://localhost:8080/api/jobs/applied/${pageIndex}/${pageSize}`);
      expect(req.request.method).toBe('GET');
      expect(req.request.withCredentials).toBeTrue();
      req.flush(mockResponse);
    });

    it('should handle HTTP error properly', () => {
      const pageIndex = 1;
      const pageSize = 5;

      service.listJobUserApplied(pageIndex, pageSize).subscribe({
        next: () => fail('Should have failed with the 500 error'),
        error: (error) => {
          expect(error.status).toEqual(500);
        }
      });

      const req = httpMock.expectOne(`http://localhost:8080/api/jobs/applied/${pageIndex}/${pageSize}`);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });
    });
  });
});
