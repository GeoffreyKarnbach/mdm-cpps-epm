package project.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import project.backend.dto.GitlabGroupListItemDto;
import project.backend.entity.*;
import project.backend.exception.NotFoundException;
import project.backend.repository.GitlabProjectRepository;
import project.backend.repository.GitlabSubgroupRepository;
import project.backend.repository.ProjectRepository;
import project.backend.repository.WorkSpaceRepository;
import project.backend.service.GitlabService;
import project.backend.service.UserService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GitlabServiceImpl implements GitlabService {

    private final UserService userService;
    private final ProjectRepository projectRepository;
    private final GitlabProjectRepository gitlabProjectRepository;
    private final GitlabSubgroupRepository gitlabSubgroupRepository;
    private final WorkSpaceRepository workSpaceRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    private final String groupsGitlabUrl = "https://gitlab.com/api/v4/groups/";
    private final String projectsCreateUrl = "https://gitlab.com/api/v4/projects";
    private final String subgroupsCreateUrl = "https://gitlab.com/api/v4/groups";


    @Override
    public List<GitlabGroupListItemDto> getAllUserGroups(Principal principal) {

        long requesterId = Long.parseLong(principal.getName());

        HttpEntity<String> requestEntity = refreshAccessTokenAndGetHttpEntity(requesterId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(groupsGitlabUrl)
            .queryParam("top_level_only", "true");

        String urlWithParams = uriBuilder.toUriString();

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.GET, requestEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        List<GitlabGroupListItemDto> groups = new ArrayList<>();
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            for (JsonNode node : jsonNode) {
                GitlabGroupListItemDto group = GitlabGroupListItemDto.builder()
                    .id(node.get("id").asLong())
                    .name(node.get("name").asText())
                    .build();
                groups.add(group);
            }
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
            throw new NotFoundException("User not found");
        }
        return groups;
    }

    @Override
    public boolean createGitlabProject(Long projectId, long userId) {
        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        String projectName = project.getName();
        Long namespaceId = project.getExternalToolsData().getRootGroupId();

        HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(projectsCreateUrl)
            .queryParam("name", this.createSlug(projectName))
            .queryParam("namespace_id", namespaceId)
            .queryParam("visibility", "private");

        String urlWithParams = uriBuilder.toUriString();

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.POST, httpEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            GitlabProject gitlabProject = GitlabProject.builder()
                .project(project)
                .gitlabId(jsonNode.get("id").asLong())
                .build();
            this.gitlabProjectRepository.save(gitlabProject);
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
            return false;
        }

        return true;
    }

    @Override
    public boolean createGitlabSubgroups(Long projectId, long userId) {

        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        Long parentGroupId = project.getExternalToolsData().getRootGroupId();

        WorkSpace commonWorkspace = project.getWorkSpaces().stream().filter(
            WorkSpace::getIsCommon
        ).findFirst().orElseThrow(() -> new NotFoundException("Common workspace not found"));

        if (!createGitlabSubgroup(false, commonWorkspace.getId(), commonWorkspace.getName(), userId, parentGroupId)) {
            return false;
        }

        if (!createGitlabSubgroup(true, commonWorkspace.getId(), commonWorkspace.getName() + "_reviewers", userId, parentGroupId)) {
            return false;
        }

        for (WorkSpace workSpace : project.getWorkSpaces()) {
            if (!workSpace.getIsCommon()) {
                if (!createGitlabSubgroup(false, workSpace.getId(), workSpace.getName(), userId, parentGroupId)) {
                    return false;
                }
                if (!createGitlabSubgroup(true, workSpace.getId(), workSpace.getName() + "_reviewers", userId, parentGroupId)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean addGitlabProjectUsers(Long projectId, long userId) {
        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        Long projectIdInGitlab = this.gitlabProjectRepository.findByProjectId(projectId)
            .stream().findFirst()
            .orElseThrow(() -> new NotFoundException("Gitlab project not found")).getGitlabId();

        for (ProjectUserAccess projectUserAccess : project.getProjectUserAccesses()) {
            User user = projectUserAccess.getUser();
            if (!user.getGitlabId().equals(project.getExternalToolsData().getCreatorUserId())){
                if (!addUserToProject(projectIdInGitlab, user.getGitlabId(), userId, projectUserAccess.getIsAdmin())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean addGitlabSubgroupsUsers(Long projectId, long userId) {
        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        Long creatorUserId = project.getExternalToolsData().getCreatorUserId();

        for (WorkSpace workSpace : project.getWorkSpaces()) {
            Long groupId = this.gitlabSubgroupRepository.findNonReviewerSubgroupByWorkspaceId(workSpace.getId())
                .orElseThrow(() -> new NotFoundException("Gitlab subgroup not found")).getGitlabId();
            log.info("Adding all users to group: " + groupId);
            for (User user : workSpace.getUsers()) {
                ProjectUserAccess projectUserAccess = project.getProjectUserAccesses().stream()
                    .filter(access -> access.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("User not found in project access list"));

                if (!user.getGitlabId().equals(creatorUserId)) {
                    if (!addUserToGroup(groupId, user.getGitlabId(), userId, projectUserAccess.getIsAdmin())) {
                        return false;
                    }
                }
            }

            Long reviewerGroupId = this.gitlabSubgroupRepository.findReviewerSubgroupByWorkspaceId(workSpace.getId())
                .orElseThrow(() -> new NotFoundException("Gitlab subgroup not found")).getGitlabId();

            log.info("Adding reviewer users to group: " + reviewerGroupId);

            for (User user : workSpace.getUsers()) {
                ProjectUserAccess projectUserAccess = project.getProjectUserAccesses().stream()
                    .filter(access -> access.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("User not found in project access list"));

                log.info(String.valueOf(projectUserAccess));
                if (!user.getGitlabId().equals(creatorUserId) && projectUserAccess.getIsReviewer()) {
                    if (!addUserToGroup(reviewerGroupId, user.getGitlabId(), userId, projectUserAccess.getIsAdmin())) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public boolean addGitlabDeployKey(Long projectId, long userId) {
        // Create if not present on the local machine the deploy key "id_rsa_mdcpps_epm" and "id_rsa_mdcpps_epm.pub"
        String privateKeyPath = System.getProperty("user.home") + "/.ssh/id_rsa_mdcpps_epm";
        String publicKeyPath = privateKeyPath + ".pub";

        if (!new File(privateKeyPath).exists() || !new File(publicKeyPath).exists()) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(
                    "ssh-keygen",
                    "-t", "rsa",
                    "-b", "4096",
                    "-f", privateKeyPath,
                    "-N", ""
                );
                processBuilder.inheritIO();
                Process process = processBuilder.start();
                process.waitFor();
            } catch (IOException | InterruptedException e) {
                log.error("Error generating SSH key", e);
                return false;
            }
        } else {
            log.info("SSH key already exists");
        }

        HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

        Long projectIdInGitlab = this.gitlabProjectRepository.findByProjectId(projectId)
            .stream().findFirst()
            .orElseThrow(() -> new NotFoundException("Gitlab project not found")).getGitlabId();


        String publicKey = "";
        try {
            publicKey = new String(Files.readAllBytes(Paths.get(publicKeyPath)));
        } catch (IOException e) {
            log.error("Error reading public key", e);
            return false;
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", "mdcpps_epm");
        requestBody.put("key", publicKey);
        requestBody.put("can_push", true);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, httpEntity.getHeaders());

        ResponseEntity<String> responseEntity = restTemplate.exchange(
            projectsCreateUrl + "/" + projectIdInGitlab + "/deploy_keys",
            HttpMethod.POST, requestEntity, String.class);

        return responseEntity.getStatusCode().is2xxSuccessful();
    }

    @Override
    public boolean resetGitlabProject(Long projectId, long userId) {

        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        Long rootGroupId = project.getExternalToolsData().getRootGroupId();

        Long creatorUserId = project.getExternalToolsData().getCreatorUserId();

        List<GitlabProject> gitlabProject = this.gitlabProjectRepository.findByProjectId(projectId);
        this.gitlabProjectRepository.deleteAll(gitlabProject);
        project.setGitlabProject(null);

        this.gitlabProjectRepository.flush();

        Set<WorkSpace> workSpaces = project.getWorkSpaces();
        for (WorkSpace workSpace : workSpaces) {
            Set<GitlabSubgroup> gitlabSubgroup = workSpace.getGitlabSubgroups();
            this.gitlabSubgroupRepository.deleteAll(gitlabSubgroup);
            workSpace.setGitlabSubgroups(new HashSet<>());
        }

        this.projectRepository.save(project);

        List<Long> subprojects = getAllSubprojects(rootGroupId, userId);
        List<Long> subgroups = getAllSubgroups(rootGroupId, userId);
        List<Long> billableMembers = getAllBillableMembers(rootGroupId, userId);

        if (!removeAllSubprojects(userId, subprojects)) {
            return false;
        }

        if (!removeAllSubgroups(userId, subgroups)) {
            return false;
        }

        return removeAllBillableMembers(userId, billableMembers, rootGroupId, creatorUserId);
    }

    @Override
    public String getGitlabUrl(Long groupId, long userId) {
        HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(groupsGitlabUrl + groupId);

        String urlWithParams = uriBuilder.toUriString();

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.GET, httpEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            return jsonNode.get("web_url").asText();
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
            return "";
        }
    }

    @Override
    public String getRootGroupUrl(Long projectId, long userId) {

        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(groupsGitlabUrl + project.getExternalToolsData().getRootGroupId());

        String urlWithParams = uriBuilder.toUriString();

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.GET, httpEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            return jsonNode.get("path").asText();
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
            return "";
        }
    }

    @Override
    public String getProjectUrl(Long projectId, long userId) {
        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        Long projectIdInGitlab = project.getGitlabProject().getGitlabId();

        HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(projectsCreateUrl + "/" + projectIdInGitlab);
        String urlWithParams = uriBuilder.toUriString();

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.GET, httpEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            return jsonNode.get("name").asText();
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
            return "";
        }
    }

    @Override
    public boolean uploadRepositoryFiles(Long projectId, String tempDirName, long userId) {

        String rootGroupUrl = getRootGroupUrl(projectId, userId);
        String projectUrl = getProjectUrl(projectId, userId);

        String privateKeyPath = System.getProperty("user.home") + "/.ssh/id_rsa_mdcpps_epm";
        privateKeyPath = privateKeyPath.replace("\\", "/");

        String gitSshCommand = "ssh -i " + privateKeyPath;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "git", "init", "--initial-branch=main"
            );
            processBuilder.directory(new File(tempDirName));
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Error initializing git repository", e);
            return false;
        }

        try {
            String command = "git remote add origin git@gitlab.com:" + rootGroupUrl + "/" + projectUrl + ".git";
            ProcessBuilder processBuilder = new ProcessBuilder(
                command.split(" "));
            processBuilder.directory(new File(tempDirName));
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Error adding remote origin", e);
            return false;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "git", "add", "."
            );
            processBuilder.directory(new File(tempDirName));
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Error adding files to git", e);
            return false;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "git", "commit", "-m", "Initial commit"
            );
            processBuilder.directory(new File(tempDirName));
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Error committing files to git", e);
            return false;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "git", "push", "--set-upstream", "origin", "main"
            );
            processBuilder.environment().put("GIT_SSH_COMMAND", gitSshCommand);
            processBuilder.directory(new File(tempDirName));
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Error pushing files to git", e);
            return false;
        }

        return true;
    }

    @Override
    public boolean updateRepositoryFiles(Long projectId, String tempDirName, long userId) {

        String privateKeyPath = System.getProperty("user.home") + "/.ssh/id_rsa_mdcpps_epm";
        privateKeyPath = privateKeyPath.replace("\\", "/");

        String gitSshCommand = "ssh -i " + privateKeyPath;

        // Verify if the private key exists
        if (!new File(privateKeyPath).exists()) {
            this.addGitlabDeployKey(projectId, userId);
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "git", "add", "."
            );
            processBuilder.directory(new File(tempDirName));
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Error adding files to git", e);
            return false;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "git", "commit", "-m", "Update files"
            );
            processBuilder.directory(new File(tempDirName));
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Error committing files to git", e);
            return false;
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                "git", "push"
            );
            processBuilder.environment().put("GIT_SSH_COMMAND", gitSshCommand);
            processBuilder.directory(new File(tempDirName));
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Error pushing files to git", e);
            return false;
        }

        return true;
    }

    @Override
    public boolean createGitlabProjectLabels(Long projectId, long userId) {
        List<String> labels = Arrays.asList("Change Request (CR)", "Review Task (RT)", "WiP (CR)", "Review (CR)", "Review (RT)", "Requested (RT)", "Rework (RT)", "Escalate (RT)");
        List<String> colors = Arrays.asList("red", "green", "blue", "yellow", "purple", "orange", "pink", "gray");

        Long projectIdInGitlab = this.gitlabProjectRepository.findByProjectId(projectId)
            .stream().findFirst()
            .orElseThrow(() -> new NotFoundException("Gitlab project not found")).getGitlabId();

        for (int i = 0; i < labels.size(); i++) {
            HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(projectsCreateUrl + "/" + projectIdInGitlab + "/labels");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", labels.get(i));
            requestBody.put("color", colors.get(i));

            String urlWithParams = uriBuilder.encode().toUriString();

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, httpEntity.getHeaders());

            ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.POST, requestEntity, String.class);

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean createOnlyNewGitlabSubgroups(Long projectId, long userId) {
        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        Long parentGroupId = project.getExternalToolsData().getRootGroupId();

        for (WorkSpace workSpace : project.getWorkSpaces()) {
            if (!workSpace.getIsCommon()) {
                if (!workSpace.getGitlabSubgroups().isEmpty()) {
                    continue;
                }

                log.info("Creating subgroups for workspace: " + workSpace.getName());
                if (!createGitlabSubgroup(false, workSpace.getId(), workSpace.getName(), userId, parentGroupId)) {
                    return false;
                }
                if (!createGitlabSubgroup(true, workSpace.getId(), workSpace.getName() + "_reviewers", userId, parentGroupId)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean editGitlabProjectUsers(Long projectId, long userId) {
        // First delete all users from the project
        log.info("Removing all users from the project");
        removeAllUsersFromGitlabProject(projectId, userId);

        // Then add all users to the project
        return addGitlabProjectUsers(projectId, userId);
    }

    @Override
    public boolean editGitlabSubgroupsUsers(Long projectId, long userId) {

        // First delete all users from the subgroups
        log.info("Removing all users from the subgroups");
        removeALlUsersFromAllSubgroups(projectId, userId);

        // Then add all users to the subgroups
        return addGitlabSubgroupsUsers(projectId, userId);
    }

    private void removeAllUsersFromGitlabProject(Long projectId, Long userId) {
        GitlabProject gitlabProject = this.gitlabProjectRepository.findByProjectId(projectId)
            .stream().findFirst()
            .orElseThrow(() -> new NotFoundException("Gitlab project not found"));

        List<Long> projectMembers = getAllProjectMembers(gitlabProject.getGitlabId(), userId);
        removeAllProjectMembers(userId, projectMembers, gitlabProject.getGitlabId(), userId);
    }

    private List<Long> getAllSubprojects(Long rootGroupId, Long userId) {
        List<Long> subprojects = new ArrayList<>();

        HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(groupsGitlabUrl + rootGroupId + "/projects");

        String urlWithParams = uriBuilder.toUriString();

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.GET, httpEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            for (JsonNode node : jsonNode) {
                subprojects.add(node.get("id").asLong());
            }
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
            return Collections.emptyList();
        }

        return subprojects;
    }

    private List<Long> getAllSubgroups(Long rootGroupId, Long userId) {
        List<Long> subgroups = new ArrayList<>();

        HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(groupsGitlabUrl + rootGroupId + "/subgroups");

        String urlWithParams = uriBuilder.toUriString();

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.GET, httpEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            for (JsonNode node : jsonNode) {
                subgroups.add(node.get("id").asLong());
            }
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
            return Collections.emptyList();
        }

        return subgroups;
    }

    private List<Long> getAllBillableMembers(Long groupId, Long userId) {
        List<Long> billableMembers = new ArrayList<>();

        HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(groupsGitlabUrl + groupId + "/billable_members");

        String urlWithParams = uriBuilder.toUriString();

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.GET, httpEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            for (JsonNode node : jsonNode) {
                billableMembers.add(node.get("id").asLong());
            }
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
            return Collections.emptyList();
        }

        return billableMembers;
    }

    private boolean removeAllSubprojects(Long userId, List<Long> subprojectIdList) {
        for (Long subprojectId : subprojectIdList) {
            HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                projectsCreateUrl + "/" + subprojectId,
                HttpMethod.DELETE, httpEntity, String.class);

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                return false;
            }
        }
        return true;
    }

    private boolean removeAllSubgroups(Long userId, List<Long> subgroupList) {
        for (Long subgroup : subgroupList) {
            HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                groupsGitlabUrl + subgroup,
                HttpMethod.DELETE, httpEntity, String.class);

            if (!responseEntity.getStatusCode().is2xxSuccessful()) {
                return false;
            }
        }
        return true;
    }

    private boolean removeAllBillableMembers(Long userId, List<Long> billableMembers, Long groupId, Long creatorUserId) {
        for (Long billableMember : billableMembers) {
            if (billableMember.equals(creatorUserId)) {
                continue;
            }

            HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

            log.info("Removing billable member: " + billableMember);

            ResponseEntity<String> responseEntity = null;
            try {
                responseEntity = restTemplate.exchange(
                    groupsGitlabUrl + groupId + "/billable_members/" + billableMember,
                    HttpMethod.DELETE, httpEntity, String.class);
            } catch (Exception e) {
                log.error("Error removing billable member", e);
            }

            if (responseEntity != null && !responseEntity.getStatusCode().is2xxSuccessful()) {
                return false;
            }
        }
        return true;
    }

    private boolean removeAllProjectMembers(Long userId, List<Long> projectMembers, Long projectId, Long creatorUserId) {
        for (Long projectMember : projectMembers) {
            if (projectMember.equals(creatorUserId)) {
                continue;
            }

            HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

            log.info("Removing project member: " + projectMember);

            ResponseEntity<String> responseEntity = null;
            try {
                responseEntity = restTemplate.exchange(
                    projectsCreateUrl + "/" + projectId + "/members/" + projectMember,
                    HttpMethod.DELETE, httpEntity, String.class);
            } catch (Exception e) {
                log.error("Error removing project member", e);
            }

            if (responseEntity != null && !responseEntity.getStatusCode().is2xxSuccessful()) {
                return false;
            }
        }
        return true;
    }

    private boolean removeAllSubgroupMembers(Long userId, List<Long> subgroupMembers, Long groupId, Long creatorUserId) {
        for (Long subgroupMember : subgroupMembers) {
            if (subgroupMember.equals(creatorUserId)) {
                continue;
            }

            HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

            log.info("Removing subgroup member: " + subgroupMember);

            ResponseEntity<String> responseEntity = null;
            try {
                responseEntity = restTemplate.exchange(
                    groupsGitlabUrl + groupId + "/members/" + subgroupMember,
                    HttpMethod.DELETE, httpEntity, String.class);
            } catch (Exception e) {
                log.error("Error removing subgroup member", e);
            }

            if (responseEntity != null && !responseEntity.getStatusCode().is2xxSuccessful()) {
                return false;
            }
        }
        return true;
    }

    private boolean removeALlUsersFromAllSubgroups(Long projectId, long userId) {
        Project project = this.projectRepository.findById(projectId)
            .orElseThrow(() -> new NotFoundException("Project not found"));

        Long creatorUserId = project.getExternalToolsData().getCreatorUserId();

        for (WorkSpace workSpace : project.getWorkSpaces()) {
            Long groupId = this.gitlabSubgroupRepository.findNonReviewerSubgroupByWorkspaceId(workSpace.getId())
                .orElseThrow(() -> new NotFoundException("Gitlab subgroup not found")).getGitlabId();
            List<Long> subgroupMembers = getALlSubgroupMembers(groupId, userId);
            if (!removeAllSubgroupMembers(userId, subgroupMembers, groupId, creatorUserId)) {
                return false;
            }

            Long reviewerGroupId = this.gitlabSubgroupRepository.findReviewerSubgroupByWorkspaceId(workSpace.getId())
                .orElseThrow(() -> new NotFoundException("Gitlab subgroup not found")).getGitlabId();
            List<Long> reviewerSubgroupMembers = getALlSubgroupMembers(reviewerGroupId, userId);
            if (!removeAllSubgroupMembers(userId, reviewerSubgroupMembers, reviewerGroupId, creatorUserId)) {
                return false;
            }
        }

        return true;
    }

    private boolean addUserToProject(Long projectId, Long userId, long requesterId, boolean isAdmin) {
        HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(requesterId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(projectsCreateUrl + "/" + projectId + "/members")
            .queryParam("user_id", userId)
            .queryParam("access_level", isAdmin ? 40 : 30);

        String urlWithParams = uriBuilder.toUriString();

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.POST, httpEntity, String.class);

        return responseEntity.getStatusCode().is2xxSuccessful();
    }

    private boolean addUserToGroup(Long groupId, Long userId, long requesterId, boolean isAdmin) {
        HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(requesterId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(groupsGitlabUrl + groupId + "/members")
            .queryParam("user_id", userId)
            .queryParam("access_level", isAdmin ? 40 : 30);

        String urlWithParams = uriBuilder.toUriString();

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.POST, httpEntity, String.class);

        return responseEntity.getStatusCode().is2xxSuccessful();
    }

    private boolean createGitlabSubgroup(boolean isReviewerGroup, Long workspaceId, String groupName, long userId, long parentId) {
        HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(subgroupsCreateUrl)
            .queryParam("name", this.createSlug(groupName))
            .queryParam("path", this.createSlug(groupName))
            .queryParam("parent_id", parentId)
            .queryParam("visibility", "private");

        String urlWithParams = uriBuilder.toUriString();

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.POST, httpEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        WorkSpace workSpace;
        try {
            workSpace = this.workSpaceRepository.findById(workspaceId)
                .orElseThrow(() -> new NotFoundException("Workspace not found"));
        } catch (Exception e) {
            log.error("Error getting workspace", e);
            return false;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            GitlabSubgroup gitlabSubgroup = GitlabSubgroup.builder()
                .workspace(workSpace)
                .gitlabId(jsonNode.get("id").asLong())
                .isReviewerGroup(isReviewerGroup)
                .build();
            this.gitlabSubgroupRepository.save(gitlabSubgroup);
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
            return false;
        }

        return true;
    }

    @Override
    public String createSlug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    @Override
    public HttpEntity<String> refreshAccessTokenAndGetHttpEntity(long userId) {

        boolean validToken = this.userService.checkForNonExpiredToken(userId);
        if (!validToken) {
            this.userService.refreshGitlabToken(userId);
        }

        String accessToken = this.userService.getUserEntity(userId).getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        return new HttpEntity<>(headers);
    }

    private List<Long> getAllProjectMembers(Long projectId, Long userId) {
        List<Long> projectMembers = new ArrayList<>();

        HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(projectsCreateUrl + "/" + projectId + "/members");

        String urlWithParams = uriBuilder.toUriString();

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.GET, httpEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            for (JsonNode node : jsonNode) {
                projectMembers.add(node.get("id").asLong());
            }
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
            return Collections.emptyList();
        }

        return projectMembers;
    }

    private List<Long> getALlSubgroupMembers(Long groupId, Long userId) {
        List<Long> subgroupMembers = new ArrayList<>();

        HttpEntity<String> httpEntity = refreshAccessTokenAndGetHttpEntity(userId);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(groupsGitlabUrl + groupId + "/members");

        String urlWithParams = uriBuilder.toUriString();

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.GET, httpEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            for (JsonNode node : jsonNode) {
                subgroupMembers.add(node.get("id").asLong());
            }
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
            return Collections.emptyList();
        }

        return subgroupMembers;
    }




}
