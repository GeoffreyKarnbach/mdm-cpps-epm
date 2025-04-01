import { Component, OnInit } from '@angular/core';
import { Subscription } from 'rxjs';
import { FeedbackService } from '../../../services/feedback.service';

@Component({
  selector: 'app-error-banner',
  templateUrl: './error-banner.component.html',
  styleUrl: './error-banner.component.scss',
})
export class ErrorBannerComponent implements OnInit {
  showBanner: boolean = false;
  message: string = '';
  progress = 100;
  intervalId: any;

  showSuccessBanner: boolean = false;
  successMessage: string = '';

  private errorSubscription: Subscription = new Subscription();
  private successSubscription: Subscription = new Subscription();

  constructor(private feedbackService: FeedbackService) {}

  ngOnInit(): void {
    this.errorSubscription = this.feedbackService.error$.subscribe(
      (errorMessage) => {
        this.showBanner = true;
        this.message = errorMessage.message;
        if (errorMessage.duration > 0) {
          this.progress = 100;
          this.startProgress(errorMessage.duration / 100);
        }
      }
    );
    this.successSubscription = this.feedbackService.success$.subscribe(
      (successMessage) => {
        this.showSuccessBanner = true;
        this.successMessage = successMessage.message;
        if (successMessage.duration > 0) {
          this.progress = 100;
          this.startProgress(successMessage.duration / 100);
        }
      }
    );
  }

  startProgress(steps: number): void {
    this.intervalId = setInterval(() => {
      this.progress -= 100 / steps;
      if (this.progress <= 0) {
        this.closeBanner();
      }
    }, 100);
  }

  closeBanner(): void {
    clearInterval(this.intervalId);
    this.showBanner = false;
    this.showSuccessBanner = false;
  }
}
