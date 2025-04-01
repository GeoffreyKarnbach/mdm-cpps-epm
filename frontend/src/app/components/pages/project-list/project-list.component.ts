import { Component, Input } from '@angular/core';
import { QueryParams } from '../../../dtos/NavigationItem';
import { RoutingService } from '../../../services/routing.service';
import { UserService } from '../../../services/user.service';
import { ProjectService } from '../../../services/project.service';
import { CppsProjectListItem } from '../../../dtos/CppsProjectListItem';
import { FeedbackService } from '../../../services/feedback.service';

@Component({
  selector: 'app-project-list',
  templateUrl: './project-list.component.html',
  styleUrl: './project-list.component.scss',
})
export class ProjectListComponent {
  @Input() public queryParams: QueryParams = {};
  public projects: CppsProjectListItem[] = [];

  constructor(
    public routingService: RoutingService,
    public userService: UserService,
    private projectService: ProjectService,
    private feedbackService: FeedbackService
  ) {}

  ngOnInit(): void {
    this.projectService.getProjectsForUser().subscribe(
      (projects) => {
        this.projects = projects;
        console.log('Projects loaded', projects);
      },
      (error) => {
        this.feedbackService.showError('Failed to load projects', error);
      }
    );
  }

  addProject(): void {
    this.routingService.navigateToComponent('project-type-selection', {});
  }

  viewProject(id: number): void {
    this.routingService.navigateToComponent('project-view', {
      id: id.toString(),
      isAdmin: String(
        this.projects.find((project) => project.id === id)?.hasAdmin
      ),
    });
  }

  editProject(id: number): void {
    this.routingService.navigateToComponent('create-edit-project', {
      mode: 'edit',
      id: id.toString(),
    });
  }

  deleteProject(id: number): void {
    this.projectService.deleteProject(id).subscribe(
      () => {
        this.projects = this.projects.filter((project) => project.id !== id);
      },
      (error) => {
        this.feedbackService.showError('Failed to delete project', error);
      }
    );
  }

  get demoProjects(): CppsProjectListItem[] {
    return this.projects.filter((project) => project.demo);
  }

  get mdCppsProjects(): CppsProjectListItem[] {
    return this.projects.filter((project) => !project.demo);
  }
}
