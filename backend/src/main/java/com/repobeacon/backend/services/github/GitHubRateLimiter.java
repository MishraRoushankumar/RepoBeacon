package com.repobeacon.backend.services.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GitHubRateLimiter {

  private final Long delayMs;

  public GitHubRateLimiter(@Value("${app.github.api-delay-ms:50}") long delayMs) {
    this.delayMs = delayMs;
  }

  public void pause() {
    if (delayMs <= 0) {
      return;
    }
    try {
      Thread.sleep(delayMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while rate limiting", e);
    }
  }

}
