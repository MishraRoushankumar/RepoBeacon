package com.repobeacon.backend.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.repobeacon.backend.dto.UserResponse;
import com.repobeacon.backend.entity.User;
import com.repobeacon.backend.security.AppUserPrincipal;
import com.repobeacon.backend.security.CurrentUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final CurrentUser currentUser;

  @GetMapping("/login")
  public Map<String, Object> loginUrl() {
    return Map.of("url", "/oauth2/authorization/github");
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> me() {
    AppUserPrincipal principal = currentUser.require();
    User user = principal.getUser();

    return ResponseEntity.ok(new UserResponse(
        user.getId(),
        user.getGithubId(),
        user.getGithubUsername(),
        user.getDisplayName(),
        user.getAvatarUrl()));
  }

}
