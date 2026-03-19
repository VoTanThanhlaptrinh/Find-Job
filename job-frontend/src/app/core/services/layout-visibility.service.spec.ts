import { TestBed } from '@angular/core/testing';

import { LayoutVisibilityService } from './layout-visibility.service';

describe('LayoutVisibilityService', () => {
  let service: LayoutVisibilityService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(LayoutVisibilityService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
