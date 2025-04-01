import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DemoProjectConfigComponent } from './demo-project-config.component';

describe('DemoProjectConfigComponent', () => {
  let component: DemoProjectConfigComponent;
  let fixture: ComponentFixture<DemoProjectConfigComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [DemoProjectConfigComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DemoProjectConfigComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
