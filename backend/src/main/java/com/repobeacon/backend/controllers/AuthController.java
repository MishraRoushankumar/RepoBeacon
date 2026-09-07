package com.repobeacon.backend.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.repobeacon.backend.dto.UserResponse;
import com.repobeacon.backend.entity.User;
import com.repobeacon.backend.security.AppUserPrincipal;
import com.repobeacon.backend.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final CurrentUser currentUser;

  @GetMapping("/login-url")
  public Map<String, String> loginUrl() {
    return Map.of("url", "/oauth2/authorization/github");
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> me(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Authentication secContextAuth = SecurityContextHolder.getContext().getAuthentication();
    Object principalObj = secContextAuth != null ? secContextAuth.getPrincipal() : null;

    log.info("[AUTH-DIAG][ME] requestedSessionId={}, httpSessionExists={}, sessionId={}, authClass={}, principalClass={}, authName={}, secContextAuth={}",
        request.getRequestedSessionId(),
        session != null,
        session != null ? session.getId() : null,
        secContextAuth != null ? secContextAuth.getClass().getName() : null,
        principalObj != null ? principalObj.getClass().getName() : null,
        secContextAuth != null ? secContextAuth.getName() : null,
        secContextAuth != null ? secContextAuth.getClass().getName() : null);

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