package project.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDetailsDto {

    @NotBlank
    String name;

    @NotBlank
    String description;

    @NotBlank
    String version;

    @NotBlank
    Boolean demo;
}
