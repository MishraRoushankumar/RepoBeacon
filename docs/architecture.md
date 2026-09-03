# RepoBeacon Architecture

RepoBeacon is a full-stack codebase assistant built around a Next.js client, Spring Boot backend, PostgreSQL + pgvector, GitHub integration, and Google GenAI.

## System Architecture

```mermaid
flowchart TB
    subgraph Client["Next.js 16 Client"]
        UI["Pages & Components"]
        RQ["TanStack React Query"]
        API["API Client"]
        SSE["SSE Stream Client"]
    end

    subgraph Backend["Spring Boot Backend"]
        SEC["Spring Security"]
        CTRL["REST Controllers"]
        SVC["Application Services"]
        RAG["RAG Pipeline"]
        AI["Spring AI"]
    end

    subgraph External["External Services"]
        GH["GitHub API"]
        GEMINI["Google GenAI"]
    end

    subgraph Data["Data Layer"]
        PG[("PostgreSQL + pgvector")]
    end

    UI --> RQ
    RQ --> API
    API -->|REST + Session Cookie| SEC
    SSE -->|SSE| CTRL
    SEC --> CTRL
    CTRL --> SVC
    SVC --> RAG
    SVC --> GH
    SVC --> PG
    RAG --> PG
    RAG --> AI
    AI --> GEMINI
```

## Related Documentation

- [Authentication](authentication.md)
- [Repository Synchronization](repository-sync.md)
- [Repository Indexing](repository-indexing.md)
- [RAG Chat](rag-chat.md)
- [Data Model](data-model.md)
