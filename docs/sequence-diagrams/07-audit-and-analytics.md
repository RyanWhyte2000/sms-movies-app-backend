# Audit and analytics

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant API as Audit / Analytics Controller
    participant Audit as Audit Service
    participant Analytics as Analytics Service
    database DB

    alt Read personal audit history
        User->>API: GET /api/me/audit-events
        API->>Audit: list(user, action, from, to, page)
        Audit->>DB: Query user's immutable events newest first
        Audit-->>API: PageResponse
        API-->>User: 200 audit page
    else Submit consented client event
        User->>API: POST /api/analytics/events
        API->>Analytics: accept(user, event)
        Analytics->>DB: Lock user and look up eventId
        alt Same eventId and identical payload
            Analytics-->>API: Treat retry as successful
            API-->>User: 202 Accepted
        else Event ID reused with different payload or user
            Analytics-->>User: 409 Conflict
        else New valid event
            Analytics->>DB: Verify movie exists
            alt Movie absent
                Analytics-->>User: 404 Not Found
            else Movie exists
                Analytics->>DB: Insert server-attributed event
                API-->>User: 202 Accepted
            end
        end
    else Backend-generated activity
        Note over Audit,Analytics: Collections, ratings, discovery,<br/>authentication and shares report activity
        Audit->>DB: Insert correlated audit record
        Analytics->>DB: Insert operational analytics event
    end
```

