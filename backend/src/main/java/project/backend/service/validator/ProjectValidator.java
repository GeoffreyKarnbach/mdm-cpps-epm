package project.backend.service.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import project.backend.dto.CppsProjectDto;
import project.backend.dto.ValidationErrorDto;
import project.backend.dto.ValidationErrorRestDto;
import project.backend.exception.ValidationException;
import project.backend.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProjectValidator {

    private final UserRepository userRepository;

    public void validateProject(CppsProjectDto projectDto) {
        log.info("Validating project for creation");
        List<String> validationErrors = new ArrayList<>();

        List<String> workspaceNames = new ArrayList<>();

        // Validate project details
        if (projectDto.getDetails() == null) {
            validationErrors.add("Project details are required");
        } else {
            if (projectDto.getDetails().getName() == null || projectDto.getDetails().getName().isEmpty()) {
                validationErrors.add("Project name is required");
            }

            if (projectDto.getDetails().getDescription() == null || projectDto.getDetails().getDescription().isEmpty()) {
                validationErrors.add("Project description is required");
            }

            if (projectDto.getDetails().getVersion() == null || projectDto.getDetails().getVersion().isEmpty()) {
                validationErrors.add("Project version is required");
            }
        }

        // Validate users
        if (projectDto.getUsers() == null || projectDto.getUsers().isEmpty()) {
            validationErrors.add("At least one user is required");
        } else {
            for (int i = 0; i < projectDto.getUsers().size(); i++) {
                Long userId = projectDto.getUsers().get(i).getUser().getId();
                if (userId == null) {
                    validationErrors.add("User ID is required");
                } else {
                    if (!userRepository.existsById(userId)) {
                        validationErrors.add("User with ID " + userId + " does not exist");
                    }
                }
            }
        }

        // Validate common workspace
        if (projectDto.getCommonWorkspace() == null) {
            validationErrors.add("Common workspace is required");
        } else {
            if (projectDto.getCommonWorkspace().getName() == null || projectDto.getCommonWorkspace().getName().isEmpty()) {
                validationErrors.add("Common workspace name is required");
            }
            if (projectDto.getCommonWorkspace().getUsers().isEmpty()) {
                validationErrors.add("At least one user is required for common workspace");
            } else {
                for (int i = 0; i < projectDto.getCommonWorkspace().getUsers().size(); i++) {
                    Long userId = projectDto.getCommonWorkspace().getUsers().get(i).getId();
                    if (userId == null) {
                        validationErrors.add("User ID is required");
                    } else {
                        if (!userRepository.existsById(userId)) {
                            validationErrors.add("User with ID " + userId + " does not exist");
                        }
                    }
                }
            }
            workspaceNames.add(projectDto.getCommonWorkspace().getName());
        }

        // Validate domain workspaces
        if (projectDto.getDomainWorkspaces() == null || projectDto.getDomainWorkspaces().isEmpty()) {
            validationErrors.add("At least one domain workspace is required");
        } else {
            for (int i = 0; i < projectDto.getDomainWorkspaces().size(); i++) {
                if (projectDto.getDomainWorkspaces().get(i).getName() == null || projectDto.getDomainWorkspaces().get(i).getName().isEmpty()) {
                    validationErrors.add("Domain workspace name is required");
                }
                if (projectDto.getDomainWorkspaces().get(i).getUsers().isEmpty()) {
                    validationErrors.add("At least one user is required for domain workspace");
                } else {
                    for (int j = 0; j < projectDto.getDomainWorkspaces().get(i).getUsers().size(); j++) {
                        Long userId = projectDto.getDomainWorkspaces().get(i).getUsers().get(j).getId();
                        if (userId == null) {
                            validationErrors.add("User ID is required");
                        } else {
                            if (!userRepository.existsById(userId)) {
                                validationErrors.add("User with ID " + userId + " does not exist");
                            }
                        }
                    }
                }
                workspaceNames.add(projectDto.getDomainWorkspaces().get(i).getName());
            }
        }

        // Validate workspace names
        for (int i = 0; i < workspaceNames.size(); i++) {
            for (int j = i + 1; j < workspaceNames.size(); j++) {
                if (workspaceNames.get(i).equals(workspaceNames.get(j))) {
                    validationErrors.add("Workspace name " + workspaceNames.get(i) + " is duplicated");
                }
            }
        }

        // Validate external tools data
        if (projectDto.getExternalToolsDetails() == null) {
            validationErrors.add("External tools data is required");
        } else {
            if (projectDto.getExternalToolsDetails().getRootGroupId() == null || projectDto.getExternalToolsDetails().getRootGroupId() <= 0) {
                validationErrors.add("Root group ID is required and must be greater than 0");
            }
        }

        // Throw validation exception if there are any validation errors
        List<ValidationErrorDto> validationErrorDtos = new ArrayList<>();

        for (int i = 0; i < validationErrors.size(); i++) {
            validationErrorDtos.add(new ValidationErrorDto((long) i, validationErrors.get(i), null));
        }

        if (validationErrors.size() > 0) {
            throw new ValidationException(new ValidationErrorRestDto("Validation error for project creation", validationErrorDtos));
        }
    }

    public void validateDemoProject(CppsProjectDto projectDto) {
        log.info("Validating project for creation");
        List<String> validationErrors = new ArrayList<>();

        List<String> workspaceNames = new ArrayList<>();

        // Validate project details
        if (projectDto.getDetails() == null) {
            validationErrors.add("Project details are required");
        } else {
            if (projectDto.getDetails().getName() == null || projectDto.getDetails().getName().isEmpty()) {
                validationErrors.add("Project name is required");
            }

            if (projectDto.getDetails().getDescription() == null || projectDto.getDetails().getDescription().isEmpty()) {
                validationErrors.add("Project description is required");
            }

            if (projectDto.getDetails().getVersion() == null || projectDto.getDetails().getVersion().isEmpty()) {
                validationErrors.add("Project version is required");
            }
        }

        // Validate users
        if (projectDto.getUsers() == null || projectDto.getUsers().isEmpty()) {
            validationErrors.add("At least one user is required");
        } else {
            for (int i = 0; i < projectDto.getUsers().size(); i++) {
                Long userId = projectDto.getUsers().get(i).getUser().getId();
                if (userId == null) {
                    validationErrors.add("User ID is required");
                } else {
                    if (!userRepository.existsById(userId)) {
                        validationErrors.add("User with ID " + userId + " does not exist");
                    }
                }
            }
        }

        // Validate external tools data
        if (projectDto.getExternalToolsDetails() == null) {
            validationErrors.add("External tools data is required");
        } else {
            if (projectDto.getExternalToolsDetails().getRootGroupId() == null || projectDto.getExternalToolsDetails().getRootGroupId() <= 0) {
                validationErrors.add("Root group ID is required and must be greater than 0");
            }
        }

        // Throw validation exception if there are any validation errors
        List<ValidationErrorDto> validationErrorDtos = new ArrayList<>();

        for (int i = 0; i < validationErrors.size(); i++) {
            validationErrorDtos.add(new ValidationErrorDto((long) i, validationErrors.get(i), null));
        }

        if (validationErrors.size() > 0) {
            throw new ValidationException(new ValidationErrorRestDto("Validation error for project creation", validationErrorDtos));
        }
    }
}
