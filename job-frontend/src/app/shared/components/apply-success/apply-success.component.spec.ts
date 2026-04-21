import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ApplySuccessComponent } from './apply-success.component';

describe('ApplySuccessComponent', () => {
  let component: ApplySuccessComponent;
  let fixture: ComponentFixture<ApplySuccessComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ApplySuccessComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ApplySuccessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
