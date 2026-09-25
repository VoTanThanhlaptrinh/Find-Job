import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { provideRouter } from '@angular/router';
import { AdminPageHeaderComponent, AdminBreadcrumbItem } from './page-header.component';

@Component({
  standalone: true,
  imports: [AdminPageHeaderComponent],
  template: `
    <app-admin-page-header
      [title]="title"
      [description]="description"
      [breadcrumbs]="breadcrumbs"
      [actionLabel]="actionLabel"
      (actionClick)="onPrimaryClick()"
    >
      <button header-secondary-action type="button" class="test-sec-btn" (click)="onSecClick()">
        Xuất file
      </button>
      <button header-primary-action type="button" class="test-pri-btn" (click)="onCustomPriClick()">
        Tạo mới
      </button>
    </app-admin-page-header>
  `,
})
class TestHostComponent {
  title = 'Quản lý việc làm';
  description = 'Theo dõi và quản lý danh sách việc làm';
  breadcrumbs: AdminBreadcrumbItem[] = [
    { label: 'Quản trị', routerLink: '/admin/dashboard' },
    { label: 'Tuyển dụng', routerLink: '/admin/recruitment' },
    { label: 'Việc làm' },
  ];
  actionLabel = '';
  primaryClicked = false;
  secClicked = false;
  customPriClicked = false;

  onPrimaryClick(): void {
    this.primaryClicked = true;
  }
  onSecClick(): void {
    this.secClicked = true;
  }
  onCustomPriClick(): void {
    this.customPriClicked = true;
  }
}

describe('AdminPageHeaderComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let hostComponent: TestHostComponent;
  let headerElement: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, AdminPageHeaderComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
    headerElement = fixture.nativeElement.querySelector('header');
  });

  it('should render header landmark with a single H1 title', () => {
    expect(headerElement).toBeTruthy();

    const h1Elements = headerElement.querySelectorAll('h1');
    expect(h1Elements.length).toBe(1);
    expect(h1Elements[0].textContent?.trim()).toBe('Quản lý việc làm');
  });

  it('should render description when provided', () => {
    const descEl = headerElement.querySelector('p');
    expect(descEl?.textContent?.trim()).toBe('Theo dõi và quản lý danh sách việc làm');
  });

  it('should render breadcrumb with routerLink on preceding items and aria-current on the last item', () => {
    const navEl = headerElement.querySelector('nav[aria-label="Đường dẫn trang"]');
    expect(navEl).toBeTruthy();

    const links = navEl?.querySelectorAll('a');
    expect(links?.length).toBe(2);
    expect(links?.[0].textContent?.trim()).toContain('Quản trị');
    expect(links?.[1].textContent?.trim()).toBe('Tuyển dụng');

    const currentSpan = navEl?.querySelector('[aria-current="page"]');
    expect(currentSpan).toBeTruthy();
    expect(currentSpan?.textContent?.trim()).toBe('Việc làm');
  });

  it('should project secondary action before primary action', () => {
    const secBtn = headerElement.querySelector('.test-sec-btn');
    const priBtn = headerElement.querySelector('.test-pri-btn');

    expect(secBtn).toBeTruthy();
    expect(priBtn).toBeTruthy();

    // Verify DOM position (secondary before primary)
    expect(secBtn?.compareDocumentPosition(priBtn!) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it('should trigger click handlers on projected action buttons', () => {
    const secBtn = headerElement.querySelector('.test-sec-btn') as HTMLButtonElement;
    secBtn.click();
    expect(hostComponent.secClicked).toBeTrue();

    const priBtn = headerElement.querySelector('.test-pri-btn') as HTMLButtonElement;
    priBtn.click();
    expect(hostComponent.customPriClicked).toBeTrue();
  });

  it('should render direct action button when actionLabel is set and emit actionClick', () => {
    hostComponent.actionLabel = 'Thêm quản trị viên';
    fixture.detectChanges();

    const directBtn = Array.from(headerElement.querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Thêm quản trị viên')
    );
    expect(directBtn).toBeTruthy();

    directBtn?.click();
    expect(hostComponent.primaryClicked).toBeTrue();
  });
});
