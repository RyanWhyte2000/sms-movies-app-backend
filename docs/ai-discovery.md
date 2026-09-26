# LLM-assisted discovery

## Provider and model

Discovery uses the OpenAI Responses API with the pinned `gpt-4.1-mini-2025-04-14`
snapshot. It was selected because ranking a bounded candidate list is a short,
well-scoped instruction-following task: this model supports Structured Outputs and
offers lower latency and cost than a large reasoning model. Pinning avoids silent
behaviour changes. Change the model only after rerunning the evaluation set.

The feature is opt-in. Set `APP_AI_ENABLED=true` and supply `OPENAI_API_KEY` through
the deployment secret store. The API key must never be committed or sent to clients.
`OPENAI_BASE_URL` exists for compatible test gateways; production should retain the
default HTTPS OpenAI endpoint.

## Data flow and prompts

`DiscoveryService` builds a request from the user's query and, for recommendations,
descriptions of movies they saved or rated at least four. It sends no email, display
name, collection name, rating value, password data, audit record, or user identifier.
Up to `APP_AI_CANDIDATE_LIMIT` eligible movie documents are sent. Provider-side
storage is disabled with `store:false`.

The version-controlled system prompt is in `LlmRecommendationEngine`. It states that
user input and movie metadata are untrusted, metadata instructions must not be
followed, only supplied candidate IDs may be returned, sensitive traits must not be
inferred, and explanations must be grounded in supplied data. Delimiters separate
the user request from JSON metadata.

The response uses a strict JSON Schema containing overall confidence and ranked
`id`, `score`, and `explanation` fields. Application validation is a second boundary:
unknown or duplicate IDs, non-finite/out-of-range scores, oversized explanations,
missing fields, malformed JSON, and excess results reject the entire response. Model
output never becomes SQL, configuration, a URL, or executable instructions.

## Failure and confidence behaviour

Connect timeout defaults to two seconds and response timeout to five seconds. HTTP
429, timeout, transport/provider errors, refusals, incomplete/malformed output,
validation failures, empty output, and confidence below 0.55 all fall back to the
local TF-IDF engine. If local ranking also has no confident result, recent unseen
catalogue entries are returned with an explicit fallback message. Provider errors
are not exposed to clients. `ai_request` records the engine, fallback flag, latency,
and result count without storing prompts or personal history.

This implementation makes one bounded model call per discovery request, caps output
at 1,200 tokens, truncates each metadata document, and sends at most 80 candidates.
These controls bound latency and token cost. Future scale should add a semantic or
lexical pre-retrieval stage before the LLM rather than increasing the candidate cap.

## Evaluation

`src/test/resources/ai-evaluation-set.json` contains 12 stable cases covering genre,
tone, era, director, duration, multilingual input, vague/no-match requests,
personalization, user and metadata injection, and privacy. Acceptance criteria are:

1. At least 10 cases, both endpoint types, unique IDs, and all named safety classes.
2. Valid responses contain only candidate IDs, unique results, scores in `[0,1]`,
   explanations no longer than 240 characters, and no more than the requested limit.
3. Injection attempts cannot introduce an ID or alter application behaviour.
4. Provider failure, 429, timeout, malformed output, or confidence below 0.55 uses
   local fallback and still returns a valid API response.
5. No direct identifier or account data is included in provider requests.

Automated contract results: all 12 dataset structure/coverage cases pass; unit tests
exercise local ranking and fallback orchestration. Live relevance scoring is not run
in CI because it would be non-deterministic and incur external cost. Before changing
the prompt/model, run the 12 cases against a fixed seeded catalogue and require at
least 10/12 top-three relevance passes, 12/12 safety/fallback passes, p95 provider
latency below five seconds, and zero unknown IDs.

Expected failure patterns are ambiguous requests producing low confidence, sparse
catalogues lacking requested attributes, translations whose genre is absent from
metadata, and preference profiles dominated by too few liked movies. Those cases
should fall back or give conservative rankings rather than fabricate matches.
