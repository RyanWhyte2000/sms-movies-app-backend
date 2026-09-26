# Request lifecycle

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant Correlation as Correlation Filter
    participant Limiter as Rate-Limit Filter
    participant Session as JDBC Session / Security
    participant Controller
    participant Service
    database DB
    participant Errors as Exception Handler

    Client->>Correlation: HTTP request
    Correlation->>Correlation: Generate correlation ID
    Correlation-->>Client: X-Correlation-ID header
    Correlation->>Limiter: Continue request

    alt OPTIONS, non-API, or limiter disabled
        Limiter->>Session: Skip limiting
    else Request permitted
        Limiter->>Limiter: Increment client window
        Limiter-->>Client: RateLimit-* headers
        Limiter->>Session: Continue request
    else Limit exceeded
        Limiter-->>Client: 429 Problem Detail + Retry-After
    end

    Session->>DB: Load session and security context
    Session->>Controller: Dispatch request

    alt Missing login for private endpoint
        Controller-->>Client: 401 Problem Detail
    else Insufficient ADMIN authority
        Controller-->>Client: 403 Problem Detail
    else Authorized or public endpoint
        Controller->>Controller: Validate body, query and UUIDs
        alt Validation failure
            Controller->>Errors: Validation exception
            Errors-->>Client: 400 Problem Detail + errors map
        else Valid request
            Controller->>Service: Execute operation
            Service->>DB: Read or mutate data
            DB-->>Service: Result
            Service-->>Controller: DTO / page / status
            Controller-->>Client: 2xx response
        end
    end

    opt Domain or persistence error
        Service->>Errors: Throw API/domain exception
        Errors-->>Client: 404, 409, or 500 Problem Detail
    end
```

