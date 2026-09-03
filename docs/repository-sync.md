# Repository Synchronization

Repository synchronization connects RepoBeacon with GitHub and persists repository information used by the application and indexing pipeline.

## Synchronization Flow

```mermaid
sequenceDiagram
    actor User
    participant Client as Next.js Client
    participant Backend as Spring Boot Backend
    participant GitHub as GitHub API
    participant DB as PostgreSQL

    User->>Client: Open repositories
    Client->>Backend: GET /api/repos
    Backend->>DB: Load repositories
    DB-->>Backend: Repository records
    Backend-->>Client: Repository list

    User->>Client: Sync repository
    Client->>Backend: GET /api/repos?refresh=true
    Backend->>GitHub: Fetch repository data
    GitHub-->>Backend: Repository data
    Backend->>DB: Update repository
    DB-->>Backend: Updated repository
    Backend-->>Client: Sync result
```

## Role in the RAG Pipeline

Synchronization provides the repository metadata and GitHub access required by the indexing pipeline.

## Related Documentation

- [Architecture](architecture.md)
- [Repository Indexing](repository-indexing.md)
