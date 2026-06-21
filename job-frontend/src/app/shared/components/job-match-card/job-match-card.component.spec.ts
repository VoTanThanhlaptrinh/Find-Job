import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JobMatchCardComponent } from './job-match-card.component';

describe('JobMatchCardComponent', () => {
  let component: JobMatchCardComponent;
  let fixture: ComponentFixture<JobMatchCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JobMatchCardComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(JobMatchCardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
