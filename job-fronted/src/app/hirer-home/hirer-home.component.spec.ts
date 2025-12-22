import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HirerHomeComponent } from './hirer-home.component';

describe('HirerHomeComponent', () => {
  let component: HirerHomeComponent;
  let fixture: ComponentFixture<HirerHomeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HirerHomeComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HirerHomeComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
