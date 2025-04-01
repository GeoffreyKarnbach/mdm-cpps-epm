import { CppsProject } from './CppsProject';
import { ProjectDetails } from './ProjectDetails';

export interface CppsProjectListItem {
  id: number;
  details: ProjectDetails;
  userCount: number;
  workspaceCount: number;
  hasAdmin: boolean;
  demo: boolean;
}
