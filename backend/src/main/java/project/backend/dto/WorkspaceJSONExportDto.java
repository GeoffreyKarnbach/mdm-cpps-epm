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
public class WorkspaceJSONExportDto {
    String name;
    List<String> users;
    Long gitlabGroupId;
    Long gitlabReviewerGroupId;
}
