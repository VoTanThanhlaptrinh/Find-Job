import { TestBed } from '@angular/core/testing';
import { CanActivateFn } from '@angular/router';

import { hirerGuardGuard } from './hirer-guard.guard';

describe('hirerGuardGuard', () => {
  const executeGuard: CanActivateFn = (...guardParameters) => 
      TestBed.runInInjectionContext(() => hirerGuardGuard(...guardParameters));

  beforeEach(() => {
    TestBed.configureTestingModule({});
  });

  it('should be created', () => {
    expect(executeGuard).toBeTruthy();
  });
});
