package project.backend.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.Column;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import project.backend.dto.UserDto;
import project.backend.exception.NotFoundException;
import project.backend.service.ExampleService;
import project.backend.service.UserService;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/user")
@Slf4j
@RequiredArgsConstructor
public class UserEndpoint {

    private final UserService userService;

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @GetMapping
    @Operation(summary = "Return the current user from whom the request comes from", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public UserDto getCurrentUser(Principal principal) {
        long id = Long.parseLong(principal.getName());

        return userService.get_user(id);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @GetMapping("/registration/{id}")
    @Operation(summary = "Potentially creates a user in the system if not defined", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public UserDto getUser(@PathVariable("id") Long gitlabId, Principal principal) {
        long id = Long.parseLong(principal.getName());
        return userService.register_or_return_gitlab_user(gitlabId, id);
    }

    @PreAuthorize("hasAuthority('SCOPE_read_user')")
    @GetMapping("/matchingUsers")
    @Operation(summary = "Get all users that match the search criteria", security = @SecurityRequirement(name = "bearerAuth"))
    @ResponseStatus(HttpStatus.OK)
    public List<UserDto> getMatchingUsers(@RequestParam("search") String usernameSubStr, Principal principal) {
        long id = Long.parseLong(principal.getName());

        /*
        List<UserDto> users = new ArrayList<>(List.of(
            UserDto.builder().gitlabUsername("GeoffreyKarnbachTUWien").gitlabId(22663746L).build(),
            UserDto.builder().gitlabUsername("GeoffreyKarnbachTUWien2").gitlabId(22778983L).build(),
            UserDto.builder().gitlabUsername("GeoffreyKarnbachTUWien3").gitlabId(22663820L).build(),
            UserDto.builder().gitlabUsername("GeoffreyKarnbachTUWien4").gitlabId(22663848L).build(),
            UserDto.builder().gitlabUsername("GeoffreyKarnbachTUWien5").gitlabId(22663855L).build()));

        // Filter now that at the usernameSubStr is contained in the gitlabUsername
        users.removeIf(user -> !user.getGitlabUsername().contains(usernameSubStr));
        log.info("User {} searched for users with the search string '{}'. Found {} users.", id, usernameSubStr, users.size());
         */

        return this.userService.getMatchingUsers(usernameSubStr, id);
    }
}