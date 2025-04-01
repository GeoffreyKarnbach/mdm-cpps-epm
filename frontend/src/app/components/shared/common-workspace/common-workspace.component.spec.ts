import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CommonWorkspaceComponent } from './common-workspace.component';

describe('CommonWorkspaceComponent', () => {
  let component: CommonWorkspaceComponent;
  let fixture: ComponentFixture<CommonWorkspaceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [CommonWorkspaceComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CommonWorkspaceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
