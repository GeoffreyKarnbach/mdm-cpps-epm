package project.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import project.backend.dto.UserDto;
import project.backend.entity.User;
import project.backend.exception.NotFoundException;
import project.backend.service.AuthService;
import project.backend.service.UserService;
import project.backend.repository.UserRepository;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    @Value("${spring.security.oauth2.client.registration.gitlab.client-id}")
    private String clientID;

    @Value("${spring.security.oauth2.client.registration.gitlab.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.gitlab.redirect-uri}")
    private String redirectURI;

    @Value("${spring.security.oauth2.client.provider.gitlab.token-uri}")
    private String tokenURI;

    private final UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    private final String userGetByGitlabIdUrl = "https://gitlab.com/api/v4/users/";

    @Override
    public Long create_user(long gitLabId, String gitLabUsername, String email, String avatarUrl) {
        log.info("Creating user with gitLabId: {}, gitLabUsername: {}, email: {}, avatarUrl: {}", gitLabId, gitLabUsername, email, avatarUrl);

        User user = User.builder()
                .gitlabId(gitLabId)
                .gitlabUsername(gitLabUsername)
                .email(email)
                .avatarUrl(avatarUrl)
                .build();

        userRepository.save(user);

        return user.getId();
    }

    @Override
    public Long create_or_return_user(long gitLabId, String gitLabUsername, String email, String avatarUrl) {
        Optional<User> user = userRepository.findByGitlabId(gitLabId);

        if (user.isPresent()) {
            if (user.get().getEmail() == null || user.get().getEmail().isEmpty() || user.get().getEmail().isBlank()) {
                user.get().setEmail(email);
                userRepository.save(user.get());
            }
            return user.get().getId();
        } else {
            return create_user(gitLabId, gitLabUsername, email, avatarUrl);
        }
    }

    @Override
    public UserDto get_user(long id){
        Optional<User> user = userRepository.findById(id);
        return user.map(value -> UserDto.builder()
            .id(value.getId())
            .gitlabId(value.getGitlabId())
            .gitlabUsername(value.getGitlabUsername())
            .email(value.getEmail())
            .avatarUrl(value.getAvatarUrl())
            .build()).orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    public User getUserEntity(long id) {
        Optional<User> user = userRepository.findById(id);
        return user.orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Override
    public User getUserEntityByGitlabId(long gitlabId) {
        Optional<User> user = userRepository.findByGitlabId(gitlabId);
        return user.orElseThrow(() -> new NotFoundException("User not found"));
    }


    @Override
    public void updateGitlabAuthData(long userId, String accessToken, String refreshToken, int expirationTime) {
        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            User userToUpdate = user.get();
            userToUpdate.setAccessToken(accessToken);
            userToUpdate.setRefreshToken(refreshToken);
            userToUpdate.setExpirationTime(expirationTime);
            userRepository.save(userToUpdate);
        } else {
            throw new NotFoundException("User not found");
        }
    }

    @Override
    public UserDto register_or_return_gitlab_user(Long gitlabId, long requesterId) {
        Optional<User> user = userRepository.findByGitlabId(gitlabId);
        if (user.isPresent()) {
            return UserDto.builder()
                    .id(user.get().getId())
                    .gitlabId(user.get().getGitlabId())
                    .gitlabUsername(user.get().getGitlabUsername())
                    .email(user.get().getEmail())
                    .avatarUrl(user.get().getAvatarUrl())
                    .build();
        }

        // Get user from Gitlab
        UserDto userDto = this.getUserByGitlabId(gitlabId, requesterId);

        // Create new user and return it
        User newUser = User.builder()
                .gitlabId(userDto.getGitlabId())
                .gitlabUsername(userDto.getGitlabUsername())
                .email(userDto.getEmail())
                .avatarUrl(userDto.getAvatarUrl())
                .build();

        userRepository.save(newUser);

        userDto.setId(newUser.getId());

        return userDto;
    }

    private UserDto getUserByGitlabId(long gitlabId, long requesterId) {

        boolean validToken = this.checkForNonExpiredToken(requesterId);

        if (!validToken) {
            this.refreshGitlabToken(requesterId);
        }

        String accessToken = this.getUserEntity(requesterId).getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange(userGetByGitlabIdUrl + gitlabId , HttpMethod.GET, requestEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        long id = -1L;
        String avatarUrl = null;
        String username = null;

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            id = jsonNode.get("id").asLong();
            avatarUrl = jsonNode.get("avatar_url").asText();
            username = jsonNode.get("username").asText();
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
            throw new NotFoundException("User not found");
        }

        return UserDto.builder()
                .gitlabId(id)
                .gitlabUsername(username)
                .avatarUrl(avatarUrl)
                .email("")
                .build();
    }

    @Override
    public void refreshGitlabToken(long userId) {
        User user = this.getUserEntity(userId);
        String refreshToken = user.getRefreshToken();

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("client_id", clientID);
        parameters.add("client_secret", clientSecret);
        parameters.add("refresh_token", refreshToken);
        parameters.add("grant_type", "refresh_token");
        parameters.add("redirect_uri", redirectURI);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange(tokenURI, HttpMethod.POST, requestEntity, String.class);

        String jsonResponse = responseEntity.getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        String accessToken = null;
        refreshToken = null;
        Integer expirationTime = null;

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            accessToken = jsonNode.get("access_token").asText();
            refreshToken = jsonNode.get("refresh_token").asText();
            expirationTime = jsonNode.get("created_at").asInt() + jsonNode.get("expires_in").asInt();
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
        }

        this.updateGitlabAuthData(userId, accessToken, refreshToken, expirationTime);
    }

    @Override
    public boolean checkForNonExpiredToken(long userId) {
        User user = this.getUserEntity(userId);
        Integer expirationTime = user.getExpirationTime();
        if (expirationTime != null) {
            return expirationTime > (System.currentTimeMillis() / 1000);
        }
        return false;
    }

    @Override
    public List<UserDto> getMatchingUsers(String usernameSubStr, long requesterId) {

        // Make request to Gitlab to get all users with search string
        boolean validToken = this.checkForNonExpiredToken(requesterId);

        if (!validToken) {
            this.refreshGitlabToken(requesterId);
        }

        String accessToken = this.getUserEntity(requesterId).getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(userGetByGitlabIdUrl)
            .queryParam("search", usernameSubStr);

        String urlWithParams = uriBuilder.encode().toUriString();

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange(urlWithParams, HttpMethod.GET, requestEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        List<UserDto> users = new ArrayList<>();
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            for (JsonNode node : jsonNode) {
                long id = node.get("id").asLong();
                String username = node.get("username").asText();
                users.add(UserDto.builder()
                        .gitlabId(id)
                        .gitlabUsername(username)
                        .build());
            }
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
        }

        return users;
    }
}
