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
public class CppsProjectDto {
    Long id;
    ProjectDetailsDto details;
    List<ProjectUserDto> users;
    List<DomainWorkspaceDto> domainWorkspaces;
    CommonWorkspaceDto commonWorkspace;
    ExternalToolsDetailsDto externalToolsDetails;
}
