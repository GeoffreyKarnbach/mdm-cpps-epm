package project.backend.service;

import project.backend.dto.UserDto;
import project.backend.entity.User;

import java.util.List;

public interface UserService {

    Long create_user(long gitLabId, String gitLabUsername, String email, String avatarUrl);

    Long create_or_return_user(long gitLabId, String gitLabUsername, String email, String avatarUrl);

    UserDto get_user(long id);

    User getUserEntity(long id);

    User getUserEntityByGitlabId(long gitlabId);

    void updateGitlabAuthData(long userId, String accessToken, String refreshToken, int expirationTime);

    UserDto register_or_return_gitlab_user(Long gitlabId, long requesterId);

    void refreshGitlabToken(long userId);

    boolean checkForNonExpiredToken(long userId);

    List<UserDto> getMatchingUsers(String usernameSubStr, long requesterId);
}
