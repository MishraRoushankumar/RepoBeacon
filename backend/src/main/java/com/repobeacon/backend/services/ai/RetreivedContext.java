package com.repobeacon.backend.services.ai;

import java.util.List;

import com.repobeacon.backend.dto.CitationDto;

public record RetreivedContext(
    List<CitationDto> citations,
    String contextText) {
}