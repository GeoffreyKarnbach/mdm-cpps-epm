import {
  Component,
  EventEmitter,
  Injector,
  Input,
  Output,
  InjectionToken,
} from '@angular/core';
import { CppsProject } from '../../../dtos/CppsProject';

@Component({
  selector: 'app-stepper',
  templateUrl: './stepper.component.html',
  styleUrls: ['./stepper.component.scss'],
})
export class StepperComponent {
  @Input() steps: { name: string }[] = [];
  @Input() mode: string = 'create';
  @Input() isDemoProject: boolean = false;

  @Output() stepperEvent = new EventEmitter<any>();

  @Input() cppsProject: CppsProject = {
    id: -1,
    details: {
      name: '',
      description: '',
      version: '',
      demo: false,
    },
    users: [],
    domainWorkspaces: [],
    commonWorkspace: {
      id: -1,
      name: '',
      users: [],
    },
    externalToolsDetails: {
      rootGroupId: 0,
      creatorGitlabUserId: 0,
      gitlabProjectId: 0,
      mergeRequestTemplates: [],
      issueTemplates: [],
      gitlabHasBeenGenerated: false,
    },
  };

  domainCount: number = 0;

  catchEvent(event: any) {
    this.stepperEvent.emit(event);
  }

  currentStep: number = 0;
  furthestSkippableStep: number = 0;

  goToStep(stepIndex: number): void {
    if (this.mode == 'create' && stepIndex > this.furthestSkippableStep) {
      return;
    }

    if (stepIndex >= 0 && stepIndex < this.steps.length) {
      this.furthestSkippableStep = Math.max(
        this.furthestSkippableStep,
        this.currentStep
      );
      this.currentStep = stepIndex;
    }
  }

  nextStep(event: any): void {
    if (this.currentStep < this.steps.length - 1) {
      this.currentStep++;
      this.furthestSkippableStep = Math.max(
        this.furthestSkippableStep,
        this.currentStep
      );
    } else if (this.currentStep == this.steps.length - 1) {
      if (this.isDemoProject) {
        const domainCount = event.domainCount;
        this.stepperEvent.emit({ type: 'save', domainCount: domainCount });
      } else {
        this.stepperEvent.emit({ type: 'save' });
      }
    }
  }

  prevStep(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
    }
  }
}
