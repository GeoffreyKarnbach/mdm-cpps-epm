import { Component } from '@angular/core';
import { CppsProject } from '../../../dtos/CppsProject';
import { DemoProject } from '../../../dtos/DemoProject';
import { ProjectService } from '../../../services/project.service';
import { RoutingService } from '../../../services/routing.service';

@Component({
  selector: 'app-create-demo-project',
  templateUrl: './create-demo-project.component.html',
  styleUrl: './create-demo-project.component.scss',
})
export class CreateDemoProjectComponent {
  constructor(
    private projectService: ProjectService,
    private routingService: RoutingService
  ) {}

  mySteps = [
    { name: 'Project Details' },
    { name: 'Users List' },
    { name: 'External Tools' },
    { name: 'Demo Project Data' },
  ];

  cppsProject: CppsProject = {
    id: -1,
    details: {
      name: '',
      description: '',
      version: '0.0.1',
      demo: true,
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

  catchEvent(event: any) {
    const domnainCount = event.domainCount;

    const demoProject: DemoProject = {
      cppsProject: this.cppsProject,
      domainCount: domnainCount,
    };

    this.projectService.createDemoProject(demoProject).subscribe(
      (response) => {
        this.routingService.navigateToComponent('project-view', {
          id: response.cppsProject.id.toString(),
          isAdmin: 'true',
        });
      },
      (error) => {
        console.error(error);
      }
    );
  }
}
