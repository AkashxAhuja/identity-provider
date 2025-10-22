package com.example.accesstoken.controller;

import com.example.accesstoken.dto.JwksResponse;
import com.example.accesstoken.dto.TokenRequest;
import com.example.accesstoken.dto.TokenResponse;
import com.example.accesstoken.dto.TokenRevocationRequest;
import com.example.accesstoken.dto.TokenValidationRequest;
import com.example.accesstoken.dto.TokenValidationResponse;
import com.example.accesstoken.service.TokenService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth")
public class TokenController {

    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> issueToken(@Valid @RequestBody TokenRequest request) {
        TokenResponse response = tokenService.generateToken(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/token/introspect")
    public ResponseEntity<TokenValidationResponse> introspect(@Valid @RequestBody TokenValidationRequest request) {
        TokenValidationResponse response = tokenService.validate(request.getToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/token/revoke")
    public ResponseEntity<Void> revoke(@Valid @RequestBody TokenRevocationRequest request) {
        tokenService.revoke(request.getToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<JwksResponse> jwks() {
        return ResponseEntity.ok(tokenService.jwks());
    }
}
