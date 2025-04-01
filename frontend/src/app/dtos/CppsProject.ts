import { ProjectDetails } from './ProjectDetails';
import { DomainWorkspace } from './DomainWorkspace';
import { CommonWorkspace } from './CommonWorkspace';
import { ProjectUser } from './ProjectUser';
import { ExternalToolsDetails } from './ExternalToolsDetails';

export interface CppsProject {
  id: number;
  details: ProjectDetails;
  users: ProjectUser[];
  domainWorkspaces: DomainWorkspace[];
  commonWorkspace: CommonWorkspace;
  externalToolsDetails: ExternalToolsDetails;
}
