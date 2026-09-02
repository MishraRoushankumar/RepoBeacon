package com.repobeacon.backend.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.repobeacon.backend.dto.IndexStatusResponse;
import com.repobeacon.backend.dto.RepositoryResponse;
import com.repobeacon.backend.security.CurrentUser;
import com.repobeacon.backend.services.RepoService;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepoController {

  private final CurrentUser currentUser;
  private final RepoService repoService;

  @GetMapping
  public List<RepositoryResponse> list(@RequestParam(name = "refresh", defaultValue = "true") boolean refresh) {
    UUID userId = currentUser.require().getId();
    if (refresh) {
      return repoService.syncAndListRepos(userId);
    }
    return repoService.listSorted(userId);
  }

  @GetMapping("/{id}")
  public RepositoryResponse get(@PathVariable UUID id) {
    UUID userId = currentUser.require().getId();
    return repoService.toResponse(repoService.requireOwned(id, userId));
  }

  @GetMapping("/{id}/status")
  public IndexStatusResponse status(@PathVariable UUID id) {
    UUID userId = currentUser.require().getId();
    return repoService.status(id, userId);
  }

}
