# Share-link lifecycle

```mermaid
sequenceDiagram
    autonumber
    actor Admin
    actor Recipient
    participant API as Share Controllers
    participant Shares as Share Service
    participant Collections as Collection Service
    participant Codec as SHA-256 Token Codec
    participant Activity as Activity Reporter
    database DB

    alt Create share link
        Admin->>API: POST /api/me/collections/{id}/shares
        API->>Shares: create(user, collection, expiresAt?)
        Shares->>Collections: Verify ownership
        Collections->>DB: Lock owned collection
        Shares->>Codec: Generate random token
        Codec-->>Shares: Raw token
        Shares->>Codec: SHA-256 digest
        Shares->>DB: Store digest and optional expiry
        Shares->>Activity: Record SHARE_CREATED
        Shares-->>API: ID, raw token, path, expiry
        API-->>Admin: 201 Created
        Note over Admin,DB: Raw token is returned once;<br/>only its digest is stored
    else List active links
        Admin->>API: GET collection shares
        API->>Shares: list(user, collection)
        Shares->>Collections: Verify ownership
        Shares->>DB: Query active, unexpired shares
        Shares-->>API: Metadata without tokens
        API-->>Admin: 200 list
    else Revoke link
        Admin->>API: DELETE collection share
        API->>Shares: revoke(user, collection, share)
        Shares->>Collections: Verify ownership
        Shares->>DB: Set revoked timestamp
        opt Newly revoked
            Shares->>Activity: Record SHARE_REVOKED
        end
        API-->>Admin: 204
    else Read shared collection
        Recipient->>API: GET /api/shared/collections/{token}
        API->>Shares: read(token, page, size)
        Shares->>Codec: Validate format and digest token
        Shares->>DB: Find matching active, unexpired share
        alt Invalid, expired, revoked, or absent
            Shares-->>Recipient: 404 Not Found
        else Valid
            Shares->>Collections: Load paginated movies
            Collections->>DB: Query collection items
            Shares-->>API: Collection name + movies
            API-->>Recipient: 200 read-only, no-cache response
        end
    end
```

