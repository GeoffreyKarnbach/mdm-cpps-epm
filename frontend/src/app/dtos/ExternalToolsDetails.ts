import { GitlabIssuePrTemplate } from './GitlabIssuePrTemplate';

export interface ExternalToolsDetails {
  rootGroupId: number;
  creatorGitlabUserId: number;
  gitlabProjectId: number;
  mergeRequestTemplates: GitlabIssuePrTemplate[];
  issueTemplates: GitlabIssuePrTemplate[];
  gitlabHasBeenGenerated: boolean;
}
