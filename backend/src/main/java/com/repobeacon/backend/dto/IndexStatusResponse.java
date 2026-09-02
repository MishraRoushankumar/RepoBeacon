package com.repobeacon.backend.dto;

import java.time.Instant;
import java.util.UUID;

import com.repobeacon.backend.entity.IndexStatus;

public record IndexStatusResponse(
    UUID id,
    IndexStatus indexStatus,
    int filesTotal,
    int filesProcessed,
    int chunkCount,
    Instant indexedAt,
    String errorMessage) {

}
