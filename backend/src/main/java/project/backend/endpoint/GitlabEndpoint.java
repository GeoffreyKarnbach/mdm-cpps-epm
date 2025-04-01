package project.backend.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import project.backend.dto.GitlabGroupListItemDto;
import project.backend.service.ExampleService;
import project.backend.service.GitlabService;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/gitlab")
@Slf4j
@RequiredArgsConstructor
public class GitlabEndpoint {

    private final GitlabService gitlabService;

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @GetMapping("/groups")
    @Operation(summary = "Get all groups available to logged in user", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public List<GitlabGroupListItemDto> getAllUserGroups(Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("GET /api/v1/example");
        return this.gitlabService.getAllUserGroups(principal);
    }
}