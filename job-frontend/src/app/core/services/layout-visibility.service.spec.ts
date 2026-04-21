import { LayoutVisibilityService } from './layout-visibility.service';

describe('LayoutVisibilityService', () => {
  let service: LayoutVisibilityService;

  beforeEach(() => {
    service = new LayoutVisibilityService();
  });

  it('hides seeker shell on recruiter routes', () => {
    const visibility = service.getLayoutVisibility('/recruiter/dashboard');

    expect(visibility.showHeader).toBeFalse();
    expect(visibility.showFooter).toBeFalse();
    expect(visibility.hiddenReason).toBe('matched hidden prefix: /recruiter');
  });

  it('keeps seeker shell visible on non-recruiter routes', () => {
    const visibility = service.getLayoutVisibility('/dashboard');

    expect(visibility.showHeader).toBeTrue();
    expect(visibility.showFooter).toBeTrue();
    expect(visibility.hiddenReason).toBeNull();
  });

  it('hides layout on reset password routes', () => {
    const visibility = service.getLayoutVisibility('/reset-pass/abc123');

    expect(visibility.showHeader).toBeFalse();
    expect(visibility.showFooter).toBeFalse();
    expect(visibility.hiddenReason).toBe('matched reset password route');
  });
});
