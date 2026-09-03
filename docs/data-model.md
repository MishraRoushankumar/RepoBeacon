# Data Model

RepoBeacon uses PostgreSQL for application persistence and pgvector for repository vector data.

## Entity Relationships

```mermaid
erDiagram
    USER {
        uuid id PK
        string github_id UK
        string github_username
        string display_name
        string avatar_url
        string encrypted_access_token
        string token_scopes
        timestamp created_at
        timestamp updated_at
    }

    REPOSITORY {
        uuid id PK
        uuid user_id FK
        bigint github_repo_id
        string owner
        string name
        string full_name
        boolean is_private
        string default_branch
        string language
        string html_url
        string description
        string index_status
        int files_total
        int files_processed
        int chunk_count
        timestamp indexed_at
        string error_message
        timestamp created_at
        timestamp updated_at
    }

    CHAT_SESSION {
        uuid id PK
        uuid user_id FK
        uuid repository_id FK
        string title
        timestamp created_at
    }

    CHAT_MESSAGE {
        uuid id PK
        uuid session_id FK
        string role
        text content
        jsonb citations
        timestamp created_at
    }

    VECTOR_EMBEDDINGS {
        uuid id PK
        text content
        vector embedding
        jsonb metadata
    }

    USER ||--o{ REPOSITORY : owns
    USER ||--o{ CHAT_SESSION : has
    REPOSITORY ||--o{ CHAT_SESSION : scopes
    CHAT_SESSION ||--o{ CHAT_MESSAGE : contains
    REPOSITORY ||--o{ VECTOR_EMBEDDINGS : indexed_as
```

## Main Relationships

- A `User` owns repositories and chat sessions.
- A `Repository` belongs to a user and scopes chat sessions.
- A `ChatSession` contains ordered `ChatMessage` records.
- Assistant messages can contain citation metadata.
- Repository source chunks are represented as vector embeddings for similarity retrieval.

## Related Documentation

- [Architecture](architecture.md)
- [Repository Indexing](repository-indexing.md)
- [RAG Chat](rag-chat.md)
