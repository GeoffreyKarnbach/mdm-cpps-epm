import { Component } from '@angular/core';
import { RoutingService } from '../../../services/routing.service';

@Component({
  selector: 'app-project-type-selection',
  templateUrl: './project-type-selection.component.html',
  styleUrl: './project-type-selection.component.scss',
})
export class ProjectTypeSelectionComponent {
  constructor(public routingService: RoutingService) {}

  createCppsProject(): void {
    this.routingService.navigateToComponent('create-edit-project', {
      mode: 'create',
    });
  }

  createDemoProject(): void {
    this.routingService.navigateToComponent('create-demo-project', {});
  }

  importProject(): void {
    this.routingService.navigateToComponent('import-project', {});
  }
}
