package project.backend.service;

import jakarta.transaction.Transactional;
import project.backend.dto.*;

import java.security.Principal;
import java.util.List;

public interface ProjectService {

    CppsProjectDto createProject(CppsProjectDto projectDto, Principal principal);

    DemoProjectDto createDemoProject(DemoProjectDto demoProjectDto, Principal principal);

    List<CppsProjectListItemDto> getProjects(Principal principal);

    CppsProjectDto getProject(Long projectId, Principal principal);

    CppsProjectJSONExportDto exportProject(Long projectId, String jwt);

    CppsProjectJSONExportDto exportAnonymizedProject(Long projectId, String jwt);

    @Transactional
    void deleteProject(Long projectId, Principal principal);

    CppsProjectDto updateProject(Long projectId, CppsProjectDto projectDto, Principal principal);

    BuildResponseDto buildGitlabProject(Long projectId, Principal principal);

    BuildResponseDto buildGitlabSubgroups(Long projectId, Principal principal);

    BuildResponseDto addGitlabProjectUsers(Long projectId, Principal principal);

    BuildResponseDto addGitlabSubgroupsUsers(Long projectId, Principal principal);

    BuildResponseDto addGitlabDeployKey(Long projectId, Principal principal);

    BuildResponseDto resetGitlabProject(Long projectId, Principal principal);

    BuildResponseDto generateAndUploadRepositoryFiles(Long projectId, Principal principal);

    BuildResponseDto generateGitlabProjectLabels(Long projectId, Principal principal);

    BuildResponseDto editGitlabSubgroups(Long projectId, Principal principal);

    BuildResponseDto editGitlabProjectUsers(Long projectId, Principal principal);

    BuildResponseDto editGitlabSubgroupsUsers(Long projectId, Principal principal);

    BuildResponseDto updateAndUploadRepositoryFiles(Long projectId, Principal principal);

    String getGitlabUrl(Long projectId, Principal principal);

    CppsProjectDto importProject(CppsProjectJSONExportDto projectDto, Principal principal);

    BuildResponseDto performFileConsistencyCheck(Long projectId, Principal principal);
}
