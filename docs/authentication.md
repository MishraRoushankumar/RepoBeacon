# GitHub OAuth2 Authentication

RepoBeacon uses GitHub OAuth2 for authentication. The backend owns the OAuth flow, user persistence, access-token handling, and authenticated session.

## Authentication Flow

```mermaid
sequenceDiagram
    actor User
    participant Browser as Next.js Browser
    participant Backend as Spring Boot Backend
    participant GitHub as GitHub OAuth2

    User->>Browser: Sign in with GitHub
    Browser->>Backend: GET /oauth2/authorization/github
    Backend-->>Browser: Redirect to GitHub
    Browser->>GitHub: Authorization request
    GitHub-->>User: Consent screen
    User->>GitHub: Grant permission
    GitHub-->>Browser: OAuth callback
    Browser->>Backend: Authorization code
    Backend->>GitHub: Exchange code for access token
    GitHub-->>Backend: Access token
    Backend->>Backend: Create/update user
    Backend->>Backend: Create authenticated session
    Backend-->>Browser: Redirect to dashboard
    Browser->>Backend: GET /api/auth/me
    Backend-->>Browser: Authenticated user
```

GitHub access tokens are handled by the backend and are not intended to be exposed to the frontend.

## Related Documentation

- [Architecture](architecture.md)
- [Repository Synchronization](repository-sync.md)
