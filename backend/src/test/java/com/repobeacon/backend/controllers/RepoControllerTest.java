package com.repobeacon.backend.controllers;

import com.repobeacon.backend.dto.RepositoryResponse;
import com.repobeacon.backend.entity.IndexStatus;
import com.repobeacon.backend.entity.Repository;
import com.repobeacon.backend.entity.User;
import com.repobeacon.backend.exceptions.BadRequestException;
import com.repobeacon.backend.security.AppUserPrincipal;
import com.repobeacon.backend.security.CurrentUser;
import com.repobeacon.backend.services.RepoService;
import com.repobeacon.backend.services.indexing.IndexingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepoControllerTest {

  @Mock
  private CurrentUser currentUser;

  @Mock
  private RepoService repoService;

  @Mock
  private IndexingService indexingService;

  @InjectMocks
  private RepoController repoController;

  @Test
  void index_whenTaskRejected_marksFailed() {
    UUID userId = UUID.randomUUID();
    UUID repoId = UUID.randomUUID();

    User user = new User();
    user.setId(userId);
    AppUserPrincipal principal = new AppUserPrincipal(user, Map.of());
    when(currentUser.require()).thenReturn(principal);

    Repository initialRepo = new Repository();
    initialRepo.setId(repoId);
    initialRepo.setIndexStatus(IndexStatus.INDEXING);

    Repository failedRepo = new Repository();
    failedRepo.setId(repoId);
    failedRepo.setIndexStatus(IndexStatus.FAILED);
    failedRepo.setErrorMessage("Indexing task queue is full. Please try again later.");

    RepositoryResponse responseDto = new RepositoryResponse(
        repoId, 1L, "owner", "name", "owner/name", false, "main", "Java", "http://url", "desc",
        IndexStatus.FAILED, null, 0, 0, 0, "Indexing task queue is full. Please try again later."
    );

    when(indexingService.startIndexing(repoId, userId)).thenReturn(initialRepo);
    doThrow(new RejectedExecutionException("Queue full")).when(indexingService).indexAsync(repoId, userId);
    when(indexingService.markFailed(eq(repoId), anyString())).thenReturn(failedRepo);
    when(repoService.toResponse(failedRepo)).thenReturn(responseDto);

    ResponseEntity<RepositoryResponse> response = repoController.index(repoId);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertEquals(IndexStatus.FAILED, response.getBody().indexStatus());
    verify(indexingService).markFailed(eq(repoId), contains("queue is full"));
  }

  @Test
  void index_whenStartIndexingFails_doesNotScheduleIndexAsync() {
    UUID userId = UUID.randomUUID();
    UUID repoId = UUID.randomUUID();

    User user = new User();
    user.setId(userId);
    AppUserPrincipal principal = new AppUserPrincipal(user, Map.of());
    when(currentUser.require()).thenReturn(principal);

    when(indexingService.startIndexing(repoId, userId))
        .thenThrow(new BadRequestException("Repository is already being indexed"));

    assertThrows(BadRequestException.class, () -> repoController.index(repoId));

    verify(indexingService, never()).indexAsync(any(), any());
  }
}
