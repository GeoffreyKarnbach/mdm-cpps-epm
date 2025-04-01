import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ExternalToolsDetailsComponent } from './external-tools-details.component';

describe('ExternalToolsDetailsComponent', () => {
  let component: ExternalToolsDetailsComponent;
  let fixture: ComponentFixture<ExternalToolsDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ExternalToolsDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ExternalToolsDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
