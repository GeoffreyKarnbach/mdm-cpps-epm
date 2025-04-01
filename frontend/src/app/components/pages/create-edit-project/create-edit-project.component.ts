import { Component, input, Input } from '@angular/core';
import { QueryParams } from '../../../dtos/NavigationItem';
import { RoutingService } from '../../../services/routing.service';
import { CppsProject } from '../../../dtos/CppsProject';
import { ProjectService } from '../../../services/project.service';
import { FeedbackService } from '../../../services/feedback.service';
import { UserService } from '../../../services/user.service';

@Component({
  selector: 'app-create-edit-project',
  templateUrl: './create-edit-project.component.html',
  styleUrl: './create-edit-project.component.scss',
})
export class CreateEditProjectComponent {
  @Input() public queryParams: QueryParams = {};
  title: string = '';

  cppsProject: CppsProject = {
    id: -1,
    details: {
      name: '',
      description: '',
      version: '0.0.1',
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
      mergeRequestTemplates: [
        {
          name: 'CPPS Merge Request',
          content: `## Description 
This merge request closes issue: [#ISSUE]

Other optional information: 

## Check List
Please, keep track of the MR **manually** here: 
- [x] You finalized your change request
- [x] You created the merge request
- [ ] Pipeline run on your changes to analyze impact of the change and to create Review Tasks (RT) if change impact was identified
- [ ] RTs were either closed, escalated or requested 
- [ ] requested RTs were manually assigned by its reviewer to an assignee of their choice
- [ ] requested RTs were reworked and closed
- [ ] The merge request was reviewed and can be merged`,
        },
      ],
      issueTemplates: [],
      gitlabHasBeenGenerated: false,
    },
  };

  mode: string = 'create';
  id: number = -1;

  mySteps = [
    { name: 'Project Details' },
    { name: 'Users List' },
    { name: 'Common Workspace' },
    { name: 'Domain Workspaces' },
    { name: 'External Tools' },
  ];

  constructor(
    public routingService: RoutingService,
    private projectService: ProjectService,
    private feedbackService: FeedbackService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    if (this.queryParams['mode'] === 'create') {
      this.title = 'Create MD-CPPS Project:';
      this.mode = 'create';
    } else {
      this.title = 'Edit MD-CPPS Project:';
      this.mode = 'edit';
      this.id = parseInt(this.queryParams['id']);

      this.projectService.getProjectById(this.id).subscribe(
        (project) => {
          this.cppsProject = project;
        },
        (error) => {
          this.feedbackService.showError('Failed to load project', error);
        }
      );
    }
  }

  catchEvent(event: any) {
    const eventType = event.type;

    if (eventType === 'userList') {
      this.cppsProject.users = event.data;
    }

    if (eventType === 'save') {
      if (this.mode === 'create') {
        this.projectService.createProject(this.cppsProject).subscribe(
          (project) => {
            this.routingService.navigateToComponent('project-view', {
              id: project.id.toString(),
              isAdmin: 'true',
            });
          },
          (error) => {
            let errorMessage = error.error.message + ':<br><ul>';
            for (let i = 0; i < error.error.errors.length; i++) {
              errorMessage += '<li>' + error.error.errors[i].message + '</li>';
            }
            errorMessage += '</ul>';
            this.feedbackService.showError(errorMessage);
          }
        );
      } else if (this.mode === 'edit') {
        this.projectService.updateProject(this.cppsProject).subscribe(
          (project) => {
            this.routingService.navigateToComponent('project-view', {
              id: project.id.toString(),
              isAdmin: 'true',
            });
          },
          (error) => {
            let errorMessage = error.error.message + ':<br><ul>';
            for (let i = 0; i < error.error.errors.length; i++) {
              errorMessage += '<li>' + error.error.errors[i].message + '</li>';
            }
            errorMessage += '</ul>';
            this.feedbackService.showError(errorMessage);
          }
        );
      }
    }
  }

  validateAndUpdate(): void {
    // Validate the content of project details
    if (
      !this.cppsProject.details.name ||
      this.cppsProject.details.name.trim() === ''
    ) {
      this.feedbackService.showError('Please enter a name for the project');
      return;
    }

    if (
      !this.cppsProject.details.description ||
      this.cppsProject.details.description.trim() === ''
    ) {
      this.feedbackService.showError(
        'Please enter a description for the project'
      );
      return;
    }

    if (
      !this.cppsProject.details.version ||
      this.cppsProject.details.version.trim() === ''
    ) {
      this.feedbackService.showError('Please enter a version for the project');
      return;
    }

    // Validate content of user list
    if (this.cppsProject.users.length === 0) {
      this.feedbackService.showError(
        'Please add at least one user to the project'
      );
      return;
    }

    this.userService.getCurrentUser().subscribe(
      (user) => {
        const currentUserId = user.id;
        const currentUser = this.cppsProject.users.find(
          (user) => user.user.id == currentUserId
        );
        if (!currentUser) {
          this.feedbackService.showError(
            'Please add yourself to the project to continue'
          );
          return;
        }
      },
      (error) => {
        this.feedbackService.showError('Error getting current user');
      }
    );

    // Validate content of common workspace
    if (this.cppsProject.commonWorkspace.name.trim() === '') {
      this.feedbackService.showError(
        'Please enter a name for the common workspace'
      );
      return;
    }

    if (this.cppsProject.commonWorkspace.users.length === 0) {
      this.feedbackService.showError(
        'Please add at least one user to the common workspace'
      );
      return;
    }

    // Validate content of domain workspaces
    for (let i = 0; i < this.cppsProject.domainWorkspaces.length; i++) {
      if (
        !this.cppsProject.domainWorkspaces[i].name ||
        this.cppsProject.domainWorkspaces[i].name.trim() === ''
      ) {
        this.feedbackService.showError(
          'Please enter a name for domain workspace ' + (i + 1)
        );
        return;
      }
      if (this.cppsProject.domainWorkspaces[i].users.length === 0) {
        this.feedbackService.showError(
          'Please select at least one user for domain workspace ' + (i + 1)
        );
        return;
      }
    }

    // Forwards the event to the event handler
    this.catchEvent({ type: 'save' });
  }
}
