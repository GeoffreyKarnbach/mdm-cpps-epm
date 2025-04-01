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
public class CppsProjectJSONExportDto {
    String name;
    String description;
    String version;
    Boolean isDemo;

    WorkspaceJSONExportDto commonWorkspace;
    List<WorkspaceJSONExportDto> domainWorkspaces;

    List<UserJSONExportDto> users;

    ExternalToolsDetailsDto externalToolsDetails;
}