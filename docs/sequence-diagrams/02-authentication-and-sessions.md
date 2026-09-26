# Registration, login, session, and logout

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Auth as Auth Controller
    participant Accounts as Account Service
    participant Provider as Authentication Manager
    participant Context as Security Context
    participant Session as JDBC Session Store
    participant Audit as Audit Service
    database DB

    alt Register
        User->>Auth: POST /api/auth/register
        Auth->>Accounts: register(email, password, displayName)
        Accounts->>Accounts: Normalize email and hash password
        Accounts->>DB: Insert USER account
        alt Email already exists
            DB-->>Auth: Uniqueness conflict
            Auth-->>User: 409 Conflict
        else Account created
            Accounts->>DB: Create "My movies" collection
            Accounts->>Audit: Record registration
            Audit->>DB: Insert audit event
            Auth->>Provider: Authenticate new credentials
            Provider->>DB: Load user and password hash
            Provider-->>Auth: Authenticated principal
            Auth->>Session: Rotate/create session ID
            Auth->>Context: Save authenticated context
            Context->>Session: Persist context
            Auth->>Audit: Record successful login
            Auth-->>User: 201 user + HttpOnly session cookie
        end
    else Login
        User->>Auth: POST /api/auth/login
        Auth->>Provider: Authenticate normalized email/password
        Provider->>DB: Load user and verify PBKDF2 hash
        alt Invalid credentials
            Auth->>Audit: Record failed login when user is known
            Audit->>DB: Insert audit event
            Auth-->>User: 401 generic credential error
        else Valid credentials
            Auth->>Audit: Record successful login
            Auth->>Session: Rotate session ID
            Auth->>Context: Save security context
            Context->>Session: Persist context
            Auth-->>User: 200 user + session cookie
        end
    else Current account
        User->>Auth: GET /api/me + session cookie
        Auth->>Session: Resolve authenticated user
        Session->>DB: Load session/account
        Auth-->>User: Account response without password hash
    else Logout
        User->>Auth: POST /api/auth/logout
        Auth->>Audit: Record logout
        Audit->>DB: Insert audit event
        Auth->>Session: Invalidate session and clear context
        Auth-->>User: 204 No Content
    end
```

