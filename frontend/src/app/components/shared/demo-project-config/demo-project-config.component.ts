import { Component, EventEmitter, Output } from '@angular/core';
import { FeedbackService } from '../../../services/feedback.service';

@Component({
  selector: 'app-demo-project-config',
  templateUrl: './demo-project-config.component.html',
  styleUrl: './demo-project-config.component.scss',
})
export class DemoProjectConfigComponent {
  constructor(private feedbackService: FeedbackService) {}

  @Output() nextStep = new EventEmitter<any>();

  domainCount = 0;

  save(): void {
    if (this.domainCount === 0) {
      this.feedbackService.showError(
        'Please select a domain count higher than 0'
      );
      return;
    }
    this.nextStep.emit({ domainCount: this.domainCount });
  }
}
