import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { NavigationItem, QueryParams } from '../dtos/NavigationItem';

@Injectable({
  providedIn: 'root',
})
export class RoutingService {
  private nagivateSubject = new Subject<NavigationItem>();
  private navigationStack: NavigationItem[] = [
    {
      component: 'project-list',
      queryParams: {},
    },
  ];

  private currentStackPosition = 0;

  navigate$ = this.nagivateSubject.asObservable();

  constructor() {}

  public navigateToComponent(
    component: string,
    queryParams: QueryParams
  ): void {
    this.nagivateSubject.next({ component, queryParams });
    this.navigationStack.push({ component, queryParams });
    this.currentStackPosition = this.navigationStack.length - 1;
  }

  public navigatePreviousComponent() {
    this.currentStackPosition = this.currentStackPosition - 1;
    const previousNavigationItem =
      this.navigationStack[this.currentStackPosition];

    if (!previousNavigationItem) {
      this.currentStackPosition = 0;
      return;
    }
    this.nagivateSubject.next(previousNavigationItem);
  }

  public navigateNextComponent() {
    this.currentStackPosition = this.currentStackPosition + 1;
    const nextNavigationItem = this.navigationStack[this.currentStackPosition];

    if (!nextNavigationItem) {
      this.currentStackPosition = this.currentStackPosition - 1;
      return;
    }
    this.nagivateSubject.next(nextNavigationItem);
  }
}
