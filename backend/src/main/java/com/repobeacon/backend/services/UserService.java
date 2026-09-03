package com.repobeacon.backend.services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repobeacon.backend.entity.User;
import com.repobeacon.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final BytesEncryptor tokenEncryptor;

  @Transactional
  public User upsertFromGitHub(Map<String, Object> attributes, String accessToken, String scopes) {
    Long githubId = toLong(attributes.get("id"));
    String login = String.valueOf(attributes.get("login"));
    String name = attributes.get("name") != null
        ? String.valueOf(attributes.get("name"))
        : login;
    String avatarUrl = attributes.get("avatar_url") != null
        ? String.valueOf(attributes.get("avatar_url"))
        : null;

    String encryptedToken = Base64.getEncoder().encodeToString(
        tokenEncryptor.encrypt(accessToken.getBytes(StandardCharsets.UTF_8)));

    User user = userRepository.findByGithubId(githubId).orElseGet(User::new);
    user.setGithubId(githubId);
    user.setGithubUsername(login);
    user.setDisplayName(name);
    user.setAvatarUrl(avatarUrl);
    user.setAccessToken(encryptedToken);
    user.setTokenScopes(scopes);
    return userRepository.save(user);
  }

  @Transactional(readOnly = true)
  public User requireById(UUID id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
  }

  public String decryptAccessToken(User user) {
    byte[] encryptedToken = Base64.getDecoder().decode(user.getAccessToken());
    return new String(tokenEncryptor.decrypt(encryptedToken), StandardCharsets.UTF_8);
  }

  private static Long toLong(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.parseLong(String.valueOf(value));
  }
}