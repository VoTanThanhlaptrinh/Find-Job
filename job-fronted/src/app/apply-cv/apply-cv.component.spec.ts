import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ApplyCvComponent } from './apply-cv.component';

describe('ApplyCvComponent', () => {
  let component: ApplyCvComponent;
  let fixture: ComponentFixture<ApplyCvComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplyCvComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ApplyCvComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
