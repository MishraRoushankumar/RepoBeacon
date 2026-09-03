# Repository Indexing

RepoBeacon indexes repository source code asynchronously so that relevant code can later be retrieved through vector similarity search.

## Indexing Flow

```mermaid
sequenceDiagram
    actor User
    participant Client as Next.js Client
    participant Backend as Spring Boot Backend
    participant Indexer as Indexing Service
    participant GitHub as GitHub API
    participant Gemini as Google GenAI
    participant DB as PostgreSQL + pgvector

    User->>Client: Start repository indexing
    Client->>Backend: POST /api/repos/{id}/index
    Backend->>DB: Set status = IN_PROGRESS
    Backend-->>Client: Indexing started

    Backend->>Indexer: Start async indexing
    Indexer->>GitHub: Fetch repository files
    GitHub-->>Indexer: Source files

    loop Supported files
        Indexer->>Indexer: Filter file
        Indexer->>Indexer: Create code chunks
        Indexer->>Gemini: Generate embedding
        Gemini-->>Indexer: Vector embedding
        Indexer->>DB: Store chunk + embedding
    end

    Indexer->>DB: Set status = COMPLETED
```

## Pipeline

1. Fetch repository contents from GitHub.
2. Filter supported source/configuration files and ignored directories.
3. Split eligible files into token-based overlapping chunks.
4. Generate Google GenAI embeddings.
5. Store embeddings and metadata in PostgreSQL + pgvector.
6. Track indexing progress and failure state.
7. Mark successfully indexed repositories as ready for retrieval.

## Indexing Status

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> INDEXING : Start indexing
    INDEXING --> READY : Indexing completed
    INDEXING --> FAILED : Indexing failed
    FAILED --> INDEXING : Retry
    READY --> INDEXING : Re-index
```

## Related Documentation

- [Architecture](architecture.md)
- [RAG Chat](rag-chat.md)
- [Data Model](data-model.md)
