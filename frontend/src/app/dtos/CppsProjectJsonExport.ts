import { ExternalToolsDetails } from './ExternalToolsDetails';

export interface CppsProjectJsonExport {
  name: string;
  description: string;
  version: string;
  isDemo: boolean;

  commonWorkspace: WorkspaceJsonExport;
  domainWorkspace: WorkspaceJsonExport[];

  users: UserJsonExport[];

  externalToolsDetails: ExternalToolsDetails;
}

export interface WorkspaceJsonExport {
  name: string;
  users: string[];
  gitlabGroupId: number;
  gitlabReviewerGroupId: number;
}

export interface UserJsonExport {
  gitlabId: string;
  gitlabUsername: string;
  email: string;
  isAdmin: boolean;
  isReviewer: boolean;
}
