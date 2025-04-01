package project.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUserDto {

    UserDto user;

    @JsonProperty("isReviewer")
    boolean isReviewer;

    @JsonProperty("isAdmin")
    boolean isAdmin;
}
