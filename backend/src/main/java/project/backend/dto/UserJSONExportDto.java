package project.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserJSONExportDto {
    String gitlabId;
    String gitlabUsername;
    String email;
    Boolean isAdmin;
    Boolean isReviewer;
}
