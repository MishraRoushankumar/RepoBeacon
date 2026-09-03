package com.repobeacon.backend.services.indexing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.repobeacon.backend.entity.IndexStatus;
import com.repobeacon.backend.entity.Repository;
import com.repobeacon.backend.exceptions.BadRequestException;
import com.repobeacon.backend.exceptions.NotFoundException;
import com.repobeacon.backend.repository.RepositoryRepository;
import com.repobeacon.backend.services.UserService;
import com.repobeacon.backend.services.ai.RagSettings;
import com.repobeacon.backend.services.github.GitHubRateLimiter;
import com.repobeacon.backend.services.github.GithubApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Indexes a GitHub repo for RAG: list files → chunk → embed → store in
 * pgvector.
 *
 * <p>
 * See also {@link CodeFileFilter} (which files) and {@link CodeChunker} (how to
 * split them).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingService {

  private static final int VECTOR_BATCH_SIZE = 32;
  private static final int PROGRESS_EVERY_N_FILES = 5;

  private final RepositoryRepository repositoryRepository;
  private final UserService userService;
  private final GithubApiClient gitHubApiClient;
  private final CodeFileFilter fileFilter;
  private final CodeChunker codeChunker;
  private final VectorStore vectorStore;
  private final GitHubRateLimiter rateLimiter;

  @Value("${app.indexing.max-file-bytes:102400}")
  private long maxFileBytes;

  @Transactional
  public Repository startIndexing(UUID repoId, UUID userId) {
    Repository repo = repositoryRepository.findWithLockByIdAndUserId(repoId, userId)
        .orElseThrow(() -> new NotFoundException("Repository not found"));

    if (repo.getIndexStatus() == IndexStatus.INDEXING) {
      throw new BadRequestException("Repository is already being indexed");
    }

    repo.setIndexStatus(IndexStatus.INDEXING);
    repo.setFilesProcessed(0);
    repo.setFilesTotal(0);
    repo.setChunkCount(0);
    repo.setErrorMessage(null);
    repo.setUpdatedAt(Instant.now());
    return repositoryRepository.save(repo);
  }

  @Async("indexingExecutor")
  public void indexAsync(UUID repoId, UUID userId) {
    try {
      doIndex(repoId, userId);
    } catch (Exception ex) {
      log.error("Indexing failed for repo {}", repoId, ex);
      markFailed(repoId, ex.getMessage());
    }
  }

  private void doIndex(UUID repoId, UUID userId) {
    Repository repo = repositoryRepository.findById(repoId)
        .orElseThrow(() -> new NotFoundException("Repository not found"));
    String token = userService.decryptAccessToken(userService.requireById(userId));

    String stagingRepoId = repoId.toString() + "_staging_" + UUID.randomUUID();

    List<Document> activeDocs = new ArrayList<>();
    int processed = 0;
    int totalChunks = 0;

    try {
      Map<String, Object> tree = gitHubApiClient.getRepoTree(
          token, repo.getOwner(), repo.getName(), repo.getDefaultBranch());
      List<String> filePaths = listIndexableFiles(tree);

      updateProgress(repoId, filePaths.size(), 0, 0, IndexStatus.INDEXING, null);

      List<Document> stagingBatch = new ArrayList<>();
      int successfullyIndexedCount = 0;
      int failedCount = 0;

      for (String path : filePaths) {
        try {
          String content = gitHubApiClient.getFileContent(
              token, repo.getOwner(), repo.getName(), path);
          List<Document> stagingChunks = codeChunker.chunkFile(stagingRepoId, path, content);
          List<Document> readyChunks = codeChunker.chunkFile(repoId.toString(), path, content);

          stagingBatch.addAll(stagingChunks);
          activeDocs.addAll(readyChunks);
          totalChunks += stagingChunks.size();

          if (stagingBatch.size() >= VECTOR_BATCH_SIZE) {
            vectorStore.add(stagingBatch);
            stagingBatch.clear();
          }
          successfullyIndexedCount++;
        } catch (Exception ex) {
          failedCount++;
          log.warn("Skipping file {} in {}: {}", path, repo.getFullName(), ex.getMessage());
        }

        processed++;
        if (processed % PROGRESS_EVERY_N_FILES == 0 || processed == filePaths.size()) {
          updateProgress(repoId, filePaths.size(), processed, totalChunks, IndexStatus.INDEXING, null);
        }
        rateLimiter.pause();
      }

      if (!filePaths.isEmpty() && successfullyIndexedCount == 0) {
        deleteExistingVectors(stagingRepoId);
        markFailed(repoId, "Failed to index any files from repository (" + failedCount + " file(s) failed)");
        return;
      }

      if (!stagingBatch.isEmpty()) {
        vectorStore.add(stagingBatch);
      }

      deleteExistingVectors(repoId.toString());

      for (int i = 0; i < activeDocs.size(); i += VECTOR_BATCH_SIZE) {
        List<Document> batch = activeDocs.subList(i, Math.min(i + VECTOR_BATCH_SIZE, activeDocs.size()));
        vectorStore.add(batch);
      }

      deleteExistingVectors(stagingRepoId);
      markReady(repoId, filePaths.size(), processed, totalChunks, repo.getFullName());
    } catch (Exception ex) {
      deleteExistingVectors(stagingRepoId);
      throw ex;
    }
  }

  /** GitHub tree API → paths of source files we want to embed. */
  @SuppressWarnings("unchecked")
  private List<String> listIndexableFiles(Map<String, Object> tree) {
    if (tree == null || tree.get("tree") == null) {
      return List.of();
    }

    List<Map<String, Object>> entries = (List<Map<String, Object>>) tree.get("tree");
    return entries.stream()
        .filter(entry -> "blob".equals(String.valueOf(entry.get("type"))))
        .filter(entry -> {
          String path = String.valueOf(entry.get("path"));
          long size = entry.get("size") instanceof Number n ? n.longValue() : 0L;
          return fileFilter.isEligible(path, size, maxFileBytes);
        })
        .map(entry -> String.valueOf(entry.get("path")))
        .toList();
  }

  private void deleteExistingVectors(String repoId) {
    try {
      var filter = new FilterExpressionBuilder().eq(RagSettings.METADATA_REPO_ID, repoId).build();
      vectorStore.delete(filter);
    } catch (Exception ex) {
      log.warn("Could not delete existing vectors for repo {}: {}", repoId, ex.getMessage());
    }
  }

  @Transactional
  protected void updateProgress(
      UUID repoId,
      int total,
      int processed,
      int chunks,
      IndexStatus status,
      String error) {
    repositoryRepository.findById(repoId).ifPresent(repo -> {
      repo.setFilesTotal(total);
      repo.setFilesProcessed(processed);
      repo.setChunkCount(chunks);
      repo.setIndexStatus(status);
      repo.setErrorMessage(error);
      repo.setUpdatedAt(Instant.now());
      repositoryRepository.save(repo);
    });
  }

  @Transactional
  protected void markReady(UUID repoId, int totalFiles, int processedFiles, int totalChunks, String fullName) {
    repositoryRepository.findById(repoId).ifPresent(repo -> {
      repo.setIndexStatus(IndexStatus.READY);
      repo.setFilesTotal(totalFiles);
      repo.setFilesProcessed(processedFiles);
      repo.setChunkCount(totalChunks);
      repo.setIndexedAt(Instant.now());
      repo.setErrorMessage(null);
      repo.setUpdatedAt(Instant.now());
      repositoryRepository.save(repo);
    });
    log.info("Indexed {} files ({} chunks) for {}", processedFiles, totalChunks, fullName);
  }

  @Transactional
  public Repository markFailed(UUID repoId, String message) {
    return repositoryRepository.findById(repoId).map(repo -> {
      repo.setIndexStatus(IndexStatus.FAILED);
      repo.setErrorMessage(message != null && message.length() > 2000
          ? message.substring(0, 2000)
          : message);
      repo.setUpdatedAt(Instant.now());
      return repositoryRepository.save(repo);
    }).orElse(null);
  }
}