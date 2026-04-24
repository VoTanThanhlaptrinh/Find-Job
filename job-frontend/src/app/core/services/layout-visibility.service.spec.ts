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
    expect(visibility.hiddenHeaderReason).toBe('matched hidden header prefix: /recruiter');
    expect(visibility.hiddenFooterReason).toBe('matched hidden footer prefix: /recruiter');
  });

  it('keeps seeker shell visible on non-recruiter routes', () => {
    const visibility = service.getLayoutVisibility('/dashboard');

    expect(visibility.showHeader).toBeTrue();
    expect(visibility.showFooter).toBeTrue();
    expect(visibility.hiddenHeaderReason).toBeNull();
    expect(visibility.hiddenFooterReason).toBeNull();
  });

  it('hides layout on reset password routes', () => {
    const visibility = service.getLayoutVisibility('/reset-pass/abc123');

    expect(visibility.showHeader).toBeFalse();
    expect(visibility.showFooter).toBeFalse();
    expect(visibility.hiddenHeaderReason).toBe('matched hidden header reset password route');
    expect(visibility.hiddenFooterReason).toBe('matched hidden footer reset password route');
  });

  it('can hide only the header for header-only routes', () => {
    const visibility = service.getLayoutVisibility('/login-callback');

    expect(visibility.showHeader).toBeFalse();
    expect(visibility.showFooter).toBeTrue();
    expect(visibility.hiddenHeaderReason).toBe('matched hidden header route: /login-callback');
    expect(visibility.hiddenFooterReason).toBeNull();
  });
});
