import { Component } from '@angular/core';
import { CppsProjectJsonExport } from '../../../dtos/CppsProjectJsonExport';
import { ProjectService } from '../../../services/project.service';
import { RoutingService } from '../../../services/routing.service';

@Component({
  selector: 'app-import-project',
  templateUrl: './import-project.component.html',
  styleUrl: './import-project.component.scss',
})
export class ImportProjectComponent {
  constructor(
    private projectService: ProjectService,
    private routingService: RoutingService
  ) {}

  public textContent = '';

  public importProject(): void {
    try {
      const cppsImportProject: CppsProjectJsonExport = JSON.parse(
        this.textContent
      );
      this.projectService.importProject(cppsImportProject).subscribe(
        (project) => {
          console.log('Project imported:', project);
          this.routingService.navigateToComponent('project-view', {
            id: project.id.toString(),
            isAdmin: 'true',
          });
        },
        (error) => {
          console.error('Error importing project:', error);
        }
      );
    } catch (error) {
      console.error('Invalid JSON input:', error);
    }
  }
}
