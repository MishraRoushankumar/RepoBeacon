package com.repobeacon.backend.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.repobeacon.backend.entity.Repository;

import jakarta.persistence.LockModeType;

public interface RepositoryRepository extends JpaRepository<Repository, UUID> {
  List<Repository> findByUserIdOrderByFullNameAsc(UUID userId);

  Optional<Repository> findByIdAndUserId(UUID id, UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<Repository> findWithLockByIdAndUserId(UUID id, UUID userId);

  Optional<Repository> findByUserIdAndGithubRepoId(UUID id, Long githubRepoId);
}
