# Catalogue browsing and administration

```mermaid
sequenceDiagram
    autonumber
    actor Guest
    actor Admin
    participant API as Catalogue Controllers
    participant Catalog as Catalog Service
    participant Movies as Movie Service
    participant Activity as Activity Reporter
    database DB

    alt Search/list movies
        Guest->>API: GET /api/movies?filters&page&size
        API->>Catalog: search(user?, q, cast, director, genre)
        Catalog->>DB: Query matching movie IDs and total
        DB-->>Catalog: IDs + count
        Catalog->>DB: Load movie summaries
        opt Signed-in user
            Catalog->>Activity: Record search audit + analytics
            Activity->>DB: Insert events
        end
        Catalog-->>API: Paginated movies
        API-->>Guest: 200 PageResponse
    else Movie details
        Guest->>API: GET /api/movies/{id}
        API->>Catalog: details(id, current user?)
        Catalog->>DB: Load movie, credits, genres, external ratings
        opt Signed-in user
            Catalog->>DB: Load personal rating
        end
        Catalog-->>API: Movie details
        API-->>Guest: 200 details
    else Browse genres or people
        Guest->>API: GET /api/genres or /api/people
        API->>Catalog: genres() or people(query, role, page)
        Catalog->>DB: Query normalized catalogue tables
        Catalog-->>API: List or page
        API-->>Guest: 200 response
    else Create or update movie
        Admin->>API: POST /api/movies or PUT /api/movies/{id}
        API->>Movies: create/update validated request
        Movies->>DB: Insert/update movie
        Movies->>Catalog: sync relationships
        Catalog->>DB: Replace movie credits and genres
        Catalog->>DB: Reuse or create primary director/genre
        Catalog->>DB: Insert director, cast and genre links
        API-->>Admin: 201 or 200 movie
    else Delete movie
        Admin->>API: DELETE /api/movies/{id}
        API->>Movies: delete(id)
        Movies->>DB: Find and delete movie
        API-->>Admin: 204 No Content
    else Create person or genre
        Admin->>API: POST /api/people or /api/genres
        API->>Catalog: create normalized item
        Catalog->>DB: Insert person/genre
        API-->>Admin: Created item
    else Import external rating
        Admin->>API: PUT /api/movies/{id}/external-ratings/{source}
        API->>Catalog: putExternal(...)
        Catalog->>DB: Lock and verify movie
        Catalog->>DB: Upsert sourced rating
        Catalog->>Activity: Record audit event
        Activity->>DB: Insert audit event
        API-->>Admin: Stored external rating
    end
```

