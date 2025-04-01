import { Injectable } from '@angular/core';
import { ErrorMessage } from '../dtos/ErrorMessage';
import { Subject } from 'rxjs';
import { SuccessMessage } from '../dtos/SuccessMessage';

@Injectable({
  providedIn: 'root',
})
export class FeedbackService {
  private errorSubject = new Subject<ErrorMessage>();
  error$ = this.errorSubject.asObservable();

  private successSubject = new Subject<SuccessMessage>();
  success$ = this.successSubject.asObservable();

  constructor() {}

  public showError(message: string, duration: number = 2500): void {
    this.errorSubject.next({ message, duration });
    window.scrollTo(0, 0);
  }

  public showSuccess(message: string, duration: number = 2500): void {
    this.successSubject.next({ message, duration });
    window.scrollTo(0, 0);
  }
}
