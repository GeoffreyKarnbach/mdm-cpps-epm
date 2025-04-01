import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DomainWorkspaceListComponent } from './domain-workspace-list.component';

describe('DomainWorkspaceListComponent', () => {
  let component: DomainWorkspaceListComponent;
  let fixture: ComponentFixture<DomainWorkspaceListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [DomainWorkspaceListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DomainWorkspaceListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
