package project.backend.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import project.backend.service.ExampleService;

import java.security.Principal;

@RestController
@RequestMapping(value = "/api/v1/example")
@Slf4j
@RequiredArgsConstructor
public class ExampleEndpoint {

    private final ExampleService exampleService;

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @GetMapping
    @Operation(summary = "Example endpoint", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public void example(Principal principal) {
        log.info("Principal: {}", principal.getName());
        log.info("GET /api/v1/example");
    }
}