import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CvUiComponent } from './cv-ui.component';

describe('CvUiComponent', () => {
  let component: CvUiComponent;
  let fixture: ComponentFixture<CvUiComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CvUiComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CvUiComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
