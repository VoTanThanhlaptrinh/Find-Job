import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ApplySuccessModalComponent } from './apply-success-modal.component';

describe('ApplySuccessModalComponent', () => {
  let component: ApplySuccessModalComponent;
  let fixture: ComponentFixture<ApplySuccessModalComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplySuccessModalComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ApplySuccessModalComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
