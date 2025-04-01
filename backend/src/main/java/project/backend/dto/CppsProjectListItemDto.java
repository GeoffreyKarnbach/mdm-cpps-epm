package project.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CppsProjectListItemDto {
    Long id;
    ProjectDetailsDto details;
    Long userCount;
    Long workspaceCount;
    boolean hasAdmin;
    boolean demo;
}
