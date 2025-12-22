import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HistoryApplyComponent } from './history-apply.component';

describe('HistoryApplyComponent', () => {
  let component: HistoryApplyComponent;
  let fixture: ComponentFixture<HistoryApplyComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HistoryApplyComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(HistoryApplyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
