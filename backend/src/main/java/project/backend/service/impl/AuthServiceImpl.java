package project.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import project.backend.entity.User;
import project.backend.service.AuthService;
import project.backend.service.UserService;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Value("${spring.security.oauth2.client.registration.gitlab.client-id}")
    private String clientID;

    @Value("${spring.security.oauth2.client.registration.gitlab.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.gitlab.redirect-uri}")
    private String redirectURI;

    @Value("${spring.security.oauth2.client.provider.gitlab.token-uri}")
    private String tokenURI;

    @Value("${spring.security.oauth2.client.provider.gitlab.user-info-uri}")
    private String userInfoURI;

    private final RestTemplate restTemplate = new RestTemplate();

    private final UserService userService;

    @Override
    public Long getGitlabIDFromCode(String code) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.add("client_id", clientID);
        parameters.add("client_secret", clientSecret);
        parameters.add("code", code);
        parameters.add("grant_type", "authorization_code");
        parameters.add("redirect_uri", redirectURI);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange(tokenURI, HttpMethod.POST, requestEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        log.info("Response: {}", jsonResponse);

        ObjectMapper objectMapper = new ObjectMapper();
        String accessToken = null;
        String refreshToken = null;
        Integer expirationTime = null;

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            accessToken = jsonNode.get("access_token").asText();
            refreshToken = jsonNode.get("refresh_token").asText();
            expirationTime = jsonNode.get("created_at").asInt() + jsonNode.get("expires_in").asInt();
        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
        }

        log.info("Access token: {}", accessToken);

        Long userId = generateUserFromAccessToken(accessToken);

        userService.updateGitlabAuthData(userId, accessToken, refreshToken, expirationTime);

        return userId;
    }

    private Long generateUserFromAccessToken(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange(userInfoURI, HttpMethod.GET, requestEntity, String.class);

        String jsonResponse = responseEntity.getBody();
        ObjectMapper objectMapper = new ObjectMapper();

        long id = -1L;
        String avatarUrl = null;
        String username = null;
        String email = null;

        try {
            JsonNode jsonNode = objectMapper.readTree(jsonResponse);
            id = jsonNode.get("id").asLong();
            avatarUrl = jsonNode.get("avatar_url").asText();
            username = jsonNode.get("username").asText();
            email = jsonNode.get("email").asText();

        } catch (Exception e) {
            log.error("Error parsing JSON response", e);
            return -1L;
        }

        return userService.create_or_return_user(id, username, email, avatarUrl);
    }
}
