import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateDemoProjectComponent } from './create-demo-project.component';

describe('CreateDemoProjectComponent', () => {
  let component: CreateDemoProjectComponent;
  let fixture: ComponentFixture<CreateDemoProjectComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CreateDemoProjectComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CreateDemoProjectComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
