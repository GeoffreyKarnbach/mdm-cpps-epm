import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { FeedbackService } from '../../../services/feedback.service';
import { ExternalToolsDetails } from '../../../dtos/ExternalToolsDetails';
import { GitlabService } from '../../../services/gitlab.service';
import { GitlabGroupListItem } from '../../../dtos/GitlabGroupListItem';

@Component({
  selector: 'app-external-tools-details',
  templateUrl: './external-tools-details.component.html',
  styleUrl: './external-tools-details.component.scss',
})
export class ExternalToolsDetailsComponent implements OnInit {
  constructor(
    private feedbackService: FeedbackService,
    private gitlabService: GitlabService
  ) {}

  ngOnInit(): void {
    this.gitlabService.getUserGroups().subscribe(
      (groups) => {
        this.groups = groups;
      },
      (error) => {
        this.feedbackService.showError('Failed to load GitLab groups', error);
      }
    );
  }

  @Input() externalToolsDetails: ExternalToolsDetails = {
    rootGroupId: 0,
    creatorGitlabUserId: 0,
    gitlabProjectId: 0,
    mergeRequestTemplates: [],
    issueTemplates: [],
    gitlabHasBeenGenerated: false,
  };

  @Input() mode: string = 'create';
  @Input() isDemoProject: boolean = false;

  @Output() nextStep = new EventEmitter<any>();

  groups: GitlabGroupListItem[] = [];

  next(): void {
    if (this.isDemoProject) {
      if (this.externalToolsDetails.rootGroupId == 0) {
        this.feedbackService.showError('Please select a root group');
        return;
      }
    }
    this.nextStep.emit();
  }

  addMergeRequestTemplate(): void {
    console.log(this.externalToolsDetails);
    this.externalToolsDetails.mergeRequestTemplates.push({
      name: '',
      content: '',
    });
  }

  removeMergeRequestTemplate(index: number): void {
    this.externalToolsDetails.mergeRequestTemplates.splice(index, 1);
  }
}
