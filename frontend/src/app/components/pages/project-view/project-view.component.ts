import { Component, Input } from '@angular/core';
import { QueryParams } from '../../../dtos/NavigationItem';
import { FeedbackService } from '../../../services/feedback.service';
import { ProjectService } from '../../../services/project.service';
import { UserService } from '../../../services/user.service';
import { RoutingService } from '../../../services/routing.service';
import { CppsProject } from '../../../dtos/CppsProject';
import { catchError, concatMap, delay, from, of, tap } from 'rxjs';

type ProjectBuildStep = {
  name: string;
  inProgress: boolean;
  completed: boolean;
  failure: boolean;
};

@Component({
  selector: 'app-project-view',
  templateUrl: './project-view.component.html',
  styleUrl: './project-view.component.scss',
})
export class ProjectViewComponent {
  @Input() public queryParams: QueryParams = {};

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
      mergeRequestTemplates: [],
      issueTemplates: [],
      gitlabHasBeenGenerated: false,
    },
  };

  cppsProjectId: number = -1;
  downloadUrlInfo: string = '';
  isAdminViewing: boolean = false;
  isOwnerViewing: boolean = false;
  loading: boolean = false;
  loadingSpinner: boolean = false;

  buildSteps: ProjectBuildStep[] = [];

  currentBuildStep: number = 0;
  buildCompleted: boolean = false;
  showBuildProgress: boolean = false;

  projectResetInProgress: boolean = false;
  projectResetCompleted: boolean = false;
  projectResetFailure: boolean = false;

  gitlabUrl: string = '';

  showMR: boolean = false;
  showIssue: boolean = false;

  constructor(
    public routingService: RoutingService,
    public userService: UserService,
    private projectService: ProjectService,
    private feedbackService: FeedbackService
  ) {}

  ngOnInit(): void {
    if (this.queryParams['id']) {
      this.cppsProjectId = parseInt(this.queryParams['id']);

      this.projectService.getGitlabUrl(this.cppsProjectId).subscribe((url) => {
        this.gitlabUrl = url;
        console.log('Gitlab URL loaded successfully', url);
      });

      this.projectService.getProjectById(this.cppsProjectId).subscribe(
        (project) => {
          this.cppsProject = project;
          console.log('Project loaded successfully', project);
          this.userService.getCurrentUser().subscribe((user) => {
            this.isOwnerViewing =
              user.gitlabId ==
              this.cppsProject.externalToolsDetails.creatorGitlabUserId;
          });
        },
        (error) => {
          this.feedbackService.showError('Failed to load project', error);
        }
      );
    }

    if (this.queryParams['isAdmin']) {
      this.isAdminViewing = this.queryParams['isAdmin'] == 'true';
    }
  }

  copyJSONExportURL(): void {
    navigator.clipboard.writeText(
      this.projectService.exportProjectUrl(this.cppsProjectId)
    );
    this.feedbackService.showSuccess(
      'Download url copied to clipboard, please paste it in your browser!'
    );
  }

  copyAnonymizedJSONExportURL(): void {
    navigator.clipboard.writeText(
      this.projectService.exportAnonymizedProjectUrl(this.cppsProjectId)
    );
    this.feedbackService.showSuccess(
      'Download url copied to clipboard, please paste it in your browser!'
    );
  }

  editProject(): void {
    this.routingService.navigateToComponent('create-edit-project', {
      mode: 'edit',
      id: this.cppsProjectId.toString(),
    });
  }

  copyGitlabUrlToClipboard(): void {
    navigator.clipboard.writeText(this.gitlabUrl);
    this.feedbackService.showSuccess('Gitlab URL copied to clipboard');
  }

  editProjectInfrastructure(): void {
    this.buildSteps = [
      {
        name: 'Creating GitLab Subgroups',
        inProgress: false,
        completed: false,
        failure: false,
      },
      {
        name: 'Adding / Removing users to GitLab Project',
        inProgress: false,
        completed: false,
        failure: false,
      },
      {
        name: 'Adding / Removing Users to GitLab Subgroups',
        inProgress: false,
        completed: false,
        failure: false,
      },
      {
        name: 'Update and Upload Repository File Structure to GitLab',
        inProgress: false,
        completed: false,
        failure: false,
      },
    ];

    this.currentBuildStep = 0;
    this.buildCompleted = false;

    this.projectResetCompleted = false;
    this.projectResetFailure = false;
    this.projectResetInProgress = false;

    this.showBuildProgress = true;
    this.loading = true;

    const buildSteps$ = [
      this.projectService.editProjectSubgroups(this.cppsProjectId),
      this.projectService.editProjectUsers(this.cppsProjectId),
      this.projectService.editSubgroupUsers(this.cppsProjectId),
      this.projectService.editRepositoryFiles(this.cppsProjectId),
    ];

    from(buildSteps$)
      .pipe(
        concatMap((step$, index) =>
          of(null).pipe(
            tap(() => {
              this.buildSteps[index].inProgress = true;
            }),
            concatMap(() => step$),
            delay(100),
            tap((response) => {
              this.buildSteps[index].inProgress = false;
              if (!response.success) {
                this.buildSteps[index].failure = true;
              } else {
                this.buildSteps[index].completed = true;
              }
              this.currentBuildStep += 1;
              if (index + 1 < this.buildSteps.length) {
                this.buildSteps[index + 1].inProgress = true;
              }
            }),
            catchError((error) => {
              this.buildSteps[index].inProgress = false;
              this.buildSteps[index].failure = true;
              this.currentBuildStep += 1;
              return of(null);
            })
          )
        )
      )
      .subscribe({
        complete: () => {
          if (this.buildSteps.every((step) => step.completed)) {
            this.buildCompleted = true;
            this.loading = false;
            this.feedbackService.showSuccess('Infrastructure has been built');
          } else {
            this.loading = false;
            this.feedbackService.showError('Failed to build infrastructure');
          }
        },
      });
  }

  resetAndBuildProjectInfrastructure(): void {
    this.buildSteps = [
      {
        name: 'Creating GitLab Project',
        inProgress: false,
        completed: false,
        failure: false,
      },
      {
        name: 'Creating GitLab Subgroups',
        inProgress: false,
        completed: false,
        failure: false,
      },
      {
        name: 'Adding Users to GitLab Project',
        inProgress: false,
        completed: false,
        failure: false,
      },
      {
        name: 'Adding Users to GitLab Subgroups',
        inProgress: false,
        completed: false,
        failure: false,
      },
      {
        name: 'Add Deployment Key to GitLab Project',
        inProgress: false,
        completed: false,
        failure: false,
      },
      {
        name: 'Generate and Upload Repository File Structure to GitLab',
        inProgress: false,
        completed: false,
        failure: false,
      },
      {
        name: 'Generate GitLab Labels',
        inProgress: false,
        completed: false,
        failure: false,
      },
    ];

    this.currentBuildStep = 0;
    this.buildCompleted = false;

    this.projectResetCompleted = false;
    this.projectResetFailure = false;

    this.projectResetInProgress = true;
    this.showBuildProgress = true;
    this.loading = true;

    this.projectService.resetGitlabRootGroup(this.cppsProjectId).subscribe(
      (response) => {
        if (response.success) {
          this.projectResetInProgress = false;
          this.projectResetCompleted = true;
          this.feedbackService.showSuccess(
            'Root group has been reset, project building will start shortly',
            3000
          );
          setTimeout(() => {
            this.buildProjectInfrastructure();
          }, 3000);
        } else {
          this.projectResetInProgress = false;
          this.projectResetFailure = true;
          this.loading = false;
          this.feedbackService.showError('Failed to reset root group');
        }
      },
      (error) => {
        this.projectResetInProgress = false;
        this.projectResetFailure = true;
        this.loading = false;
        this.feedbackService.showError('Failed to reset root group', error);
      }
    );
  }

  buildProjectInfrastructure(): void {
    const buildSteps$ = [
      this.projectService.buildProjectInfrastructure(this.cppsProjectId),
      this.projectService.buildProjectSubgroups(this.cppsProjectId),
      this.projectService.addProjectUsers(this.cppsProjectId),
      this.projectService.addSubgroupUsers(this.cppsProjectId),
      this.projectService.addDeployKey(this.cppsProjectId),
      this.projectService.generateRepositoryFiles(this.cppsProjectId),
      this.projectService.generateGitlabLabels(this.cppsProjectId),
    ];

    from(buildSteps$)
      .pipe(
        concatMap((step$, index) =>
          of(null).pipe(
            tap(() => {
              this.buildSteps[index].inProgress = true;
            }),
            concatMap(() => step$),
            delay(100),
            tap((response) => {
              this.buildSteps[index].inProgress = false;
              if (!response.success) {
                this.buildSteps[index].failure = true;
              } else {
                this.buildSteps[index].completed = true;
              }
              this.currentBuildStep += 1;
              if (index + 1 < this.buildSteps.length) {
                this.buildSteps[index + 1].inProgress = true;
              }
            }),
            catchError((error) => {
              this.buildSteps[index].inProgress = false;
              this.buildSteps[index].failure = true;
              this.currentBuildStep += 1;
              return of(null);
            })
          )
        )
      )
      .subscribe({
        complete: () => {
          if (this.buildSteps.every((step) => step.completed)) {
            this.buildCompleted = true;
            this.loading = false;
            this.feedbackService.showSuccess('Infrastructure has been built');
            this.cppsProject.externalToolsDetails.gitlabHasBeenGenerated = true;
          } else {
            this.loading = false;
            this.feedbackService.showError('Failed to build infrastructure');
          }
        },
      });
  }

  toggleMR(): void {
    this.showMR = !this.showMR;
  }

  toggleIssue(): void {
    this.showIssue = !this.showIssue;
  }

  performFileConsistencyCheck(): void {
    this.loading = true;
    this.loadingSpinner = true;
    this.projectService
      .performFileConsistencyCheck(this.cppsProjectId)
      .subscribe(
        (response) => {
          this.loading = false;
          this.loadingSpinner = false;
          if (response.success) {
            this.feedbackService.showSuccess(
              'File consistency check successful',
              -1
            );
          } else {
            this.feedbackService.showError(
              'File consistency check failed <br>' + response.message,
              -1
            );
          }
        },
        (error) => {
          this.loading = false;
          this.loadingSpinner = false;
          this.feedbackService.showError(
            'Failed to perform file consistency check'
          );
        }
      );
  }
}
