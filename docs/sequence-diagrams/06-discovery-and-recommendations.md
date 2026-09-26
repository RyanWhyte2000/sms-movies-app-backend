# Natural-language search and recommendations

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant API as Discovery Controller
    participant Discovery as Discovery Service
    participant Local as Local TF-IDF Engine
    participant LLM as LLM Recommendation Engine
    participant OpenAI as OpenAI Responses API
    participant Activity as Activity Reporter
    database DB

    User->>API: POST natural-language search or recommendations
    API->>Discovery: search/recommendations(user, prompt, limit)
    Discovery->>DB: Load latest 1000 catalogue movies

    alt Personalized recommendations
        Discovery->>DB: Load saved movies and ratings
        Discovery->>Discovery: Build profile from saved and 4-5 rated movies
        Discovery->>Discovery: Exclude saved and all rated movies
    else Natural-language search
        Discovery->>Discovery: Use request text directly
    end

    Discovery->>LLM: rankWithStatus(query, documents, exclusions, limit)
    LLM->>Local: Pre-rank candidate set with TF-IDF
    Local-->>LLM: Candidate ranking

    alt AI disabled or API key absent
        LLM->>Local: Produce local ranking
        Local-->>LLM: Local matches
    else AI enabled
        LLM->>OpenAI: Structured-output ranking request
        alt Valid response with sufficient confidence
            OpenAI-->>LLM: Candidate IDs, scores, explanations
            LLM->>LLM: Validate IDs, limits, scores and explanations
        else Timeout, provider error, invalid output, or low confidence
            LLM->>Local: Fall back to TF-IDF
            Local-->>LLM: Local matches
        end
    end

    LLM-->>Discovery: Outcome + engine + fallback status

    alt No confident matches
        Discovery->>Discovery: Select recent unseen catalogue entries
    else Matches available
        Discovery->>Discovery: Map matches to movie responses
    end

    Discovery->>Activity: Record discovery audit and completion analytics
    Activity->>DB: Insert audit and analytics events
    Discovery->>DB: Store AI request metadata
    Note over Discovery,DB: Stores engine, latency, result count,<br/>fallback and correlation ID - not raw prompt
    Discovery-->>API: DiscoveryResponse
    API-->>User: Results, explanations and fallback status
```

