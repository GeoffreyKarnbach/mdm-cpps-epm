package project.backend.endpoint;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import project.backend.dto.JWTResponse;
import project.backend.dto.OAuthUrlResponse;
import project.backend.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class AuthEndpoint {

    @Value("${spring.security.oauth2.client.registration.gitlab.client-id}")
    private String clientID;

    @Value("${spring.security.oauth2.client.registration.gitlab.redirect-uri}")
    private String redirectURI;

    @Value("${spring.security.oauth2.client.registration.gitlab.scope}")
    private String scope;

    private final Map<String, String> clientTokenStateMap = new HashMap<>();
    private final Map<String, Long> clientTokenGitLabID = new HashMap<>();

    private final AuthService authService;
    private final String secretKey = "SECretKEySECretKEySECretKEySECretKEySECretKEySECretKEySECretKEySECretKEy";


    @GetMapping("/oauth/url")
    @Operation(summary = "Get a OAuth URL")
    @ResponseStatus(HttpStatus.OK)
    public OAuthUrlResponse getOAuthUrl(@RequestParam("client_token") String clientToken) {
        log.info("GET /api/v1/example");
        log.info("Client token: {}", clientToken);

        String state = UUID.randomUUID().toString();
        clientTokenStateMap.put(state, clientToken);
        clientTokenGitLabID.put(clientToken, -1L);

        return OAuthUrlResponse.builder()
            .url("https://gitlab.com/oauth/authorize?client_id=" + clientID + "&redirect_uri=" + redirectURI + "&response_type=code&state=" + state + "&scope=" + scope)
            .state(state)
            .build();
    }

    @GetMapping("/oauth/callback")
    @Operation(summary = "OAuth callback")
    @ResponseStatus(HttpStatus.OK)
    public void oAuthCallback(String code, String state) {
        log.info("GET /api/v1/auth/oauth/callback");

        String clientToken = clientTokenStateMap.get(state);

        if (clientToken == null) {
            log.error("Client token not found");
            return;
        }

        clientTokenStateMap.remove(state);
        clientTokenGitLabID.put(clientToken, authService.getGitlabIDFromCode(code));
    }

    @GetMapping("/jwt")
    @Operation(summary = "Get a JWT token")
    @ResponseStatus(HttpStatus.OK)
    public JWTResponse getJWT(@RequestParam("client_token") String clientToken) {
        log.info("GET /api/v1/auth/jwt");

        Long gitLabID = clientTokenGitLabID.get(clientToken);

        if (gitLabID == null || gitLabID == -1) {
            log.error("Client token not found or not logged in");
            return null;  // Handle error properly (e.g., throw an exception)
        }

        String jwt = Jwts.builder()
            .setSubject(gitLabID.toString())
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 48))
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();

        return JWTResponse.builder().token(jwt).build();
    }
}