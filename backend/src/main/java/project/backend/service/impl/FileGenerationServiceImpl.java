package project.backend.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import project.backend.dto.CppsProjectJSONExportDto;
import project.backend.dto.GitlabIssuePrTemplateDto;
import project.backend.dto.UserJSONExportDto;
import project.backend.entity.GitlabPrIssueTemplate;
import project.backend.entity.Project;
import project.backend.entity.WorkSpace;
import project.backend.exception.NotFoundException;
import project.backend.mapper.ProjectMapper;
import project.backend.repository.ProjectRepository;
import project.backend.service.ExampleService;
import project.backend.service.FileGenerationService;
import project.backend.service.GitlabService;
import project.backend.service.ProjectService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileGenerationServiceImpl implements FileGenerationService {

    private final ProjectMapper projectMapper;
    private final GitlabService gitlabService;

    @Override
    public String generateFileStructure(Project project){

        UUID randomUUID = UUID.randomUUID();
        Path tempDir;

        try {
            tempDir = Files.createTempDirectory(project.getName() + "_" + randomUUID.toString().replace("-", ""));
        } catch (IOException e) {
            log.error("Error creating temporary directory", e);
            return null;
        }

        String path = tempDir.toString();

        // Generate all VCS files (.gitignore, .gitlab folder)
        generateGitignoreFile(path, project);
        generateGitlabMergeRequestTemplateFiles(path, project);
        generateGitlabIssueTemplateFiles(path, project);

        // Generate GitOps files (gitlab-ci.yml, mdmcpps-cli.jar)
        generateGitlabCiYmlFile(path, project);
        generateMdmcppsCliJar(path);

        // Generate documentation files (README.md, docs folder)
        generateReadmeFile(path, project);

        // Generate configuration file
        generateConfigFile(path, project);

        // Generate files related to dependency management (mdcpps.json, libs folder)
        generateDependencyManagementFiles(path, project);

        // Generate workspace content
        generateCommonWorkspaceContent(path, project);
        generateDomainWorkspaceContent(path, project);

        // Generate test related structure
        generateTestStructure(path, project);

        return path;
    }

    @Override
    public String updateFileStructure(Project project, long userId) {
        UUID randomUUID = UUID.randomUUID();
        Path tempDir;

        try {
            tempDir = Files.createTempDirectory(project.getName() + "_" + randomUUID.toString().replace("-", ""));
        } catch (IOException e) {
            log.error("Error creating temporary directory", e);
            return null;
        }

        String path = tempDir.toString();

        cloneGitlabRepository(path, project, userId);
        removeGitlabIssueTemplateFiles(path);
        removeGitlabMergeRequestTemplateFiles(path);

        generateGitlabMergeRequestTemplateFiles(path, project);
        generateGitlabIssueTemplateFiles(path, project);

        generateMissingDomainWorkspaceFiles(path, project);

        deleteConfigFile(path);
        generateConfigFile(path, project);

        return path;
    }

    @Override
    public List<String> performFileStructureCheck(Project project, long userId) {
        UUID randomUUID = UUID.randomUUID();
        Path tempDir;

        try {
            tempDir = Files.createTempDirectory(project.getName() + "_" + randomUUID.toString().replace("-", ""));
        } catch (IOException e) {
            log.error("Error creating temporary directory", e);
            return null;
        }

        String path = tempDir.toString();

        cloneGitlabRepository(path, project, userId);

        List<String> inconsistencies = new ArrayList<>();
        // Check if all files are present
        if (!checkGitignoreFile(path)) {
            inconsistencies.add(".gitignore file is missing");
        }

        if (!checkGitlabMergeRequestTemplateDirectory(path)) {
            inconsistencies.add("GitLab merge request template directory is missing");
        }

        if (!checkGitlabIssueTemplateDirectory(path)) {
            inconsistencies.add("GitLab issue template directory is missing");
        }

        if (!checkGitlabCiYmlFile(path)) {
            inconsistencies.add(".gitlab-ci.yml file is missing");
        }

        if (!checkMdmcppsCliJar(path)) {
            inconsistencies.add("mdmcpps-cli.jar file is missing");
        }

        if (!checkReadmeFile(path)) {
            inconsistencies.add("README.md file is missing");
        }

        if (!checkConfigFile(path, project)) {
            inconsistencies.add("config.json file is missing or incorrect");
        }

        if (!checkDependencyManagementFiles(path)) {
            inconsistencies.add("Dependency management files are missing or incorrect");
        }

        if (!checkCommonWorkspaceContent(path, project)) {
            inconsistencies.add("Common workspace content is missing or incorrect");
        }

        inconsistencies.addAll(
            checkDomainWorkspaceContent(path, project)
        );

        deleteClonedRepository(path);

        return inconsistencies;
    }

    private void cloneGitlabRepository(String tempDir, Project project, long userId) {
        String rootGroupUrl = gitlabService.getRootGroupUrl(project.getId(), userId);
        String projectUrl = gitlabService.getProjectUrl(project.getId(), userId);

        log.info(tempDir);

        String privateKeyPath = System.getProperty("user.home") + "/.ssh/id_rsa_mdcpps_epm";
        privateKeyPath = privateKeyPath.replace("\\", "/");

        // Verify if the private key exists
        File privateKeyFile = new File(privateKeyPath);
        if (!privateKeyFile.exists()) {
            this.gitlabService.addGitlabDeployKey(project.getId(), userId);
        }

        String gitSshCommand = "ssh -i " + privateKeyPath;

        try {
            String command = "git clone git@gitlab.com:" + rootGroupUrl + "/" + projectUrl + ".git .";
            log.info(command);
            ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
            processBuilder.directory(new File(tempDir));
            processBuilder.environment().put("GIT_SSH_COMMAND", gitSshCommand);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error("Error pulling from GitLab repository", e);
        }
    }

    private void generateReadmeFile(String tempDir, Project project) {
        // Generate README.md file
        Path readmeFile = Path.of(tempDir, "README.md");
        String toWrite = "# " + project.getName() + "\n\n" + project.getDescription() + " v. " + project.getVersion();
        try {
            Files.writeString(readmeFile, toWrite);
        } catch (IOException e) {
            log.error("Error writing README.md file", e);
        }
    }

    private void generateGitignoreFile(String tempDir, Project project) {
        // Generate .gitignore file
        Path gitignoreFile = Path.of(tempDir, ".gitignore");
        String toWrite = ".env\n";
        try {
            Files.writeString(gitignoreFile, toWrite);
        } catch (IOException e) {
            log.error("Error writing .gitignore file", e);
        }
    }

    private void generateConfigFile(String tempDir, Project project) {

        Path configDir = Path.of(tempDir, ".mdcppsepm");
        // Check if .mdcppsepm directory exists
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectory(configDir);
            } catch (IOException e) {
                log.error("Error creating config directory", e);
            }
        }

        Path configFile = Path.of(tempDir, ".mdcppsepm", "config.json");

        CppsProjectJSONExportDto projectJSONExportDto = this.projectMapper.mapProjectEntityToCppsProjectJSONExportDto(project);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        String formattedJson;

        try {
            formattedJson = objectMapper.writeValueAsString(projectJSONExportDto);
        } catch (JsonProcessingException e) {
            log.error("Error converting project to JSON", e);
            return;
        }

        try {
            Files.writeString(configFile, formattedJson);
        } catch (IOException e) {
            log.error("Error writing config file", e);
        }
    }

    private void generateGitlabCiYmlFile(String tempDir, Project project) {
        // Generate .gitlab-ci.yml file
        Path gitlabCiYmlFile = Path.of(tempDir, ".gitlab-ci.yml");
        Path referenceGitlabCiYmlFile = Path.of("src/main/resources/repository/reference_gitlab_ci.yml");

        try {
            Files.copy(referenceGitlabCiYmlFile, gitlabCiYmlFile);
        } catch (IOException e) {
            log.error("Error creating .gitlab-ci.yml file", e);
        }
    }

    private void generateGitlabMergeRequestTemplateFiles(String tempDir, Project project) {

        Path gitlabDir = Path.of(tempDir, ".gitlab");

        // Check if .gitlab directory exists
        if (!Files.exists(gitlabDir)) {
            try {
                Files.createDirectory(gitlabDir);
            } catch (IOException e) {
                log.error("Error creating .gitlab directory", e);
            }
        }

        Path mergeRequestTemplatesDir = Path.of(tempDir, ".gitlab/merge_request_templates");
        try {
            Files.createDirectory(mergeRequestTemplatesDir);
        } catch (IOException e) {
            log.error("Error creating .gitlab/merge_request_templates directory", e);
        }

        // Generate .gitlab/merge_request_templates/default.md file
        int counter = 0;
        for (GitlabPrIssueTemplate template : project.getGitlabPrIssueTemplates()) {
            if (template.getIsPrTemplate()) {
                counter++;
                Path gitlabMergeRequestTemplateFile = Path.of(tempDir, ".gitlab/merge_request_templates/" + createSlug(template.getName()) + ".md");
                String toWrite = template.getContent();
                try {
                    Files.writeString(gitlabMergeRequestTemplateFile, toWrite);
                } catch (IOException e) {
                    log.error("Error writing .gitlab/merge_request_templates/" + createSlug(template.getName()) + ".md file", e);
                }
            }
        }

        if (counter == 0) {
            Path gitkeepFile = Path.of(tempDir, ".gitlab/merge_request_templates/.gitkeep");
            try {
                Files.writeString(gitkeepFile, "");
            } catch (IOException e) {
                log.error("Error writing .gitkeep file", e);
            }
        }
    }

    private void generateMdmcppsCliJar(String tempDir) {
        Path mdmccpsCliJar = Path.of(tempDir, "mdmcpps-cli.jar");
        Path referenceMdmccpsCliJar = Path.of("src/main/resources/repository/mdmcpps-cli.jar");

        try {
            Files.copy(referenceMdmccpsCliJar, mdmccpsCliJar);
        } catch (IOException e) {
            log.error("Error creating mdmccps-cli.jar file", e);
        }
    }

    private void removeGitlabMergeRequestTemplateFiles(String tempDir) {
        Path mergeRequestTemplatesDir = Path.of(tempDir, ".gitlab/merge_request_templates");
        if (!Files.exists(mergeRequestTemplatesDir)) {
            return;
        }
        try {
            Files.walk(mergeRequestTemplatesDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        log.info("Deleting file: " + path);
                        Files.delete(path);
                    } catch (IOException e) {
                        log.error("Error deleting file: " + path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Error deleting .gitlab/merge_request_templates directory", e);
        }
    }

    private void generateGitlabIssueTemplateFiles(String tempDir, Project project) {

        Path issueTemplatesDir = Path.of(tempDir, ".gitlab/issue_templates");
        try {
            Files.createDirectory(issueTemplatesDir);
        } catch (IOException e) {
            log.error("Error creating .gitlab/issue_templates directory", e);
        }

        // Generate .gitlab/issue_templates/default.md file
        int counter = 0;
        for (GitlabPrIssueTemplate template : project.getGitlabPrIssueTemplates()) {
            if (!template.getIsPrTemplate()) {
                counter++;
                Path gitlabIssueTemplateFile = Path.of(tempDir, ".gitlab/issue_templates/" + createSlug(template.getName()) + ".md");
                String toWrite = template.getContent();
                try {
                    Files.writeString(gitlabIssueTemplateFile, toWrite);
                } catch (IOException e) {
                    log.error("Error writing .gitlab/issue_templates/" + createSlug(template.getName()) + ".md file", e);
                }
            }
        }

        if (counter == 0) {
            Path gitkeepFile = Path.of(tempDir, ".gitlab/issue_templates/.gitkeep");
            try {
                Files.writeString(gitkeepFile, "");
            } catch (IOException e) {
                log.error("Error writing .gitkeep file", e);
            }
        }
    }

    private void removeGitlabIssueTemplateFiles(String tempDir) {
        Path issueTemplatesDir = Path.of(tempDir, ".gitlab/issue_templates");
        if (!Files.exists(issueTemplatesDir)) {
            return;
        }
        try {
            Files.walk(issueTemplatesDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        log.info("Deleting file: " + path);
                        Files.delete(path);
                    } catch (IOException e) {
                        log.error("Error deleting file: " + path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Error deleting .gitlab/issue_templates directory", e);
        }
    }

    private void generateCommonWorkspaceContent(String tempDir, Project project) {
        WorkSpace commonWorkspace = project.getWorkSpaces().stream().filter(WorkSpace::getIsCommon).findFirst().orElse(null);

        if (commonWorkspace == null) {
            log.error("Common workspace not found");
            return;
        }

        String commonWorkspaceDirName = createSlug(commonWorkspace.getName());

        if (!commonWorkspaceDirName.endsWith("_workspace")) {
            commonWorkspaceDirName += "_workspace";
        }

        Path commonWorkspaceDir = Path.of(tempDir, commonWorkspaceDirName);

        try {
            Files.createDirectory(commonWorkspaceDir);
        } catch (IOException e) {
            log.error("Error creating common_workspace directory", e);
        }

        // Create models and impl directories
        Path modelsDir = Path.of(tempDir, commonWorkspaceDirName, "models");
        if (!Files.exists(modelsDir)) {
            try {
                Files.createDirectory(modelsDir);
            } catch (IOException e) {
                log.error("Error creating models directory", e);
            }
        }

        Path implDir = Path.of(tempDir, commonWorkspaceDirName, "impl");
        if (!Files.exists(implDir)) {
            try {
                Files.createDirectory(implDir);
            } catch (IOException e) {
                log.error("Error creating impl directory", e);
            }
        }

        Path commonWorkspaceFile = Path.of(tempDir, commonWorkspaceDirName, "models", createSlug(commonWorkspace.getName()) + ".ccg");
        String toWrite = "";
        try {
            Files.writeString(commonWorkspaceFile, toWrite);
        } catch (IOException e) {
            log.error("Error writing common_workspace file", e);
        }

        Path gitkeepFile = Path.of(tempDir, commonWorkspaceDirName, "impl", ".gitkeep");
        toWrite = "";
        try {
            Files.writeString(gitkeepFile, toWrite);
        } catch (IOException e) {
            log.error("Error writing .gitkeep file", e);
        }

        Path commonWorkspaceTestDir = Path.of(tempDir, commonWorkspaceDirName, "test");
        if (!Files.exists(commonWorkspaceTestDir)) {
            try {
                Files.createDirectory(commonWorkspaceTestDir);
            } catch (IOException e) {
                log.error("Error creating test directory", e);
            }
        }

        Path commonWorkspaceTestGitkeepFile = Path.of(tempDir, commonWorkspaceDirName, "test", ".gitkeep");
        toWrite = "";
        try {
            Files.writeString(commonWorkspaceTestGitkeepFile, toWrite);
        } catch (IOException e) {
            log.error("Error writing .gitkeep file", e);
        }


        this.generateDependencyManagementFiles(String.valueOf(commonWorkspaceDir), project);
    }

    private void generateDomainWorkspaceContent(String tempDir, Project project) {
        for (WorkSpace domainWorkspace : project.getWorkSpaces()) {
            if (!domainWorkspace.getIsCommon()) {
                String domainWorkspaceDirName = createSlug(domainWorkspace.getName());

                if (!domainWorkspaceDirName.endsWith("_workspace")) {
                    domainWorkspaceDirName += "_workspace";
                }

                Path domainWorkspaceDir = Path.of(tempDir, domainWorkspaceDirName);

                try {
                    Files.createDirectory(domainWorkspaceDir);
                } catch (IOException e) {
                    log.error("Error creating domain_workspace directory", e);
                }

                Path modelsDir = Path.of(tempDir, domainWorkspaceDirName, "models");
                if (!Files.exists(modelsDir)) {
                    try {
                        Files.createDirectory(modelsDir);
                    } catch (IOException e) {
                        log.error("Error creating models directory", e);
                    }
                }

                Path implDir = Path.of(tempDir, domainWorkspaceDirName, "impl");
                if (!Files.exists(implDir)) {
                    try {
                        Files.createDirectory(implDir);
                    } catch (IOException e) {
                        log.error("Error creating impl directory", e);
                    }
                }

                Path domainWorkspaceFile = Path.of(tempDir, domainWorkspaceDirName, "models", createSlug(domainWorkspace.getName()) + ".cg");
                String toWrite = "";
                try {
                    Files.writeString(domainWorkspaceFile, toWrite);
                } catch (IOException e) {
                    log.error("Error writing domain_workspace file", e);
                }

                Path gitkeepFile = Path.of(tempDir, domainWorkspaceDirName, "impl", ".gitkeep");
                toWrite = "";
                try {
                    Files.writeString(gitkeepFile, toWrite);
                } catch (IOException e) {
                    log.error("Error writing .gitkeep file", e);
                }

                Path domainWorkspaceTestDir = Path.of(tempDir, domainWorkspaceDirName, "test");
                if (!Files.exists(domainWorkspaceTestDir)) {
                    try {
                        Files.createDirectory(domainWorkspaceTestDir);
                    } catch (IOException e) {
                        log.error("Error creating test directory", e);
                    }
                }

                Path domainWorkspaceTestGitkeepFile = Path.of(tempDir, domainWorkspaceDirName, "test", ".gitkeep");
                toWrite = "";
                try {
                    Files.writeString(domainWorkspaceTestGitkeepFile, toWrite);
                } catch (IOException e) {
                    log.error("Error writing .gitkeep file", e);
                }

                this.generateDependencyManagementFiles(String.valueOf(domainWorkspaceDir), project);
            }
        }
    }

    private void generateMissingDomainWorkspaceFiles(String tempDir, Project project) {
        for (WorkSpace domainWorkspace : project.getWorkSpaces()) {
            if (!domainWorkspace.getIsCommon()) {
                String domainWorkspaceDirName = createSlug(domainWorkspace.getName());

                if (!domainWorkspaceDirName.endsWith("_workspace")) {
                    domainWorkspaceDirName += "_workspace";
                }

                Path domainWorkspaceDir = Path.of(tempDir, domainWorkspaceDirName);

                if (Files.exists(domainWorkspaceDir)) {
                    continue;
                }

                try {
                    Files.createDirectory(domainWorkspaceDir);
                } catch (IOException e) {
                    log.error("Error creating domain_workspace directory", e);
                }

                Path modelsDir = Path.of(tempDir, domainWorkspaceDirName, "models");
                if (!Files.exists(modelsDir)) {
                    try {
                        Files.createDirectory(modelsDir);
                    } catch (IOException e) {
                        log.error("Error creating models directory", e);
                    }
                }

                Path implDir = Path.of(tempDir, domainWorkspaceDirName, "impl");
                if (!Files.exists(implDir)) {
                    try {
                        Files.createDirectory(implDir);
                    } catch (IOException e) {
                        log.error("Error creating impl directory", e);
                    }
                }

                Path domainWorkspaceFile = Path.of(tempDir, domainWorkspaceDirName, "models", createSlug(domainWorkspace.getName()) + ".cg");
                if (!Files.exists(domainWorkspaceFile)) {
                    String toWrite = "";
                    try {
                        Files.writeString(domainWorkspaceFile, toWrite);
                    } catch (IOException e) {
                        log.error("Error writing domain_workspace file", e);
                    }
                }

                Path gitkeepFile = Path.of(tempDir, domainWorkspaceDirName, "impl", ".gitkeep");
                if (!Files.exists(gitkeepFile)) {
                    String toWrite = "";
                    try {
                        Files.writeString(gitkeepFile, toWrite);
                    } catch (IOException e) {
                        log.error("Error writing .gitkeep file", e);
                    }
                }

                Path domainWorkspaceTestDir = Path.of(tempDir, domainWorkspaceDirName, "test");
                if (!Files.exists(domainWorkspaceTestDir)) {
                    try {
                        Files.createDirectory(domainWorkspaceTestDir);
                    } catch (IOException e) {
                        log.error("Error creating test directory", e);
                    }
                }

                Path domainWorkspaceTestGitkeepFile = Path.of(tempDir, domainWorkspaceDirName, "test", ".gitkeep");
                if (!Files.exists(domainWorkspaceTestGitkeepFile)) {
                    String toWrite = "";
                    try {
                        Files.writeString(domainWorkspaceTestGitkeepFile, toWrite);
                    } catch (IOException e) {
                        log.error("Error writing .gitkeep file", e);
                    }
                }

                this.generateDependencyManagementFiles(String.valueOf(domainWorkspaceDir), project);
            }
        }
    }

    private String createSlug(String input){
        return input.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    private void deleteConfigFile(String tempDir) {
        Path configFile = Path.of(tempDir, ".mdcppsepm", "config.json");
        if (Files.exists(configFile)) {
            try {
                Files.delete(configFile);
            } catch (IOException e) {
                log.error("Error deleting config file", e);
            }
        }
    }

    private void generateDependencyManagementFiles(String tempDir, Project project) {
        Path libsDir = Path.of(tempDir, "libs");
        if (!Files.exists(libsDir)) {
            try {
                Files.createDirectory(libsDir);
            } catch (IOException e) {
                log.error("Error creating libs directory", e);
            }
        }

        Path gitkeepFile = Path.of(tempDir, "libs", ".gitkeep");
        String toWrite = "";
        try {
            Files.writeString(gitkeepFile, toWrite);
        } catch (IOException e) {
            log.error("Error writing .gitkeep file", e);
        }

        Path mdcppsJsonFile = Path.of(tempDir, "mdcpps.json");
        toWrite = "{\n" +
                "  \"dependencies\": []\n" +
                "}";
        try {
            Files.writeString(mdcppsJsonFile, toWrite);
        } catch (IOException e) {
            log.error("Error writing mdcpps.json file", e);
        }
    }

    private void generateSourceRootFolder(String tempDir, Project project) {
        Path srcDir = Path.of(tempDir, "src");
        if (!Files.exists(srcDir)) {
            try {
                Files.createDirectory(srcDir);
            } catch (IOException e) {
                log.error("Error creating src directory", e);
            }
        }
    }

    private void generateTestStructure(String tempDir, Project project) {
        Path unitDir = Path.of(tempDir, "src", "test", "unit");
        if (!Files.exists(unitDir)) {
            try {
                Files.createDirectory(unitDir);
            } catch (IOException e) {
                log.error("Error creating unit directory", e);
            }
        }

        Path integrationDir = Path.of(tempDir, "src", "test", "integration");
        if (!Files.exists(integrationDir)) {
            try {
                Files.createDirectory(integrationDir);
            } catch (IOException e) {
                log.error("Error creating integration directory", e);
            }
        }

        Path unitReadmeFile = Path.of(tempDir, "src", "test", "unit", "README.md");
        String unitReadmeContent = "# Unit Tests\n\nThis directory contains all unit tests for the project.";
        try {
            Files.writeString(unitReadmeFile, unitReadmeContent);
        } catch (IOException e) {
            log.error("Error writing unit tests README.md file", e);
        }

        Path integrationReadmeFile = Path.of(tempDir, "src", "test", "integration", "README.md");
        String integrationReadmeContent = "# Integration Tests\n\nThis directory contains all integration tests for the project.";
        try {
            Files.writeString(integrationReadmeFile, integrationReadmeContent);
        } catch (IOException e) {
            log.error("Error writing integration tests README.md file", e);
        }
    }

    private void deleteClonedRepository(String tempDir) {
        try {
            Files.walk(Path.of(tempDir))
                .sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        log.error("Error deleting file: " + path, e);
                    }
                });
        } catch (IOException e) {
            log.error("Error deleting cloned repository", e);
        }
    }

    private boolean checkGitignoreFile(String tempDir) {
        Path gitignoreFile = Path.of(tempDir, ".gitignore");
        return Files.exists(gitignoreFile);
    }

    private boolean checkGitlabMergeRequestTemplateDirectory(String tempDir) {
        Path mergeRequestTemplatesDir = Path.of(tempDir, ".gitlab/merge_request_templates");
        return Files.exists(mergeRequestTemplatesDir);
    }

    private boolean checkGitlabIssueTemplateDirectory(String tempDir) {
        Path issueTemplatesDir = Path.of(tempDir, ".gitlab/issue_templates");
        return Files.exists(issueTemplatesDir);
    }

    private boolean checkGitlabCiYmlFile(String tempDir) {
        Path gitlabCiYmlFile = Path.of(tempDir, ".gitlab-ci.yml");
        return Files.exists(gitlabCiYmlFile);
    }

    private boolean checkMdmcppsCliJar(String tempDir) {
        Path mdmcppsCliJar = Path.of(tempDir, "mdmcpps-cli.jar");
        return Files.exists(mdmcppsCliJar);
    }

    private boolean checkReadmeFile(String tempDir) {
        Path readmeFile = Path.of(tempDir, "README.md");
        return Files.exists(readmeFile);
    }

    private boolean checkDocsFolder(String tempDir) {
        Path docsDir = Path.of(tempDir, "docs");
        return Files.exists(docsDir);
    }

    private boolean checkConfigFile(String tempDir, Project project) {
        Path configFile = Path.of(tempDir, ".mdcppsepm", "config.json");
        if (!Files.exists(configFile)) {
            return false;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        CppsProjectJSONExportDto projectJSONExportDto;

        try {
            projectJSONExportDto = objectMapper.readValue(Files.readString(configFile), CppsProjectJSONExportDto.class);
        } catch (IOException e) {
            log.error("Error reading config file", e);
            return false;
        }

        CppsProjectJSONExportDto projectJSONExportDtoFromEntity = this.projectMapper.mapProjectEntityToCppsProjectJSONExportDto(project);

        projectJSONExportDto.getCommonWorkspace().getUsers().sort(Comparator.naturalOrder());
        projectJSONExportDtoFromEntity.getCommonWorkspace().getUsers().sort(Comparator.naturalOrder());

        projectJSONExportDto.getDomainWorkspaces().sort(Comparator.comparing(workspace -> workspace.getName().toLowerCase()));
        projectJSONExportDtoFromEntity.getDomainWorkspaces().sort(Comparator.comparing(workspace -> workspace.getName().toLowerCase()));

        projectJSONExportDto.getUsers().sort(Comparator.comparing(UserJSONExportDto::getGitlabId));
        projectJSONExportDtoFromEntity.getUsers().sort(Comparator.comparing(UserJSONExportDto::getGitlabId));

        projectJSONExportDto.getExternalToolsDetails().getMergeRequestTemplates().sort(Comparator.comparing(GitlabIssuePrTemplateDto::getName));
        projectJSONExportDtoFromEntity.getExternalToolsDetails().getMergeRequestTemplates().sort(Comparator.comparing(GitlabIssuePrTemplateDto::getName));

        projectJSONExportDto.getExternalToolsDetails().getIssueTemplates().sort(Comparator.comparing(GitlabIssuePrTemplateDto::getName));
        projectJSONExportDtoFromEntity.getExternalToolsDetails().getIssueTemplates().sort(Comparator.comparing(GitlabIssuePrTemplateDto::getName));

        return projectJSONExportDto.equals(projectJSONExportDtoFromEntity);
    }

    private boolean checkDependencyManagementFiles(String tempDir) {
        Path libsDir = Path.of(tempDir, "libs");
        if (!Files.exists(libsDir)) {
            return false;
        }

        Path mdcppsJsonFile = Path.of(tempDir, "mdcpps.json");
        if (!Files.exists(mdcppsJsonFile)) {
            return false;
        }

        return true;
    }

    private boolean checkSourceRootFolder(String tempDir) {
        Path srcDir = Path.of(tempDir, "src");
        return Files.exists(srcDir);
    }

    private boolean checkMdcppsFolder(String tempDir) {
        Path mdcppsDir = Path.of(tempDir, "src", "mdcpps");
        return Files.exists(mdcppsDir);
    }

    private boolean checkTestFolder(String tempDir) {
        Path testDir = Path.of(tempDir, "src", "test");
        return Files.exists(testDir);
    }

    private boolean checkCommonWorkspaceContent(String tempDir, Project project) {
        WorkSpace commonWorkspace = project.getWorkSpaces().stream().filter(WorkSpace::getIsCommon).findFirst().orElse(null);

        if (commonWorkspace == null) {
            log.error("Common workspace not found");
            return false;
        }

        String commonWorkspaceDirName = createSlug(commonWorkspace.getName());

        if (!commonWorkspaceDirName.endsWith("_workspace")) {
            commonWorkspaceDirName += "_workspace";
        }

        Path commonWorkspaceDir = Path.of(tempDir, commonWorkspaceDirName);

        if (!Files.exists(commonWorkspaceDir)) {
            return false;
        }

        Path modelsDir = Path.of(tempDir, commonWorkspaceDirName, "models");
        if (!Files.exists(modelsDir)) {
            return false;
        }

        Path implDir = Path.of(tempDir, commonWorkspaceDirName, "impl");
        if (!Files.exists(implDir)) {
            return false;
        }

        Path dependencyManagementFiles = Path.of(tempDir, commonWorkspaceDirName, "mdcpps.json");
        if (!Files.exists(dependencyManagementFiles)) {
            return false;
        }

        Path dependencyDir = Path.of(tempDir, commonWorkspaceDirName, "libs");
        if (!Files.exists(dependencyDir)) {
            return false;
        }

        Path testDir = Path.of(tempDir, commonWorkspaceDirName, "test");
        if (!Files.exists(testDir)) {
            return false;
        }

        Path commonWorkspaceFile = Path.of(tempDir, commonWorkspaceDirName, "models", createSlug(commonWorkspace.getName()) + ".ccg");

        return Files.exists(commonWorkspaceFile);
    }

    private List<String> checkDomainWorkspaceContent(String tempDir, Project project) {
        List<String> inconsistentWorkspaces = new ArrayList<>();
        for (WorkSpace domainWorkspace : project.getWorkSpaces()) {
            if (!domainWorkspace.getIsCommon()) {
                String domainWorkspaceDirName = createSlug(domainWorkspace.getName());

                if (!domainWorkspaceDirName.endsWith("_workspace")) {
                    domainWorkspaceDirName += "_workspace";
                }

                Path domainWorkspaceDir = Path.of(tempDir, domainWorkspaceDirName);

                if (!Files.exists(domainWorkspaceDir)) {
                    inconsistentWorkspaces.add("Domain workspace " + domainWorkspace.getName() + " is missing");
                    continue;
                }

                Path modelsDir = Path.of(tempDir, domainWorkspaceDirName, "models");
                if (!Files.exists(modelsDir)) {
                    inconsistentWorkspaces.add("Domain workspace " + domainWorkspace.getName() + " models directory is missing");
                }

                Path implDir = Path.of(tempDir, domainWorkspaceDirName, "impl");
                if (!Files.exists(implDir)) {
                    inconsistentWorkspaces.add("Domain workspace " + domainWorkspace.getName() + " impl directory is missing");
                }

                Path dependencyManagementFiles = Path.of(tempDir, domainWorkspaceDirName, "mdcpps.json");
                if (!Files.exists(dependencyManagementFiles)) {
                    inconsistentWorkspaces.add("Domain workspace " + domainWorkspace.getName() + " dependency management files are missing");
                }

                Path dependencyDir = Path.of(tempDir, domainWorkspaceDirName, "libs");
                if (!Files.exists(dependencyDir)) {
                    inconsistentWorkspaces.add("Domain workspace " + domainWorkspace.getName() + " dependency directory is missing");
                }

                Path domainWorkspaceFile = Path.of(tempDir, domainWorkspaceDirName, "models", createSlug(domainWorkspace.getName()) + ".cg");
                if (!Files.exists(domainWorkspaceFile)) {
                    inconsistentWorkspaces.add("Domain workspace " + domainWorkspace.getName() + " file is missing");
                }

                Path testDir = Path.of(tempDir, domainWorkspaceDirName, "test");
                if (!Files.exists(testDir)) {
                    inconsistentWorkspaces.add("Domain workspace " + domainWorkspace.getName() + " test directory is missing");
                }
            }
        }

        return inconsistentWorkspaces;
    }

    private boolean checkTestStructure(String tempDir) {
        Path unitDir = Path.of(tempDir, "src", "test", "unit");
        if (!Files.exists(unitDir)) {
            return false;
        }

        Path integrationDir = Path.of(tempDir, "src", "test", "integration");
        if (!Files.exists(integrationDir)) {
            return false;
        }

        Path unitReadmeFile = Path.of(tempDir, "src", "test", "unit", "README.md");
        if (!Files.exists(unitReadmeFile)) {
            return false;
        }

        Path integrationReadmeFile = Path.of(tempDir, "src", "test", "integration", "README.md");
        return Files.exists(integrationReadmeFile);
    }
}
