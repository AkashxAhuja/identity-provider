package com.example.accesstoken.controller;

import com.example.accesstoken.dto.TokenRequest;
import com.example.accesstoken.dto.TokenResponse;
import com.example.accesstoken.model.ApplicationClient;
import com.example.accesstoken.service.ApplicationRegistryService;
import com.example.accesstoken.service.TokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications")
public class ApplicationRegistryController {

    private final ApplicationRegistryService registryService;
    private final TokenService tokenService;

    public ApplicationRegistryController(ApplicationRegistryService registryService, TokenService tokenService) {
        this.registryService = registryService;
        this.tokenService = tokenService;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<TokenResponse> authenticate(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "resource", required = false) String resource,
            @RequestParam(value = "audience", required = false) String audience) {
        TokenRequest request = new TokenRequest();
        request.setGrantType("client_credentials");
        request.setScope(scope);
        request.setResource(resource);
        request.setAudience(audience);

        ApplicationClient client = registryService.authenticateFromAuthorizationHeader(authorization);
        TokenResponse response = tokenService.generateTokenForApplicationClient(client, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
