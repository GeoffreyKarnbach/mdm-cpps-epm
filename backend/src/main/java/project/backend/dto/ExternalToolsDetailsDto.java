package project.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class    ExternalToolsDetailsDto {
    Long rootGroupId;
    Long creatorGitlabUserId;
    Long gitlabProjectId;
    List<GitlabIssuePrTemplateDto> mergeRequestTemplates;
    List<GitlabIssuePrTemplateDto> issueTemplates;
    Boolean gitlabHasBeenGenerated;
}
