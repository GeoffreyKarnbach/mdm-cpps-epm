import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { User } from '../../../dtos/User';
import { DomainWorkspace } from '../../../dtos/DomainWorkspace';
import { FeedbackService } from '../../../services/feedback.service';
import { ProjectUser } from '../../../dtos/ProjectUser';

@Component({
  selector: 'app-domain-workspace-list',
  templateUrl: './domain-workspace-list.component.html',
  styleUrl: './domain-workspace-list.component.scss',
})
export class DomainWorkspaceListComponent implements OnInit {
  constructor(private feedbackService: FeedbackService) {}

  ngOnInit(): void {
    this.availableUsers = this.availableProjectUsers.map((pu) => pu.user);

    for (let i = 0; i < this.domainWorkspaces.length; i++) {
      this.domainWorkspacesAreNew.push(false);
    }
  }

  @Input() mode: string = 'create';
  @Input() gitlabHasBeenInitiatedAlready: boolean = false;
  @Input() commonWorkspaceUsers: User[] = [];

  @Input() availableProjectUsers: ProjectUser[] = [];
  availableUsers: User[] = [];

  @Input() domainWorkspaces: DomainWorkspace[] = [];
  domainWorkspacesAreNew: boolean[] = [];
  @Output() nextStep = new EventEmitter<any>();

  addDomainWorkspace(): void {
    this.domainWorkspaces.push({
      id: 0,
      name: '',
      users: [],
    });

    this.domainWorkspacesAreNew.push(true);
  }

  removeDomainWorkspace(index: number): void {
    this.domainWorkspaces.splice(index, 1);
  }

  goToNextStep(): void {
    // For each domain workspace, check if a name is set and at least one user is selected
    for (let i = 0; i < this.domainWorkspaces.length; i++) {
      for (let j = 0; j < this.domainWorkspaces[i].users.length; j++) {
        if (
          !this.commonWorkspaceUsers.find(
            (user) => user.id === this.domainWorkspaces[i].users[j].id
          )
        ) {
          this.feedbackService.showError(
            'All users in domain workspace ' +
              (i + 1) +
              ' need to be added to the common workspace'
          );
          return;
        }
      }

      if (
        !this.domainWorkspaces[i].name ||
        this.domainWorkspaces[i].name === ''
      ) {
        this.feedbackService.showError(
          'Please enter a name for domain workspace ' + (i + 1)
        );
        return;
      }
      if (this.domainWorkspaces[i].users.length === 0) {
        this.feedbackService.showError(
          'Please select at least one user for domain workspace ' + (i + 1)
        );
        return;
      }
    }
    this.nextStep.emit();
  }
}
