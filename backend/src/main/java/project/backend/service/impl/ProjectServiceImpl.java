package project.backend.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import project.backend.dto.*;
import project.backend.entity.*;
import project.backend.exception.AccessForbiddenException;
import project.backend.exception.NotFoundException;
import project.backend.mapper.ProjectMapper;
import project.backend.repository.*;
import project.backend.service.ExampleService;
import project.backend.service.FileGenerationService;
import project.backend.service.GitlabService;
import project.backend.service.ProjectService;
import project.backend.service.validator.ProjectValidator;

import java.io.File;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectValidator projectValidator;
    private final ProjectMapper projectMapper;
    private final ProjectUserAccessRepository projectUserAccessRepository;
    private final ExternalToolsDataRepository externalToolsDataRepository;
    private final GitlabService gitlabService;
    private final FileGenerationService fileGenerationService;

    private final String secretKey = "SECretKEySECretKEySECretKEySECretKEySECretKEySECretKEySECretKEySECretKEy";  // Same as used in token generation

    @Override
    public CppsProjectDto createProject(CppsProjectDto projectDto, Principal principal) {
        this.projectValidator.validateProject(projectDto);

        Project project = this.projectMapper.mapDtoToEntityForCreationAndPersist(projectDto);
        log.info("Project created: {}", project);

        ProjectUserAccess principalAccess = project.getProjectUserAccesses().stream()
                .filter(access -> access.getUser().getId().equals(Long.parseLong(principal.getName())))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Principal not found in project access list"));

        principalAccess.setIsAdmin(true);

        this.projectUserAccessRepository.save(principalAccess);

        ExternalToolsData externalToolsData = project.getExternalToolsData();
        externalToolsData.setCreatorUserId(principalAccess.getUser().getGitlabId());

        this.externalToolsDataRepository.save(externalToolsData);

        projectDto.setId(project.getId());

        return projectDto;
    }

    @Override
    public DemoProjectDto createDemoProject(DemoProjectDto demoProjectDto, Principal principal) {

        this.projectValidator.validateDemoProject(demoProjectDto.getCppsProject());

        Long principalId = Long.parseLong(principal.getName());

        Project project = this.projectMapper.mapDtoToEntityForCreationAndPersistDemoProject(demoProjectDto, principalId);
        log.info("Project created: {}", project);

        ProjectUserAccess principalAccess = project.getProjectUserAccesses().stream()
            .filter(access -> access.getUser().getId().equals(Long.parseLong(principal.getName())))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Principal not found in project access list"));

        principalAccess.setIsAdmin(true);

        this.projectUserAccessRepository.save(principalAccess);

        ExternalToolsData externalToolsData = project.getExternalToolsData();
        externalToolsData.setCreatorUserId(principalAccess.getUser().getGitlabId());

        this.externalToolsDataRepository.save(externalToolsData);

        DemoProjectDto response = new DemoProjectDto();
        response.setCppsProject(projectMapper.mapEntityToProjectDto(project));
        response.setDomainCount(demoProjectDto.getDomainCount());

        return response;
    }

    @Override
    public List<CppsProjectListItemDto> getProjects(Principal principal) {

        List<ProjectUserAccess> projectUserAccesses =
            this.projectUserAccessRepository.findByUserId(Long.parseLong(principal.getName()));

        if (projectUserAccesses != null) {
            List<CppsProjectListItemDto> projects = new ArrayList<>();

            for (ProjectUserAccess projectUserAccess : projectUserAccesses) {
                projects.add(
                    this.projectMapper.mapProjectUserAccessEntityToProjectListItemDto(projectUserAccess)
                );
            }

            return projects;
        }

        return List.of();
    }

    @Override
    public CppsProjectDto getProject(Long projectId, Principal principal) {

        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        if (project.getProjectUserAccesses().stream()
            .noneMatch(access -> access.getUser().getId().equals(Long.parseLong(principal.getName())))) {
            throw new NotFoundException("Project not found");
        }

        return this.projectMapper.mapEntityToProjectDto(project);
    }

    @Override
    public CppsProjectJSONExportDto exportProject(Long projectId, String jwt) {

        String userId;

        try {
            Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(jwt.replace("Bearer ", ""))
                .getBody();

            userId = claims.getSubject();
        } catch (Exception e) {
            throw new AccessForbiddenException("Access to project denied, invalid token");
        }

        log.info("Exporting project for user: {}", userId);

        boolean hasAccess = this.projectUserAccessRepository.hasAccessToProject(Long.parseLong(userId), projectId);
        if (!hasAccess) {
            throw new AccessForbiddenException("Access to project denied, user does not have access");
        }

        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        return this.projectMapper.mapProjectEntityToCppsProjectJSONExportDto(project);
    }

    @Override
    public CppsProjectJSONExportDto exportAnonymizedProject(Long projectId, String jwt) {

        String userId;

        try {
            Claims claims = Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(jwt.replace("Bearer ", ""))
                .getBody();

            userId = claims.getSubject();
        } catch (Exception e) {
            throw new AccessForbiddenException("Access to project denied, invalid token");
        }

        log.info("Exporting anonymized project for user: {}", userId);

        boolean hasAccess = this.projectUserAccessRepository.hasAccessToProject(Long.parseLong(userId), projectId);
        if (!hasAccess) {
            throw new AccessForbiddenException("Access to project denied, user does not have access");
        }

        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        return this.projectMapper.mapProjectEntityToCppsProjectJSONExportDtoAnonymized(project);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId, Principal principal) {

        this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        boolean hasAccess = this.projectUserAccessRepository.hasAdminAccessToProject(Long.parseLong(principal.getName()), projectId);
        if (!hasAccess) {
            throw new AccessForbiddenException("Access to project denied, user does not have access");
        }

        this.projectRepository.deleteById(projectId);

    }

    @Override
    public CppsProjectDto updateProject(Long projectId, CppsProjectDto projectDto, Principal principal) {

        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        boolean hasAccess = this.projectUserAccessRepository.hasAdminAccessToProject(Long.parseLong(principal.getName()), projectId);
        if (!hasAccess) {
            throw new AccessForbiddenException("Access to project denied, user cannot update project");
        }

        this.projectValidator.validateProject(projectDto);

        Project updatedProject = this.projectMapper.mapDtoToEntityForUpdateAndPersist(projectDto, project);

        return this.projectMapper.mapEntityToProjectDto(updatedProject);
    }

    @Override
    public BuildResponseDto buildGitlabProject(Long projectId, Principal principal) {
        this.hasBuildAccess(projectId, principal);

        long userId = Long.parseLong(principal.getName());

        // Create Project
        if (!gitlabService.createGitlabProject(projectId, userId)) {
            return BuildResponseDto.builder()
                .success(false)
                .message("Gitlab project creation failed")
                .build();
        }

        return BuildResponseDto.builder()
            .success(true)
            .message("Gitlab project created successfully")
            .build();
    }

    @Override
    public BuildResponseDto buildGitlabSubgroups(Long projectId, Principal principal) {
        this.hasBuildAccess(projectId, principal);

        long userId = Long.parseLong(principal.getName());

        // Create Subgroups
        if (!gitlabService.createGitlabSubgroups(projectId, userId)) {
            return BuildResponseDto.builder()
                .success(false)
                .message("Gitlab subgroups creation failed")
                .build();
        }

        return BuildResponseDto.builder()
            .success(true)
            .message("Gitlab subgroups created successfully")
            .build();
    }

    @Override
    public BuildResponseDto addGitlabProjectUsers(Long projectId, Principal principal) {
        this.hasBuildAccess(projectId, principal);

        long userId = Long.parseLong(principal.getName());

        // Add Project Users
        if (!gitlabService.addGitlabProjectUsers(projectId, userId)) {
            return BuildResponseDto.builder()
                .success(false)
                .message("Gitlab project users addition failed")
                .build();
        }

        return BuildResponseDto.builder()
            .success(true)
            .message("Gitlab project users added successfully")
            .build();
    }

    @Override
    public BuildResponseDto addGitlabSubgroupsUsers(Long projectId, Principal principal) {
        this.hasBuildAccess(projectId, principal);

        long userId = Long.parseLong(principal.getName());

        // Add Subgroups Users
        if (!gitlabService.addGitlabSubgroupsUsers(projectId, userId)) {
            return BuildResponseDto.builder()
                .success(false)
                .message("Gitlab subgroups users addition failed")
                .build();
        }

        return BuildResponseDto.builder()
            .success(true)
            .message("Gitlab subgroups users added successfully")
            .build();
    }

    @Override
    public BuildResponseDto addGitlabDeployKey(Long projectId, Principal principal) {
        this.hasBuildAccess(projectId, principal);

        long userId = Long.parseLong(principal.getName());

        // Add Deploy Key
        if (!gitlabService.addGitlabDeployKey(projectId, userId)) {
            return BuildResponseDto.builder()
                .success(false)
                .message("Gitlab deploy key addition failed")
                .build();
        }

        return BuildResponseDto.builder()
            .success(true)
            .message("Gitlab deploy key added successfully")
            .build();
    }

    @Override
    public BuildResponseDto resetGitlabProject(Long projectId, Principal principal) {

        this.hasBuildAccess(projectId, principal);

        long userId = Long.parseLong(principal.getName());

        // Reset Project
        if (!gitlabService.resetGitlabProject(projectId, userId)) {
            return BuildResponseDto.builder()
                .success(false)
                .message("Gitlab project reset failed")
                .build();
        }

        return BuildResponseDto.builder()
            .success(true)
            .message("Gitlab project reset successfully")
            .build();
    }

    @Override
    public BuildResponseDto generateAndUploadRepositoryFiles(Long projectId, Principal principal) {
        this.hasBuildAccess(projectId, principal);

        long userId = Long.parseLong(principal.getName());

        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        String tempDirectoryId = this.fileGenerationService.generateFileStructure(project);

        log.info("Temp directory created: {}", tempDirectoryId);

        if (!gitlabService.uploadRepositoryFiles(projectId, tempDirectoryId, userId)) {
            return BuildResponseDto.builder()
                .success(false)
                .message("Gitlab repository files generation and upload failed")
                .build();
        }

        return BuildResponseDto.builder()
            .success(true)
            .message("Gitlab repository files generated and uploaded successfully")
            .build();
    }

    @Override
    public BuildResponseDto generateGitlabProjectLabels(Long projectId, Principal principal) {
        this.hasBuildAccess(projectId, principal);

        long userId = Long.parseLong(principal.getName());

        // Generate Project Labels
        if (!gitlabService.createGitlabProjectLabels(projectId, userId)) {
            return BuildResponseDto.builder()
                .success(false)
                .message("Gitlab project labels generation failed")
                .build();
        }

        return BuildResponseDto.builder()
            .success(true)
            .message("Gitlab project labels generated successfully")
            .build();
    }

    @Override
    public BuildResponseDto editGitlabSubgroups(Long projectId, Principal principal) {
        this.hasBuildAccess(projectId, principal);

        long userId = Long.parseLong(principal.getName());

        // Edit Subgroups
        if (!gitlabService.createOnlyNewGitlabSubgroups(projectId, userId)) {
            return BuildResponseDto.builder()
                .success(false)
                .message("Gitlab subgroups edit failed")
                .build();
        }

        return BuildResponseDto.builder()
            .success(true)
            .message("Gitlab subgroups edited successfully")
            .build();
    }

    @Override
    public BuildResponseDto editGitlabProjectUsers(Long projectId, Principal principal) {
        this.hasBuildAccess(projectId, principal);

        long userId = Long.parseLong(principal.getName());

        // Edit Project Users
        if (!gitlabService.editGitlabProjectUsers(projectId, userId)) {
            return BuildResponseDto.builder()
                .success(false)
                .message("Gitlab project users edit failed")
                .build();
        }

        return BuildResponseDto.builder()
            .success(true)
            .message("Gitlab project users edited successfully")
            .build();
    }

    @Override
    public BuildResponseDto editGitlabSubgroupsUsers(Long projectId, Principal principal) {
        this.hasBuildAccess(projectId, principal);

        long userId = Long.parseLong(principal.getName());

        // Edit Subgroups Users
        if (!gitlabService.editGitlabSubgroupsUsers(projectId, userId)) {
            return BuildResponseDto.builder()
                .success(false)
                .message("Gitlab subgroups users edit failed")
                .build();
        }

        return BuildResponseDto.builder()
            .success(true)
            .message("Gitlab subgroups users edited successfully")
            .build();
    }

    @Override
    public BuildResponseDto updateAndUploadRepositoryFiles(Long projectId, Principal principal) {
        this.hasBuildAccess(projectId, principal);

        long userId = Long.parseLong(principal.getName());

        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        String tempDirectoryId = this.fileGenerationService.updateFileStructure(project, userId);

        log.info("Temp directory created: {}", tempDirectoryId);

        if (!gitlabService.updateRepositoryFiles(projectId, tempDirectoryId, userId)) {
            return BuildResponseDto.builder()
                .success(false)
                .message("Gitlab repository files update and upload failed")
                .build();
        }

        return BuildResponseDto.builder()
            .success(true)
            .message("Gitlab repository files updated and uploaded successfully")
            .build();
    }

    @Override
    public String getGitlabUrl(Long projectId, Principal principal) {

        long userId = Long.parseLong(principal.getName());

        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        Long rootGroupId = project.getExternalToolsData().getRootGroupId();

        return this.gitlabService.getGitlabUrl(rootGroupId, userId);
    }

    @Override
    public CppsProjectDto importProject(CppsProjectJSONExportDto projectDto, Principal principal) {
        // TODO: Implement validation
        Long requesterId = Long.parseLong(principal.getName());

        Project project = this.projectMapper.mapExportDtoToEntityForCreationAndPersist(projectDto, requesterId);

        ProjectUserAccess principalAccess = project.getProjectUserAccesses().stream()
            .filter(access -> access.getUser().getId().equals(Long.parseLong(principal.getName())))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Principal not found in project access list"));

        principalAccess.setIsAdmin(true);

        this.projectUserAccessRepository.save(principalAccess);

        ExternalToolsData externalToolsData = project.getExternalToolsData();
        externalToolsData.setCreatorUserId(principalAccess.getUser().getGitlabId());

        this.externalToolsDataRepository.save(externalToolsData);

        return this.projectMapper.mapEntityToProjectDto(project);
    }

    @Override
    public BuildResponseDto performFileConsistencyCheck(Long projectId, Principal principal) {
        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        if (project.getGitlabProject().getGitlabId() == null) {
            return BuildResponseDto.builder()
                .success(false)
                .message("Gitlab project not created")
                .build();
        }

        long requesterId = Long.parseLong(principal.getName());

        List<String> inconsistencies = this.fileGenerationService.performFileStructureCheck(project, requesterId);
        if (inconsistencies.isEmpty()) {
            return BuildResponseDto.builder()
                .success(true)
                .message("No inconsistencies found")
                .build();
        }

        StringBuilder returnMessage = new StringBuilder("Inconsistencies found: \n");
        for (String inconsistency : inconsistencies) {
            returnMessage.append(inconsistency).append("\n");
        }

        return BuildResponseDto.builder()
            .success(false)
            .message(returnMessage.toString())
            .build();
    }

    private void hasBuildAccess(Long projectId, Principal principal) {
        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        User user = project.getProjectUserAccesses().stream()
            .filter(access -> access.getUser().getId().equals(Long.parseLong(principal.getName())))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("User not found in project access list"))
            .getUser();

        boolean hasAccess = project.getProjectUserAccesses().stream()
            .anyMatch(access -> access.getUser().getId().equals(user.getId()) && access.getIsAdmin() && project.getExternalToolsData().getCreatorUserId().equals(user.getGitlabId()));

        if (!hasAccess) {
            throw new AccessForbiddenException("Access to project denied, user cannot build project, only project creator can build project");
        }
    }
}
