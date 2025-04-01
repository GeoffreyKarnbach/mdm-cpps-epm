import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DomainWorkspaceComponent } from './domain-workspace.component';

describe('DomainWorkspaceComponent', () => {
  let component: DomainWorkspaceComponent;
  let fixture: ComponentFixture<DomainWorkspaceComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [DomainWorkspaceComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DomainWorkspaceComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
