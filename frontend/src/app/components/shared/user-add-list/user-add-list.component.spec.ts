import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserAddListComponent } from './user-add-list.component';

describe('UserAddListComponent', () => {
  let component: UserAddListComponent;
  let fixture: ComponentFixture<UserAddListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [UserAddListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserAddListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
