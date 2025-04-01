package project.backend.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.backend.dto.*;
import project.backend.exception.AccessForbiddenException;
import project.backend.exception.NotFoundException;
import project.backend.service.ExampleService;
import project.backend.service.ProjectService;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/project")
@Slf4j
@RequiredArgsConstructor
public class ProjectEndpoint {

    private final ProjectService projectService;

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping
    @Operation(summary = "Create CPPS Project", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public CppsProjectDto createProject(@Valid @RequestBody CppsProjectDto cppsProjectDto, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project");
        log.info("ProjectDetailsDto: {}", cppsProjectDto);
        return this.projectService.createProject(cppsProjectDto, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @GetMapping
    @Operation(summary = "Get all project the current logged in user has access to", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public List<CppsProjectListItemDto> getProjects(Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("GET /api/v1/project");
        return this.projectService.getProjects(principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @GetMapping("/{projectId}")
    @Operation(summary = "Get project by id", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public CppsProjectDto getProject(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("GET /api/v1/project/{}", projectId);
        return this.projectService.getProject(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @GetMapping("/{projectId}/gitlab_url")
    @Operation(summary = "Get Gitlab URL for given project ID", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public String getGitlabUrl(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("GET /api/v1/project/{}/gitlab_url", projectId);
        return this.projectService.getGitlabUrl(projectId, principal);
    }

    @GetMapping("/{projectId}/export")
    @Operation(summary = "Export project by id")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> exportProject(@PathVariable Long projectId, @RequestParam String jwt) {
        log.info("GET /api/v1/project/{}/export", projectId);

        CppsProjectJSONExportDto project;

        try {
            project = this.projectService.exportProject(projectId, jwt);
        } catch (AccessForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            // Convert DTO to a formatted JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            String formattedJson = objectMapper.writeValueAsString(project);

            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + project.getName() + ".json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(formattedJson);
        } catch (Exception e) {
            log.error("Error while exporting project JSON", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{projectId}/export/anonymized")
    @Operation(summary = "Export anonymized project by id")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> exportAnonymizedProject(@PathVariable Long projectId, @RequestParam String jwt) {
        log.info("GET /api/v1/project/{}/exportAnonymized", projectId);

        // TODO: Implement anonymization
        CppsProjectJSONExportDto project;

        try {
            project = this.projectService.exportAnonymizedProject(projectId, jwt);
        } catch (AccessForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        try {
            // Convert DTO to a formatted JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            String formattedJson = objectMapper.writeValueAsString(project);

            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + project.getName() + ".json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(formattedJson);
        } catch (Exception e) {
            log.error("Error while exporting project JSON", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/import")
    @Operation(summary = "Import project by using JSON file")
    @ResponseStatus(HttpStatus.OK)
    public CppsProjectDto importProject(@Valid @RequestBody CppsProjectJSONExportDto cppsProjectDto, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/import");
        return this.projectService.importProject(cppsProjectDto, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @DeleteMapping("/{projectId}")
    @Operation(summary = "Deletes a project by id", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void deleteProject(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("DELETE /api/v1/project/{}", projectId);
        this.projectService.deleteProject(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PutMapping("/{projectId}")
    @Operation(summary = "Update a project by id", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public CppsProjectDto updateProject(@PathVariable Long projectId, @Valid @RequestBody CppsProjectDto cppsProjectDto, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("PUT /api/v1/project/{}", projectId);
        log.info("ProjectDetailsDto: {}", cppsProjectDto);
        return this.projectService.updateProject(projectId, cppsProjectDto, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/{projectId}/build/reset_gitlab")
    @Operation(summary = "Resets the Gitlab Root Group for the given project", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public BuildResponseDto resetGitlabProject(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/{}/build/reset_gitlab", projectId);
        return this.projectService.resetGitlabProject(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/{projectId}/build/project")
    @Operation(summary = "Build Gitlab project)", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public BuildResponseDto buildGitlabProject(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/{}/build/project", projectId);
        return this.projectService.buildGitlabProject(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/{projectId}/build/subgroups")
    @Operation(summary = "Build Gitlab subgroups", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public BuildResponseDto buildGitlabSubgroups(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/{}/build/subgroups", projectId);
        return this.projectService.buildGitlabSubgroups(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/{projectId}/build/project_users")
    @Operation(summary = "Add Gitlab project users", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public BuildResponseDto addGitlabProjectUsers(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/{}/build/project_users", projectId);

        return this.projectService.addGitlabProjectUsers(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/{projectId}/build/subgroup_users")
    @Operation(summary = "Add Gitlab subgroups users", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public BuildResponseDto addGitlabSubgroupsUsers(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/{}/build/subgroups_users", projectId);

        return this.projectService.addGitlabSubgroupsUsers(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/{projectId}/build/deploy_key")
    @Operation(summary = "Add Gitlab deploy key to project", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public BuildResponseDto addGitlabDeployKey(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/{}/build/deploy_key", projectId);

        return this.projectService.addGitlabDeployKey(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/{projectId}/build/repository_files")
    @Operation(summary = "Generate and upload repository files", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public BuildResponseDto generateAndUploadRepositoryFiles(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/{}/build/repository_files", projectId);

        return this.projectService.generateAndUploadRepositoryFiles(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/{projectId}/build/labels")
    @Operation(summary = "Generate GitLab project labels", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public BuildResponseDto generateGitlabProjectLabels(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/{}/build/labels", projectId);

        return this.projectService.generateGitlabProjectLabels(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/{projectId}/edit/subgroups")
    @Operation(summary = "Edit GitLab subgroups", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public BuildResponseDto editGitlabSubgroups(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/{}/edit/subgroups", projectId);

        return this.projectService.editGitlabSubgroups(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/{projectId}/edit/project_users")
    @Operation(summary = "Edit GitLab project users", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public BuildResponseDto editGitlabProjectUsers(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/{}/edit/project_users", projectId);

        return this.projectService.editGitlabProjectUsers(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/{projectId}/edit/subgroup_users")
    @Operation(summary = "Edit GitLab subgroup users", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public BuildResponseDto editGitlabSubgroupUsers(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/{}/edit/subgroup_users", projectId);

        return this.projectService.editGitlabSubgroupsUsers(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/{projectId}/edit/repository_files")
    @Operation(summary = "Edit GitLab repository files", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public BuildResponseDto editGitlabRepositoryFiles(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/{}/edit/repository_files", projectId);

        return this.projectService.updateAndUploadRepositoryFiles(projectId, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/demo")
    @Operation(summary = "Create CPPS Project", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public DemoProjectDto createDemoProject(@Valid @RequestBody DemoProjectDto demoProjectDto, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project");
        log.info("ProjectDetailsDto: {}", demoProjectDto);
        return this.projectService.createDemoProject(demoProjectDto, principal);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @PostMapping("/{projectId}/file_consistency_check")
    @Operation(summary = "Perform file consistency check", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public BuildResponseDto performFileConsistencyCheck(@PathVariable Long projectId, Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("POST /api/v1/project/{}/file_consistency_check", projectId);
        return this.projectService.performFileConsistencyCheck(projectId, principal);
    }


}