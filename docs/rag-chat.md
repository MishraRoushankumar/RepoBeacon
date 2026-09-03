# RAG Chat

RepoBeacon combines repository-scoped vector retrieval with Google GenAI to answer questions using relevant code context.

## Streaming RAG Flow

```mermaid
sequenceDiagram
    actor User
    participant UI as Chat UI
    participant Hook as useStreamChat
    participant SSE as SSE Client
    participant Controller as ChatController
    participant Service as ChatService
    participant Retriever as CodeContextRetriever
    participant PG as PostgreSQL + pgvector
    participant Gemini as Google GenAI

    User->>UI: Ask question
    UI->>Hook: send(question)
    Hook->>SSE: streamChatMessage()
    SSE->>Controller: POST /api/chat/sessions/{id}/messages

    Controller->>Service: streamReply()
    Service->>PG: Validate session
    Service->>PG: Save user message

    Service->>Retriever: Retrieve repository context
    Retriever->>PG: Similarity search
    PG-->>Retriever: Relevant code chunks
    Retriever-->>Service: Retrieved context + citations

    Service->>Service: Build system + user prompts
    Service->>Gemini: Stream response

    loop Each response token
        Gemini-->>Service: Token
        Service-->>SSE: SSE token event
        SSE-->>Hook: onToken()
        Hook-->>UI: Update streaming response
    end

    Service->>PG: Save assistant message + citations
    Service-->>SSE: assistant_message + done
    SSE-->>Hook: Stream complete
    Hook-->>UI: Render final response
```

## Chat Sessions

A chat session belongs to a user and repository. Messages are persisted as user or assistant messages so conversation history can be retrieved when a session is reopened.

## Retrieval

`CodeContextRetriever` performs vector similarity search against indexed repository content. Retrieval is scoped to the selected repository and currently uses a top-K value of 8 chunks.

## Prompt Construction

`ChatPromptBuilder` constructs system and user prompts. The system instructions establish RepoBeacon's behavior, while the user prompt contains retrieved repository context and the question.

## Citations

Retrieved chunks retain source metadata such as file path and line range. Citation metadata is mapped into `CitationDto` objects and persisted with assistant messages.

## SSE Events

The streaming flow communicates events including:

- `user_message`
- `token`
- `assistant_message`
- `done`
- `error`

The client can cancel an active stream using an abort controller.

## Related Documentation

- [Architecture](architecture.md)
- [Repository Indexing](repository-indexing.md)
- [Data Model](data-model.md)
