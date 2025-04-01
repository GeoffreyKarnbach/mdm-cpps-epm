package project.backend.mapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import project.backend.dto.*;
import project.backend.entity.*;
import project.backend.repository.*;
import project.backend.service.UserService;

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProjectMapper {

    private final UserRepository userRepository;
    private final WorkSpaceRepository workSpaceRepository;
    private final ProjectUserAccessRepository projectUserAccessRepository;
    private final ProjectRepository projectRepository;
    private final ExternalToolsDataRepository externalToolsDataRepository;
    private final GitlabPrIssueTemplateRepository gitlabPrIssueTemplateRepository;
    private final GitlabSubgroupRepository gitlabSubgroupRepository;
    private final UserService userService;
    private final GitlabProjectRepository gitlabProjectRepository;

    @Transactional
    public Project mapDtoToEntityForCreationAndPersist(CppsProjectDto projectDto) {
        Project project = new Project();

        // Set project details
        project.setName(projectDto.getDetails().getName());
        project.setDescription(projectDto.getDetails().getDescription());
        project.setVersion(projectDto.getDetails().getVersion());
        project.setIsDemo(projectDto.getDetails().getDemo());
        project.setProjectUserAccesses(new HashSet<>());
        project.setWorkSpaces(new HashSet<>());
        project.setGitlabPrIssueTemplates(new HashSet<>());

        projectRepository.save(project);

        ExternalToolsData externalToolsData = new ExternalToolsData();
        externalToolsData.setRootGroupId(projectDto.getExternalToolsDetails().getRootGroupId());
        externalToolsData.setProject(project);

        externalToolsDataRepository.save(externalToolsData);

        project.setExternalToolsData(externalToolsData);

        Set<ProjectUserAccess> projectUserAccesses = new HashSet<>();

        // Map users to ProjectUserAccess entities
        for (ProjectUserDto userDto : projectDto.getUsers()) {
            User user = userRepository.findById(userDto.getUser().getId()).orElseThrow(() -> new RuntimeException("User not found"));

            ProjectUserAccess projectUserAccess = new ProjectUserAccess();
            projectUserAccess.setUser(user);
            projectUserAccess.setIsReviewer(userDto.isReviewer());
            projectUserAccess.setIsAdmin(userDto.isAdmin());
            projectUserAccess.setProject(project);

            projectUserAccesses.add(projectUserAccess); // Do not save yet

            projectUserAccessRepository.save(projectUserAccess);
        }

        project.setProjectUserAccesses(projectUserAccesses);

        // Handle the common workspace
        WorkSpace commonWorkspace = workSpaceRepository.findById(projectDto.getCommonWorkspace().getId())
            .orElse(new WorkSpace());
        commonWorkspace.setId(projectDto.getCommonWorkspace().getId());
        commonWorkspace.setName(projectDto.getCommonWorkspace().getName());
        commonWorkspace.setIsCommon(true);
        commonWorkspace.setProject(project);

        Set<User> commonWorkspaceUsers = new HashSet<>();

        // Set users for the common workspace
        for (UserDto userDto : projectDto.getCommonWorkspace().getUsers()) {
            User user = userRepository.findById(userDto.getId()).orElseThrow(() -> new RuntimeException("User not found"));
            commonWorkspaceUsers.add(user);
        }

        commonWorkspace.setUsers(commonWorkspaceUsers);


        Set<WorkSpace> domainWorkspaces = new HashSet<>();
        domainWorkspaces.add(commonWorkspace);

        // Map and create domain workspaces
        for (DomainWorkspaceDto domainWorkspaceDto : projectDto.getDomainWorkspaces()) {
            WorkSpace domainWorkspace = workSpaceRepository.findById(domainWorkspaceDto.getId())
                .orElse(new WorkSpace());  // Fetch or create new WorkSpace

            domainWorkspace.setId(domainWorkspaceDto.getId());
            domainWorkspace.setName(domainWorkspaceDto.getName());
            domainWorkspace.setIsCommon(false);
            domainWorkspace.setProject(project);

            // Set users for the domain workspace
            Set<User> domainWorkspaceUsers = new HashSet<>();

            for (UserDto userDto : domainWorkspaceDto.getUsers()) {
                User user = userRepository.findById(userDto.getId()).orElseThrow(() -> new RuntimeException("User not found"));
                domainWorkspaceUsers.add(user);
            }
            domainWorkspace.setUsers(domainWorkspaceUsers);

            domainWorkspaces.add(domainWorkspace);
        }

        project.setWorkSpaces(domainWorkspaces);

        Set<GitlabPrIssueTemplate> gitlabPrIssueTemplates = new HashSet<>();

        // Map Gitlab MR and Issue templates
        for (GitlabIssuePrTemplateDto mrTemplateDto: projectDto.getExternalToolsDetails().getMergeRequestTemplates()){
            GitlabPrIssueTemplate gitlabPrIssueTemplate = new GitlabPrIssueTemplate();
            gitlabPrIssueTemplate.setProject(project);
            gitlabPrIssueTemplate.setName(mrTemplateDto.getName());
            gitlabPrIssueTemplate.setContent(mrTemplateDto.getContent());
            gitlabPrIssueTemplate.setIsPrTemplate(true);

            this.gitlabPrIssueTemplateRepository.save(gitlabPrIssueTemplate);

            gitlabPrIssueTemplates.add(gitlabPrIssueTemplate);
        }

        for (GitlabIssuePrTemplateDto issueTemplateDto: projectDto.getExternalToolsDetails().getIssueTemplates()){
            GitlabPrIssueTemplate gitlabPrIssueTemplate = new GitlabPrIssueTemplate();
            gitlabPrIssueTemplate.setProject(project);
            gitlabPrIssueTemplate.setName(issueTemplateDto.getName());
            gitlabPrIssueTemplate.setContent(issueTemplateDto.getContent());
            gitlabPrIssueTemplate.setIsPrTemplate(false);

            this.gitlabPrIssueTemplateRepository.save(gitlabPrIssueTemplate);

            gitlabPrIssueTemplates.add(gitlabPrIssueTemplate);
        }

        project.setGitlabPrIssueTemplates(gitlabPrIssueTemplates);

        projectRepository.save(project);

        for (WorkSpace workspace: project.getWorkSpaces()) {
            for (User user: workspace.getUsers()) {
                user.getWorkSpaces().add(workspace);
            }
        }

        project.setId(project.getId());

        return project;
    }

    @Transactional
    public Project mapDtoToEntityForCreationAndPersistDemoProject(DemoProjectDto projectDto, Long principalId) {
        Project project = new Project();

        // Set project details
        project.setName(projectDto.getCppsProject().getDetails().getName());
        project.setDescription(projectDto.getCppsProject().getDetails().getDescription());
        project.setVersion(projectDto.getCppsProject().getDetails().getVersion());
        project.setIsDemo(projectDto.getCppsProject().getDetails().getDemo());
        project.setProjectUserAccesses(new HashSet<>());
        project.setWorkSpaces(new HashSet<>());
        project.setGitlabPrIssueTemplates(new HashSet<>());

        projectRepository.save(project);

        ExternalToolsData externalToolsData = new ExternalToolsData();
        externalToolsData.setRootGroupId(projectDto.getCppsProject().getExternalToolsDetails().getRootGroupId());
        externalToolsData.setProject(project);

        externalToolsDataRepository.save(externalToolsData);

        project.setExternalToolsData(externalToolsData);

        Set<ProjectUserAccess> projectUserAccesses = new HashSet<>();

        // Map users to ProjectUserAccess entities
        for (ProjectUserDto userDto : projectDto.getCppsProject().getUsers()) {
            User user = userRepository.findById(userDto.getUser().getId()).orElseThrow(() -> new RuntimeException("User not found"));

            ProjectUserAccess projectUserAccess = new ProjectUserAccess();
            projectUserAccess.setUser(user);
            projectUserAccess.setIsReviewer(userDto.isReviewer());
            projectUserAccess.setIsAdmin(userDto.isAdmin());
            projectUserAccess.setProject(project);

            projectUserAccesses.add(projectUserAccess); // Do not save yet

            projectUserAccessRepository.save(projectUserAccess);
        }

        project.setProjectUserAccesses(projectUserAccesses);

        // Handle the common workspace
        WorkSpace commonWorkspace = new WorkSpace();
        commonWorkspace.setName("Common Workspace");
        commonWorkspace.setIsCommon(true);
        commonWorkspace.setProject(project);

        Set<User> commonWorkspaceUsers = new HashSet<>();
        // Set all users as common workspace users
        for (ProjectUserDto userDto : projectDto.getCppsProject().getUsers()) {
            User user = userRepository.findById(userDto.getUser().getId()).orElseThrow(() -> new RuntimeException("User not found"));
            commonWorkspaceUsers.add(user);
        }
        commonWorkspace.setUsers(commonWorkspaceUsers);

        Set<WorkSpace> domainWorkspaces = new HashSet<>();
        domainWorkspaces.add(commonWorkspace);

        // Map and create domain workspaces
        for (int i = 0; i < projectDto.getDomainCount(); i++) {
            WorkSpace domainWorkspace = new WorkSpace();

            domainWorkspace.setName("Domain Workspace " + (i + 1));
            domainWorkspace.setIsCommon(false);
            domainWorkspace.setProject(project);

            // Set users for the domain workspace
            Set<User> domainWorkspaceUsers = new HashSet<>();
            // Set principal as the only user for the domain workspace
            User principal = userRepository.findById(principalId).orElseThrow(() -> new RuntimeException("User not found"));
            domainWorkspaceUsers.add(principal);

            // Add other random subset of users to the domain workspace
            List<ProjectUserDto> users = projectDto.getCppsProject().getUsers();
            Collections.shuffle(users);
            for (int j = 0; j < users.size() / 2; j++) {
                User user = userRepository.findById(users.get(j).getUser().getId()).orElseThrow(() -> new RuntimeException("User not found"));
                // Check if not principal
                if (!Objects.equals(user.getId(), principalId)) {
                    domainWorkspaceUsers.add(user);
                }
            }

            domainWorkspace.setUsers(domainWorkspaceUsers);

            domainWorkspaces.add(domainWorkspace);
        }

        project.setWorkSpaces(domainWorkspaces);

        Set<GitlabPrIssueTemplate> gitlabPrIssueTemplates = new HashSet<>();

        // Map Gitlab MR and Issue templates
        for (GitlabIssuePrTemplateDto mrTemplateDto: projectDto.getCppsProject().getExternalToolsDetails().getMergeRequestTemplates()){
            GitlabPrIssueTemplate gitlabPrIssueTemplate = new GitlabPrIssueTemplate();
            gitlabPrIssueTemplate.setProject(project);
            gitlabPrIssueTemplate.setName(mrTemplateDto.getName());
            gitlabPrIssueTemplate.setContent(mrTemplateDto.getContent());
            gitlabPrIssueTemplate.setIsPrTemplate(true);

            this.gitlabPrIssueTemplateRepository.save(gitlabPrIssueTemplate);

            gitlabPrIssueTemplates.add(gitlabPrIssueTemplate);
        }

        for (GitlabIssuePrTemplateDto issueTemplateDto: projectDto.getCppsProject().getExternalToolsDetails().getIssueTemplates()){
            GitlabPrIssueTemplate gitlabPrIssueTemplate = new GitlabPrIssueTemplate();
            gitlabPrIssueTemplate.setProject(project);
            gitlabPrIssueTemplate.setName(issueTemplateDto.getName());
            gitlabPrIssueTemplate.setContent(issueTemplateDto.getContent());
            gitlabPrIssueTemplate.setIsPrTemplate(false);

            this.gitlabPrIssueTemplateRepository.save(gitlabPrIssueTemplate);

            gitlabPrIssueTemplates.add(gitlabPrIssueTemplate);
        }

        project.setGitlabPrIssueTemplates(gitlabPrIssueTemplates);

        projectRepository.save(project);

        for (WorkSpace workspace: project.getWorkSpaces()) {
            for (User user: workspace.getUsers()) {
                user.getWorkSpaces().add(workspace);
            }
        }

        project.setId(project.getId());

        return project;
    }

    public CppsProjectDto mapEntityToProjectDto(Project project) {
        CppsProjectDto cppsProjectDto = new CppsProjectDto();

        cppsProjectDto.setId(project.getId());

        ProjectDetailsDto projectDetailsDto = new ProjectDetailsDto();
        projectDetailsDto.setName(project.getName());
        projectDetailsDto.setDescription(project.getDescription());
        projectDetailsDto.setVersion(project.getVersion());
        projectDetailsDto.setDemo(project.getIsDemo());

        cppsProjectDto.setDetails(projectDetailsDto);

        ExternalToolsDetailsDto externalToolsDetailsDto = new ExternalToolsDetailsDto();
        externalToolsDetailsDto.setRootGroupId(project.getExternalToolsData().getRootGroupId());
        externalToolsDetailsDto.setCreatorGitlabUserId(project.getExternalToolsData().getCreatorUserId());

        if (project.getGitlabProject() != null) {
            externalToolsDetailsDto.setGitlabProjectId(project.getGitlabProject().getGitlabId());
        } else {
            externalToolsDetailsDto.setGitlabProjectId(null);
        }

        List<GitlabIssuePrTemplateDto> mergeRequestTemplates = new ArrayList<>();
        List<GitlabIssuePrTemplateDto> issueTemplates = new ArrayList<>();

        for (GitlabPrIssueTemplate gitlabPrIssueTemplate: project.getGitlabPrIssueTemplates()){
            GitlabIssuePrTemplateDto gitlabIssuePrTemplateDto = new GitlabIssuePrTemplateDto();
            gitlabIssuePrTemplateDto.setName(gitlabPrIssueTemplate.getName());
            gitlabIssuePrTemplateDto.setContent(gitlabPrIssueTemplate.getContent());

            if (gitlabPrIssueTemplate.getIsPrTemplate()){
                mergeRequestTemplates.add(gitlabIssuePrTemplateDto);
            } else {
                issueTemplates.add(gitlabIssuePrTemplateDto);
            }
        }

        externalToolsDetailsDto.setMergeRequestTemplates(mergeRequestTemplates);
        externalToolsDetailsDto.setIssueTemplates(issueTemplates);
        externalToolsDetailsDto.setGitlabHasBeenGenerated(project.getGitlabProject() != null);

        cppsProjectDto.setExternalToolsDetails(externalToolsDetailsDto);

        Map<Long, UserDto> userMap = new HashMap<>();

        List<ProjectUserDto> projectUser = new ArrayList<>();
        for (ProjectUserAccess projectUserAccess : project.getProjectUserAccesses()) {
            ProjectUserDto projectUserDto = new ProjectUserDto();
            projectUserDto.setAdmin(projectUserAccess.getIsAdmin());
            projectUserDto.setReviewer(projectUserAccess.getIsReviewer());

            UserDto userDto = new UserDto();
            userDto.setId(projectUserAccess.getUser().getId());
            userDto.setGitlabId(projectUserAccess.getUser().getGitlabId());
            userDto.setGitlabUsername(projectUserAccess.getUser().getGitlabUsername());
            userDto.setEmail(projectUserAccess.getUser().getEmail());
            userDto.setAvatarUrl(projectUserAccess.getUser().getAvatarUrl());

            userMap.put(userDto.getId(), userDto);

            projectUserDto.setUser(userDto);

            projectUser.add(projectUserDto);
        }

        cppsProjectDto.setUsers(projectUser);

        Set<WorkSpace> commonWorkspace = project.getWorkSpaces();

        CommonWorkspaceDto commonWorkspaceDto = new CommonWorkspaceDto();
        for (WorkSpace workspace : commonWorkspace) {
            if (workspace.getIsCommon()) {
                commonWorkspaceDto.setId(workspace.getId());
                commonWorkspaceDto.setName(workspace.getName());

                List<UserDto> users = new ArrayList<>();
                for (User user : workspace.getUsers()) {
                    users.add(userMap.get(user.getId()));
                }
                commonWorkspaceDto.setUsers(users);
            }
        }

        cppsProjectDto.setCommonWorkspace(commonWorkspaceDto);

        List<DomainWorkspaceDto> domainWorkspaces = new ArrayList<>();
        for (WorkSpace workspace : commonWorkspace) {
            if (!workspace.getIsCommon()) {
                DomainWorkspaceDto domainWorkspaceDto = new DomainWorkspaceDto();
                domainWorkspaceDto.setId(workspace.getId());
                domainWorkspaceDto.setName(workspace.getName());

                List<UserDto> users = new ArrayList<>();
                for (User user : workspace.getUsers()) {
                    users.add(userMap.get(user.getId()));
                }
                domainWorkspaceDto.setUsers(users);

                domainWorkspaces.add(domainWorkspaceDto);
            }
        }

        cppsProjectDto.setDomainWorkspaces(domainWorkspaces);

        return cppsProjectDto;
    }

    public CppsProjectListItemDto mapProjectUserAccessEntityToProjectListItemDto(ProjectUserAccess projectUserAccess) {
        CppsProjectListItemDto cppsProjectListItemDto = new CppsProjectListItemDto();
        cppsProjectListItemDto.setDemo(projectUserAccess.getProject().getIsDemo());

        cppsProjectListItemDto.setId(projectUserAccess.getProject().getId());

        ProjectDetailsDto projectDetailsDto = new ProjectDetailsDto();
        projectDetailsDto.setName(projectUserAccess.getProject().getName());
        projectDetailsDto.setDescription(projectUserAccess.getProject().getDescription());
        projectDetailsDto.setVersion(projectUserAccess.getProject().getVersion());
        projectDetailsDto.setDemo(projectUserAccess.getProject().getIsDemo());

        cppsProjectListItemDto.setDetails(projectDetailsDto);

        cppsProjectListItemDto.setUserCount((long) projectUserAccess.getProject().getProjectUserAccesses().size());
        cppsProjectListItemDto.setWorkspaceCount((long) projectUserAccess.getProject().getWorkSpaces().size());

        cppsProjectListItemDto.setHasAdmin(projectUserAccess.getIsAdmin());

        return cppsProjectListItemDto;
    }

    public CppsProjectJSONExportDto mapProjectEntityToCppsProjectJSONExportDto(Project project) {
        CppsProjectJSONExportDto cppsProjectJSONExportDto = new CppsProjectJSONExportDto();
        cppsProjectJSONExportDto.setName(project.getName());
        cppsProjectJSONExportDto.setDescription(project.getDescription());
        cppsProjectJSONExportDto.setVersion(project.getVersion());
        cppsProjectJSONExportDto.setIsDemo(project.getIsDemo());

        WorkspaceJSONExportDto commonWorkspace = new WorkspaceJSONExportDto();
        for (WorkSpace workspace : project.getWorkSpaces()) {
            if (workspace.getIsCommon()) {
                commonWorkspace.setName(workspace.getName());
                List<String> users = new ArrayList<>();
                for (User user : workspace.getUsers()) {
                    users.add(String.valueOf(user.getGitlabId()));
                }
                Set<GitlabSubgroup> gitlabSubgroups = workspace.getGitlabSubgroups();
                for (GitlabSubgroup gitlabSubgroup : gitlabSubgroups) {
                    if (gitlabSubgroup.getIsReviewerGroup()) {
                        commonWorkspace.setGitlabReviewerGroupId(gitlabSubgroup.getGitlabId());
                    } else {
                        commonWorkspace.setGitlabGroupId(gitlabSubgroup.getGitlabId());
                    }
                }

                commonWorkspace.setUsers(users);
            }
        }

        cppsProjectJSONExportDto.setCommonWorkspace(commonWorkspace);

        List<WorkspaceJSONExportDto> domainWorkspaces = new ArrayList<>();
        for (WorkSpace workspace : project.getWorkSpaces()) {
            if (!workspace.getIsCommon()) {
                WorkspaceJSONExportDto domainWorkspace = new WorkspaceJSONExportDto();
                domainWorkspace.setName(workspace.getName());
                List<String> users = new ArrayList<>();
                for (User user : workspace.getUsers()) {
                    users.add(String.valueOf(user.getGitlabId()));
                }
                domainWorkspace.setUsers(users);

                Set<GitlabSubgroup> gitlabSubgroups = workspace.getGitlabSubgroups();
                for (GitlabSubgroup gitlabSubgroup : gitlabSubgroups) {
                    if (gitlabSubgroup.getIsReviewerGroup()) {
                        domainWorkspace.setGitlabReviewerGroupId(gitlabSubgroup.getGitlabId());
                    } else {
                        domainWorkspace.setGitlabGroupId(gitlabSubgroup.getGitlabId());
                    }
                }

                domainWorkspaces.add(domainWorkspace);
            }
        }

        cppsProjectJSONExportDto.setDomainWorkspaces(domainWorkspaces);

        List<UserJSONExportDto> users = new ArrayList<>();
        for (ProjectUserAccess projectUserAccess : project.getProjectUserAccesses()) {
            UserJSONExportDto user = new UserJSONExportDto();
            user.setGitlabId(String.valueOf(projectUserAccess.getUser().getGitlabId()));
            user.setGitlabUsername(projectUserAccess.getUser().getGitlabUsername());
            user.setEmail(projectUserAccess.getUser().getEmail());
            user.setIsAdmin(projectUserAccess.getIsAdmin());
            user.setIsReviewer(projectUserAccess.getIsReviewer());
            users.add(user);
        }

        cppsProjectJSONExportDto.setUsers(users);

        ExternalToolsDetailsDto externalToolsDetailsDto = new ExternalToolsDetailsDto();
        externalToolsDetailsDto.setRootGroupId(project.getExternalToolsData().getRootGroupId());
        externalToolsDetailsDto.setCreatorGitlabUserId(project.getExternalToolsData().getCreatorUserId());
        if (project.getGitlabProject() != null) {
            externalToolsDetailsDto.setGitlabProjectId(project.getGitlabProject().getGitlabId());
        } else {
            externalToolsDetailsDto.setGitlabProjectId(null);
        }

        List<GitlabIssuePrTemplateDto> mergeRequestTemplates = new ArrayList<>();
        List<GitlabIssuePrTemplateDto> issueTemplates = new ArrayList<>();

        for (GitlabPrIssueTemplate gitlabPrIssueTemplate: project.getGitlabPrIssueTemplates()){
            GitlabIssuePrTemplateDto gitlabIssuePrTemplateDto = new GitlabIssuePrTemplateDto();
            gitlabIssuePrTemplateDto.setName(gitlabPrIssueTemplate.getName());
            gitlabIssuePrTemplateDto.setContent(gitlabPrIssueTemplate.getContent());

            if (gitlabPrIssueTemplate.getIsPrTemplate()){
                mergeRequestTemplates.add(gitlabIssuePrTemplateDto);
            } else {
                issueTemplates.add(gitlabIssuePrTemplateDto);
            }
        }

        externalToolsDetailsDto.setMergeRequestTemplates(mergeRequestTemplates);
        externalToolsDetailsDto.setIssueTemplates(issueTemplates);

        externalToolsDetailsDto.setGitlabHasBeenGenerated(project.getGitlabProject() != null);

        cppsProjectJSONExportDto.setExternalToolsDetails(externalToolsDetailsDto);

        return cppsProjectJSONExportDto;
    }

    @Transactional
    public Project mapDtoToEntityForUpdateAndPersist(CppsProjectDto projectDto, Project project) {
        // Update project details - the name can't be updated
        project.setDescription(projectDto.getDetails().getDescription());
        project.setVersion(projectDto.getDetails().getVersion());
        project.setIsDemo(projectDto.getDetails().getDemo());

        ExternalToolsData externalToolsData = project.getExternalToolsData();
        externalToolsData.setRootGroupId(projectDto.getExternalToolsDetails().getRootGroupId());
        externalToolsData.setProject(project);

        // Update the project user accesses
        Set<ProjectUserAccess> projectUserAccessesUpdated = new HashSet<>();
        Set<ProjectUserAccess> currentProjectUserAccesses = project.getProjectUserAccesses();

        for (ProjectUserDto userDto : projectDto.getUsers()) {
            User user = userRepository.findById(userDto.getUser().getId()).orElseThrow(() -> new RuntimeException("User not found"));

            ProjectUserAccess projectUserAccess = currentProjectUserAccesses.stream()
                .filter(access -> access.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(new ProjectUserAccess());

            projectUserAccess.setUser(user);
            projectUserAccess.setIsReviewer(userDto.isReviewer());
            projectUserAccess.setIsAdmin(userDto.isAdmin());
            projectUserAccess.setProject(project);

            projectUserAccessesUpdated.add(projectUserAccess);

            projectUserAccessRepository.save(projectUserAccess);
        }

        project.setProjectUserAccesses(projectUserAccessesUpdated);

        Set<WorkSpace> workSpacesUpdated = new HashSet<>();

        // Update the common workspace
        WorkSpace commonWorkspace = workSpaceRepository.findById(projectDto.getCommonWorkspace().getId())
            .orElse(new WorkSpace());

        commonWorkspace.setId(projectDto.getCommonWorkspace().getId());
        commonWorkspace.setName(projectDto.getCommonWorkspace().getName());
        commonWorkspace.setIsCommon(true);
        commonWorkspace.setProject(project);

        Set<User> commonWorkspaceUsers = new HashSet<>();

        for (UserDto userDto : projectDto.getCommonWorkspace().getUsers()) {
            User user = userRepository.findById(userDto.getId()).orElseThrow(() -> new RuntimeException("User not found"));
            commonWorkspaceUsers.add(user);
        }

        commonWorkspace.setUsers(commonWorkspaceUsers);

        workSpacesUpdated.add(commonWorkspace);

        // Update the domain workspaces
        Set<WorkSpace> oldWorkspaces = project.getWorkSpaces();

        for (DomainWorkspaceDto domainWorkspaceDto : projectDto.getDomainWorkspaces()) {
            WorkSpace domainWorkspace = workSpaceRepository.findById(domainWorkspaceDto.getId())
                .orElse(new WorkSpace());

            domainWorkspace.setId(domainWorkspaceDto.getId());
            domainWorkspace.setName(domainWorkspaceDto.getName());
            domainWorkspace.setIsCommon(false);
            domainWorkspace.setProject(project);

            Set<User> domainWorkspaceUsers = new HashSet<>();

            for (UserDto userDto : domainWorkspaceDto.getUsers()) {
                User user = userRepository.findById(userDto.getId()).orElseThrow(() -> new RuntimeException("User not found"));
                domainWorkspaceUsers.add(user);
            }

            domainWorkspace.setUsers(domainWorkspaceUsers);

            workSpacesUpdated.add(domainWorkspace);
        }

        project.setWorkSpaces(workSpacesUpdated);

        // Update Gitlab MR and Issue templates
        project.getGitlabPrIssueTemplates().clear();

        Set<GitlabPrIssueTemplate> gitlabPrIssueTemplates = new HashSet<>();

        for (GitlabIssuePrTemplateDto mrTemplateDto: projectDto.getExternalToolsDetails().getMergeRequestTemplates()){
            GitlabPrIssueTemplate gitlabPrIssueTemplate = new GitlabPrIssueTemplate();
            gitlabPrIssueTemplate.setProject(project);
            gitlabPrIssueTemplate.setName(mrTemplateDto.getName());
            gitlabPrIssueTemplate.setContent(mrTemplateDto.getContent());
            gitlabPrIssueTemplate.setIsPrTemplate(true);

            this.gitlabPrIssueTemplateRepository.save(gitlabPrIssueTemplate);

            gitlabPrIssueTemplates.add(gitlabPrIssueTemplate);
        }

        for (GitlabIssuePrTemplateDto issueTemplateDto: projectDto.getExternalToolsDetails().getIssueTemplates()){
            GitlabPrIssueTemplate gitlabPrIssueTemplate = new GitlabPrIssueTemplate();
            gitlabPrIssueTemplate.setProject(project);
            gitlabPrIssueTemplate.setName(issueTemplateDto.getName());
            gitlabPrIssueTemplate.setContent(issueTemplateDto.getContent());
            gitlabPrIssueTemplate.setIsPrTemplate(false);

            this.gitlabPrIssueTemplateRepository.save(gitlabPrIssueTemplate);

            gitlabPrIssueTemplates.add(gitlabPrIssueTemplate);
        }

        project.setGitlabPrIssueTemplates(gitlabPrIssueTemplates);

        projectRepository.save(project);

        for (WorkSpace workspace: project.getWorkSpaces()) {
            for (User user: workspace.getUsers()) {
                user.getWorkSpaces().add(workspace);
            }
        }

        return project;
    }

    public CppsProjectJSONExportDto mapProjectEntityToCppsProjectJSONExportDtoAnonymized(Project project) {
        CppsProjectJSONExportDto cppsProjectJSONExportDto = new CppsProjectJSONExportDto();
        cppsProjectJSONExportDto.setName("insert-your-project-name");
        cppsProjectJSONExportDto.setDescription("insert-your-project-description");
        cppsProjectJSONExportDto.setVersion("insert-your-project-version");
        cppsProjectJSONExportDto.setIsDemo(false);

        WorkspaceJSONExportDto commonWorkspace = new WorkspaceJSONExportDto();
        for (WorkSpace workspace : project.getWorkSpaces()) {
            if (workspace.getIsCommon()) {
                commonWorkspace.setName(workspace.getName());
                List<String> users = new ArrayList<>();
                commonWorkspace.setUsers(users);
                commonWorkspace.setGitlabReviewerGroupId(0L);
                commonWorkspace.setGitlabGroupId(0L);
            }
        }

        cppsProjectJSONExportDto.setCommonWorkspace(commonWorkspace);

        List<WorkspaceJSONExportDto> domainWorkspaces = new ArrayList<>();
        for (WorkSpace workspace : project.getWorkSpaces()) {
            if (!workspace.getIsCommon()) {
                WorkspaceJSONExportDto domainWorkspace = new WorkspaceJSONExportDto();
                domainWorkspace.setName(workspace.getName());
                List<String> users = new ArrayList<>();
                domainWorkspace.setUsers(users);
                domainWorkspace.setGitlabReviewerGroupId(0L);
                domainWorkspace.setGitlabGroupId(0L);
                domainWorkspaces.add(domainWorkspace);
            }
        }

        cppsProjectJSONExportDto.setDomainWorkspaces(domainWorkspaces);

        List<UserJSONExportDto> users = new ArrayList<>();
        cppsProjectJSONExportDto.setUsers(users);

        ExternalToolsDetailsDto externalToolsDetailsDto = new ExternalToolsDetailsDto();
        externalToolsDetailsDto.setRootGroupId(0L);
        externalToolsDetailsDto.setCreatorGitlabUserId(0L);
        externalToolsDetailsDto.setGitlabProjectId(0L);

        List<GitlabIssuePrTemplateDto> mergeRequestTemplates = new ArrayList<>();
        List<GitlabIssuePrTemplateDto> issueTemplates = new ArrayList<>();

        for (GitlabPrIssueTemplate gitlabPrIssueTemplate: project.getGitlabPrIssueTemplates()){
            GitlabIssuePrTemplateDto gitlabIssuePrTemplateDto = new GitlabIssuePrTemplateDto();
            gitlabIssuePrTemplateDto.setName(gitlabPrIssueTemplate.getName());
            gitlabIssuePrTemplateDto.setContent(gitlabPrIssueTemplate.getContent());

            if (gitlabPrIssueTemplate.getIsPrTemplate()){
                mergeRequestTemplates.add(gitlabIssuePrTemplateDto);
            } else {
                issueTemplates.add(gitlabIssuePrTemplateDto);
            }
        }

        externalToolsDetailsDto.setMergeRequestTemplates(mergeRequestTemplates);
        externalToolsDetailsDto.setIssueTemplates(issueTemplates);

        externalToolsDetailsDto.setGitlabHasBeenGenerated(false);

        cppsProjectJSONExportDto.setExternalToolsDetails(externalToolsDetailsDto);

        return cppsProjectJSONExportDto;
    }

    @Transactional
    public Project mapExportDtoToEntityForCreationAndPersist(CppsProjectJSONExportDto projectJSONExportDto, Long principalId) {
        Project project = new Project();

        // Set project details
        project.setName(projectJSONExportDto.getName());
        project.setDescription(projectJSONExportDto.getDescription());
        project.setVersion(projectJSONExportDto.getVersion());
        project.setIsDemo(projectJSONExportDto.getIsDemo());

        project.setProjectUserAccesses(new HashSet<>());
        project.setWorkSpaces(new HashSet<>());
        project.setGitlabPrIssueTemplates(new HashSet<>());

        projectRepository.save(project);

        ExternalToolsData externalToolsData = new ExternalToolsData();
        externalToolsData.setRootGroupId(projectJSONExportDto.getExternalToolsDetails().getRootGroupId());
        externalToolsData.setCreatorUserId(projectJSONExportDto.getExternalToolsDetails().getCreatorGitlabUserId());
        externalToolsData.setProject(project);

        externalToolsDataRepository.save(externalToolsData);

        project.setExternalToolsData(externalToolsData);

        // Create all users
        for (UserJSONExportDto userDto : projectJSONExportDto.getUsers()) {
            UserDto user = userService.register_or_return_gitlab_user(Long.parseLong(userDto.getGitlabId()), principalId);
            User userEntity = userService.getUserEntity(user.getId());

            ProjectUserAccess projectUserAccess = ProjectUserAccess.builder()
                .project(project)
                .user(userEntity)
                .isAdmin(userDto.getIsAdmin())
                .isReviewer(userDto.getIsReviewer())
                .build();

            projectUserAccessRepository.save(projectUserAccess);

            project.getProjectUserAccesses().add(projectUserAccess);
        }

        // Create common workspace
        WorkSpace commonWorkspace = new WorkSpace();
        commonWorkspace.setName(projectJSONExportDto.getCommonWorkspace().getName());
        commonWorkspace.setIsCommon(true);
        commonWorkspace.setProject(project);

        workSpaceRepository.save(commonWorkspace);

        Set<GitlabSubgroup> commonWorkspaceGitlabSubgroups = new HashSet<>();
        GitlabSubgroup commonWorkspaceGitlabSubgroup = GitlabSubgroup.builder()
            .gitlabId(projectJSONExportDto.getCommonWorkspace().getGitlabGroupId())
            .isReviewerGroup(false)
            .workspace(commonWorkspace)
            .build();

        commonWorkspaceGitlabSubgroups.add(commonWorkspaceGitlabSubgroup);

        GitlabSubgroup commonWorkspaceReviewerGitlabSubgroup = GitlabSubgroup.builder()
            .gitlabId(projectJSONExportDto.getCommonWorkspace().getGitlabReviewerGroupId())
            .isReviewerGroup(true)
            .workspace(commonWorkspace)
            .build();

        commonWorkspaceGitlabSubgroups.add(commonWorkspaceGitlabSubgroup);
        commonWorkspaceGitlabSubgroups.add(commonWorkspaceReviewerGitlabSubgroup);

        this.gitlabSubgroupRepository.saveAll(commonWorkspaceGitlabSubgroups);
        commonWorkspace.setGitlabSubgroups(commonWorkspaceGitlabSubgroups);

        // Set users for the common workspace
        Set<User> commonWorkspaceUsers = new HashSet<>();
        for (String userId : projectJSONExportDto.getCommonWorkspace().getUsers()) {
            User user = userService.getUserEntityByGitlabId(Long.parseLong(userId));
            commonWorkspaceUsers.add(user);
        }

        commonWorkspace.setUsers(commonWorkspaceUsers);

        // Domain workspaces
        for (WorkspaceJSONExportDto domainWorkspaceDto : projectJSONExportDto.getDomainWorkspaces()) {
            WorkSpace domainWorkspace = new WorkSpace();
            domainWorkspace.setName(domainWorkspaceDto.getName());
            domainWorkspace.setIsCommon(false);
            domainWorkspace.setProject(project);

            workSpaceRepository.save(domainWorkspace);

            Set<GitlabSubgroup> domainWorkspaceGitlabSubgroups = new HashSet<>();
            GitlabSubgroup domainWorkspaceGitlabSubgroup = GitlabSubgroup.builder()
                .gitlabId(domainWorkspaceDto.getGitlabGroupId())
                .isReviewerGroup(false)
                .workspace(domainWorkspace)
                .build();

            domainWorkspaceGitlabSubgroups.add(domainWorkspaceGitlabSubgroup);

            GitlabSubgroup domainWorkspaceReviewerGitlabSubgroup = GitlabSubgroup.builder()
                .gitlabId(domainWorkspaceDto.getGitlabReviewerGroupId())
                .isReviewerGroup(true)
                .workspace(domainWorkspace)
                .build();

            domainWorkspaceGitlabSubgroups.add(domainWorkspaceReviewerGitlabSubgroup);

            this.gitlabSubgroupRepository.saveAll(domainWorkspaceGitlabSubgroups);
            domainWorkspace.setGitlabSubgroups(domainWorkspaceGitlabSubgroups);

            // Set users for the domain workspace
            Set<User> domainWorkspaceUsers = new HashSet<>();
            for (String userId : domainWorkspaceDto.getUsers()) {
                User user = userService.getUserEntityByGitlabId(Long.parseLong(userId));
                domainWorkspaceUsers.add(user);
            }

            domainWorkspace.setUsers(domainWorkspaceUsers);

            project.getWorkSpaces().add(domainWorkspace);
        }

        project.getWorkSpaces().add(commonWorkspace);

        // Handle Gitlab MR and Issue templates
        for (GitlabIssuePrTemplateDto mrTemplateDto: projectJSONExportDto.getExternalToolsDetails().getMergeRequestTemplates()){
            GitlabPrIssueTemplate gitlabPrIssueTemplate = new GitlabPrIssueTemplate();
            gitlabPrIssueTemplate.setProject(project);
            gitlabPrIssueTemplate.setName(mrTemplateDto.getName());
            gitlabPrIssueTemplate.setContent(mrTemplateDto.getContent());
            gitlabPrIssueTemplate.setIsPrTemplate(true);

            this.gitlabPrIssueTemplateRepository.save(gitlabPrIssueTemplate);

            project.getGitlabPrIssueTemplates().add(gitlabPrIssueTemplate);
        }

        for (GitlabIssuePrTemplateDto issueTemplateDto: projectJSONExportDto.getExternalToolsDetails().getIssueTemplates()){
            GitlabPrIssueTemplate gitlabPrIssueTemplate = new GitlabPrIssueTemplate();
            gitlabPrIssueTemplate.setProject(project);
            gitlabPrIssueTemplate.setName(issueTemplateDto.getName());
            gitlabPrIssueTemplate.setContent(issueTemplateDto.getContent());
            gitlabPrIssueTemplate.setIsPrTemplate(false);

            this.gitlabPrIssueTemplateRepository.save(gitlabPrIssueTemplate);

            project.getGitlabPrIssueTemplates().add(gitlabPrIssueTemplate);
        }

        // Set gitlab project
        if (projectJSONExportDto.getExternalToolsDetails().getGitlabProjectId() != 0L) {
            GitlabProject gitlabProject = GitlabProject.builder()
                .gitlabId(projectJSONExportDto.getExternalToolsDetails().getGitlabProjectId())
                .project(project)
                .build();

            gitlabProjectRepository.save(gitlabProject);

            project.setGitlabProject(gitlabProject);
        }

        return project;
    }
}
