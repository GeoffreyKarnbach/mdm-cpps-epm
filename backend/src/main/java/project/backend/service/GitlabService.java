package project.backend.service;

import org.springframework.http.HttpEntity;
import project.backend.dto.GitlabGroupListItemDto;

import java.security.Principal;
import java.util.List;

public interface GitlabService {

    List<GitlabGroupListItemDto> getAllUserGroups(Principal principal);

    boolean createGitlabProject(Long projectId, long userId);

    boolean createGitlabSubgroups(Long projectId, long userId);

    boolean addGitlabProjectUsers(Long projectId, long userId);

    boolean addGitlabSubgroupsUsers(Long projectId, long userId);

    boolean addGitlabDeployKey(Long projectId, long userId);

    boolean resetGitlabProject(Long projectId, long userId);

    String getGitlabUrl(Long projectId, long userId);

    boolean uploadRepositoryFiles(Long projectId, String tempDirName, long userId);

    boolean updateRepositoryFiles(Long projectId, String tempDirName, long userId);

    boolean createGitlabProjectLabels(Long projectId, long userId);

    boolean createOnlyNewGitlabSubgroups(Long projectId, long userId);

    boolean editGitlabProjectUsers(Long projectId, long userId);

    boolean editGitlabSubgroupsUsers(Long projectId, long userId);

    String createSlug(String name);

    HttpEntity<String> refreshAccessTokenAndGetHttpEntity(long userId);

    String getRootGroupUrl(Long projectId, long userId);

    String getProjectUrl(Long projectId, long userId);
}
