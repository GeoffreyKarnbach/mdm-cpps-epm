import { Component, EventEmitter, Inject, Input, Output } from '@angular/core';
import { FeedbackService } from '../../../services/feedback.service';
import { ProjectDetails } from '../../../dtos/ProjectDetails';

@Component({
  selector: 'app-project-details',
  templateUrl: './project-details.component.html',
  styleUrl: './project-details.component.scss',
})
export class ProjectDetailsComponent {
  @Input() projectDetails: ProjectDetails = {
    name: '',
    description: '',
    version: '',
    demo: false,
  };

  @Input() mode: string = 'create';
  @Output() nextStep = new EventEmitter<any>();

  constructor(private feedbackService: FeedbackService) {}

  onSubmit() {
    if (!this.projectDetails.name) {
      this.feedbackService.showError('Project name is required');
      return;
    }
    if (!this.projectDetails.description) {
      this.feedbackService.showError('Project description is required');
      return;
    }
    if (!this.projectDetails.version) {
      this.feedbackService.showError('Project version is required');
      return;
    }

    this.nextStep.emit();
  }
}
