# Frontend API contract

All paths are relative to the backend origin. JSON request/response bodies use
camelCase. OpenAPI is available at `/v3/api-docs` and Swagger UI at
`/swagger-ui/index.html`, grouped by controller category.

All resource identifiers are UUID strings. Clients must treat identifiers as opaque
values and must not parse, increment or derive meaning from them.

## Authentication and browser setup

The API uses server-side JDBC sessions, HttpOnly cookies and CSRF protection. HTTP
Basic and HTML login redirects are disabled. Private endpoints return 401 without
an authenticated session. Registering does not automatically log the user in.

1. `GET /api/csrf` with cookies enabled. Save `headerName` and `token`.
2. Include that header/token and cookies on register and login requests.
3. After login, fetch `/api/csrf` again: login rotates the session ID and CSRF token.
4. Send cookies on all private requests and the new token on every write.
5. POST logout with the token; the session is invalidated. Fetch a new CSRF token
   before the next login.

For fetch, use `credentials: "include"`. Same-origin deployment works by default.
For a separate frontend origin, set `APP_CORS_ALLOWED_ORIGINS` to a comma-separated
list of exact origins, e.g. `http://localhost:5173`. Wildcard origins are rejected.
For production HTTPS set `SERVER_SERVLET_SESSION_COOKIE_SECURE=true`; cross-site
frontend/backend deployments also need a considered SameSite cookie configuration.
Do not store passwords or session identifiers in localStorage.

## Accounts — AuthController, AccountController, CsrfController

| Method | Path | Body / result |
| --- | --- | --- |
| GET | `/api/csrf` | `{headerName, token}`; anonymous access allowed |
| POST | `/api/auth/register` | `{email, password, displayName}` → 201 user |
| POST | `/api/auth/login` | `{email, password}` → 200 user + session cookie |
| POST | `/api/auth/logout` | No body → 204 |
| GET | `/api/me` | `{id: <UUID>, email, displayName, createdAt}` |

Passwords must have 12–128 characters at registration. Emails are normalized to
lowercase. Registration creates a default `My movies` collection. Duplicate emails
return 409. Public registration always creates a USER, never an ADMIN. Account
responses never contain password hashes. Login failures use a generic 401 message.

## Catalogue — MovieController, GenreController, PersonController

| Method | Path | Result |
| --- | --- | --- |
| GET | `/api/movies?q=&castId=&directorId=&genreId=&page=0&size=20` | Paginated movies |
| GET | `/api/movies/{movieId}` | Movie details, credits, genres, externalRatings, personalRating |
| GET | `/api/genres` | List of `{id, name}` |
| GET | `/api/people?q=&role=CAST&page=0&size=20` | Paginated `{id, name}`; role also accepts DIRECTOR |

Filters combine with AND. Text search matches title, description, people and genre
names case-insensitively. SQL wildcard characters are treated as literal text.
Movie list fields are `id` (UUID), `title`, `description`, `director`, `genre`,
`releaseDate`, and `durationMinutes`. Details add relationship arrays and the
signed-in user's rating (null when unrated). External ratings include `source`,
`score`, `scaleMax`, `voteCount`, and `fetchedAt`.

## Collections — CollectionController

| Method | Path | Body / result |
| --- | --- | --- |
| GET | `/api/me/collections?page=0&size=20` | Paginated owned collections |
| POST | `/api/me/collections` | `{name}` → 201 + Location |
| GET | `/api/me/collections/{id}?page=0&size=20` | `{collection, movies: <page>}` |
| PATCH | `/api/me/collections/{id}` | `{name}` → updated collection |
| DELETE | `/api/me/collections/{id}` | 204 |
| PUT | `/api/me/collections/{id}/movies/{movieId}` | No body → 204 |
| DELETE | `/api/me/collections/{id}/movies/{movieId}` | 204 |

Collection names are 1–120 nonblank characters. Ownership comes from the session.
Another user's collection returns 404. Adding the same movie twice or removing an
already absent item succeeds without duplicate state or audit events. Deleting a
collection deletes its items and share links, but preserves personal ratings and
historical audit events.

## Personal ratings — RatingController

| Method | Path | Body / result |
| --- | --- | --- |
| GET | `/api/me/ratings?page=0&size=20` | Paginated ratings |
| PUT | `/api/me/ratings/{movieId}` | `{score: 1..5}` → rating |
| DELETE | `/api/me/ratings/{movieId}` | 204, including when already absent |

Ratings contain `movieId`, `score`, `createdAt`, `updatedAt`. PUT creates or updates
one rating per user/movie, enforced by the database. Repeating an unchanged score
does not create another audit event. Ratings remain independent of collections.

## Sharing — ShareController, SharedCollectionController

| Method | Path | Body / result |
| --- | --- | --- |
| POST | `/api/me/collections/{id}/shares` | Optional `{expiresAt: <future ISO timestamp>}` → 201 `{id, token, path, expiresAt}` |
| GET | `/api/me/collections/{id}/shares` | Active unexpired link metadata; no tokens |
| DELETE | `/api/me/collections/{id}/shares/{shareId}` | Revoke → 204 |
| GET | `/api/shared/collections/{token}?page=0&size=20` | Anonymous read-only `{name, movies: <page>}` |

The raw share token is returned only on creation; only its SHA-256 hash is stored.
Anyone possessing the link can read the collection. Missing, expired and revoked
links return 404. Shared responses contain no owner email, private ratings, audit
history, or tokens. Responses disable caching. Treat URLs as credentials: avoid
logging or sending them in analytics. Share-link creation is not idempotent;
repeating POST deliberately creates another independently revocable link.

## AI discovery — SearchController, RecommendationController

| Method | Path | Body |
| --- | --- | --- |
| POST | `/api/search/natural-language` | `{query, limit: 10}` |
| POST | `/api/recommendations` | `{prompt: "optional preferences", limit: 10}`; `{}` is valid |

Responses contain `engine`, `fallbackUsed`, `message`, and
`results: [{movie, relevance, explanation}]`. Limit is 1–20. Text is at most 1000
characters. `engine` is `local-tfidf-v1`: a working local content-based baseline,
not a hosted LLM. It ranks title, synopsis, primary director, primary genre and
year using TF-IDF cosine similarity. It recognizes a few synonyms, but does not
reliably interpret negation, date ranges or complex semantic requests.

Recommendations use saved movies and ratings of 4–5; low-rated movies do not
contribute positive preference signals. Already saved or rated movies are excluded.
With insufficient history or no confident match, recent unseen catalogue entries
are returned with `fallbackUsed: true` and relevance 0. An empty catalogue returns
an empty list. Relevance is similarity, not calibrated confidence. The baseline
examines the latest 1000 catalogue movies to bound work; a production search index
or provider adapter should replace it for larger catalogues.

No prompts or movie metadata leave the process. Text is never executed as commands
or database queries. Request metadata records engine, duration, result count,
fallback use and correlation ID; raw prompts are not persisted. There are no
provider timeouts, rate limits or charges in this implementation. A future hosted
provider requires its own validated contract, failure handling and evaluation.

## Activity — AuditController, AnalyticsController

| Method | Path | Body / result |
| --- | --- | --- |
| GET | `/api/me/audit-events?action=&from=&to=&page=0&size=20` | Paginated own activity, newest first |
| POST | `/api/analytics/events` | Event below → 202 |

Audit timestamps use UTC. `from`/`to` accept ISO instants, inclusively. Events contain
`id`, `action`, `resourceType`, `resourceId`, `outcome`, `occurredAt`, `correlationId`.
Account registration, successful/failed login, logout, collection mutations, rating
mutations, shares, searches and recommendations are recorded. No public endpoint
can create, alter or delete audit records. Successful state changes and their audit
writes share a transaction. Unknown-account login failures have no acting user and
are not visible through a user's activity view.

```json
{
  "eventId": "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
  "eventName": "RECOMMENDATION_SELECTED",
  "movieId": "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
  "journeyId": "11111111-2222-4333-8444-555555555555",
  "consentGranted": true
}
```

Client events are restricted to `SEARCH_RESULT_SELECTED`, `RECOMMENDATION_VIEWED`,
and `RECOMMENDATION_SELECTED`. `journeyId` is optional. Only send these after
appropriate consent. Arbitrary properties and raw user text are not accepted.
Reuse `eventId` for retries: an identical event is accepted once; reusing its ID
for another payload/user returns 409. The server derives user and timestamp.
Search/recommendation completion, collection-item changes and rating changes are
instrumented on the backend, avoiding client assertions about completed operations.
This implementation does not yet provide consent-preference storage or retention
jobs; configure the relevant policy before production use.

## Catalogue administration

Only ADMIN accounts may use the existing POST/PUT/DELETE `/api/movies` routes,
POST `/api/people`, POST `/api/genres`, and
PUT `/api/movies/{movieId}/external-ratings/{source}`.

Movie create/update example:

```json
{
  "title": "Example Movie",
  "description": "A fictional space comedy.",
  "director": "Example Director",
  "genre": "Comedy",
  "releaseDate": "2025-01-01",
  "durationMinutes": 100,
  "castIds": [],
  "directorIds": [],
  "genreIds": []
}
```

The three ID lists are optional and refer to existing people/genres. Legacy
`director` and `genre` fields remain required and are linked into the normalized
relationships. They represent the primary names displayed in the movie summary.
Legacy director names reuse the first matching person; use explicit IDs to
represent additional namesakes. People are created with `{name}`; genre names are
unique. Movie PUT replaces all editable fields and relationships.

External rating example: `{score: 8.4, scaleMax: 10, voteCount: 100}`. The rating
must fit its scale. This endpoint stores/imports sourced ratings; no automatic
third-party movie-data provider is configured. Do not represent synthetic data as
real provider ratings.

## Common responses

Lists use `{content, page, size, totalElements, totalPages}`. Pages start at 0 and
size is 1–100. Invalid body/query values return 400; body field errors have an
`errors` map. Other errors use Problem Detail JSON: 401 for missing/invalid login,
403 for forbidden operations/CSRF failures, 404 for missing or inaccessible
resources, 409 for integrity conflicts and 500 for unexpected server failures.
Swagger lists only the failure statuses applicable to each operation. Every request
returns `X-Correlation-ID`.
Do not expose SQL or stack traces to users. POST create calls may create more than
one resource if retried; use PUT for collection membership and personal ratings.

| Status | Documented when |
| --- | --- |
| 400 | The operation accepts validated input, pagination, filters or UUID parameters |
| 401 | The operation requires a session, or login credentials are invalid |
| 403 | The operation is CSRF-protected or requires ADMIN authority |
| 404 | The operation addresses a movie, collection, share or other owned resource |
| 409 | Registration, analytics deduplication or a uniqueness constraint can conflict |
| 500 | Any operation encounters an unexpected server failure |

Standard problem response:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Resource was not found.",
  "instance": "/api/movies/aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
  "correlationId": "11111111-2222-4333-8444-555555555555"
}
```

Validation failures use the same shape and may additionally contain an `errors`
map whose keys are request fields and whose values are safe validation messages:

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed.",
  "instance": "/api/me/ratings/aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
  "correlationId": "11111111-2222-4333-8444-555555555555",
  "errors": {
    "score": "must be less than or equal to 5"
  }
}
```

The `errors` property is optional and is omitted for non-validation failures.
