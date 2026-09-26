# Collections and personal ratings

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    actor User
    participant API as Collection / Rating Controller
    participant Current as Current User
    participant Collections as Collection Service
    participant Ratings as Rating Service
    participant Movies as Movie Service
    participant Activity as Activity Reporter
    database DB

    alt List or read owned collections
        Admin->>API: GET /api/me/collections[/id]
        API->>Current: Resolve user ID and ADMIN role
        API->>Collections: list/get(user, collection)
        Collections->>DB: Query collection with owner constraint
        alt Collection belongs to another user or is absent
            Collections-->>Admin: 404 Not Found
        else Owned
            Collections->>DB: Load collection items
            Collections->>Movies: Load each movie
            Collections-->>API: Collection/page
            API-->>Admin: 200 response
        end
    else Create or rename collection
        Admin->>API: POST or PATCH collection
        API->>Collections: create/rename(user, name)
        Collections->>DB: Insert/update owned collection
        Collections->>Activity: Record audit event
        Activity->>DB: Insert audit event
        API-->>Admin: 201 or 200
    else Delete collection
        Admin->>API: DELETE collection
        API->>Collections: delete(user, id)
        Collections->>DB: Lock and verify ownership
        Collections->>DB: Delete collection
        Note over Collections,DB: Items and share links cascade;<br/>ratings and audit history remain
        Collections->>Activity: Record deletion
        API-->>Admin: 204
    else Add or remove movie
        Admin->>API: PUT/DELETE collection movie
        API->>Collections: add/remove(user, collection, movie)
        Collections->>DB: Lock and verify ownership
        opt Adding
            Collections->>Movies: Verify movie exists
        end
        Collections->>DB: Check or change membership
        alt State actually changed
            Collections->>DB: Update collection timestamp
            Collections->>Activity: Record audit + analytics
            Activity->>DB: Insert events
        else Already in requested state
            Note over Collections,DB: Idempotent; no duplicate event
        end
        API-->>Admin: 204
    else Save personal rating
        User->>API: PUT /api/me/ratings/{movieId}
        API->>Ratings: put(user, movie, score)
        Ratings->>DB: Lock user and verify movie
        Ratings->>DB: Find current rating
        alt Same score
            Ratings-->>API: Existing rating
        else New or changed score
            Ratings->>DB: Insert/update rating
            Ratings->>Activity: Record audit + analytics
            Activity->>DB: Insert events
        end
        API-->>User: Rating response
    else Delete or list ratings
        User->>API: GET or DELETE /api/me/ratings
        API->>Ratings: list/delete(user, movie?)
        Ratings->>DB: Query or delete rating
        opt Rating was deleted
            Ratings->>Activity: Record audit + analytics
        end
        API-->>User: Page or 204
    end
```

