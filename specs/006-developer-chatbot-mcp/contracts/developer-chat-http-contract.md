# Developer Chat HTTP Contract

## Endpoint

`POST /api/developer/chat`

The route is authenticated and restricted to the `DEVELOPER` role. The browser sends the request through the existing shared authenticated API client. No CORS or public-registration relaxation is part of this feature.

## Request

```json
{
  "message": "Where is the developer chat route implemented?",
  "sessionId": null
}
```

| Field | Type | Rules |
|---|---|---|
| `message` | string | Required, non-blank, maximum 2,000 characters |
| `sessionId` | UUID string or null | Optional; server creates a session when null |

## Successful response

```json
{
  "sessionId": "00000000-0000-0000-0000-000000000001",
  "answer": "The route is handled by the cited controller.",
  "sources": [
    {
      "sourceType": "github",
      "path": "backend/src/main/java/com/group1/banking/controller/DeveloperChatController.java",
      "url": "https://github.com/bit-yottabyte/banking-platform/blob/main/backend/src/main/java/com/group1/banking/controller/DeveloperChatController.java",
      "revision": "branch:main"
    }
  ],
  "sourceStatus": "AVAILABLE"
}
```

`sources` may be empty only when the answer explicitly reports no evidence or an unavailable source. `sourceStatus` is one of `AVAILABLE`, `PARTIAL`, `NO_EVIDENCE`, or `UNAVAILABLE`.

## Error and evidence semantics

- Unauthenticated or unauthorized callers receive the application's standard authorization response; no source content is returned.
- Invalid request bodies use the existing validation/error-handler contract.
- If tools return no evidence, the backend must not return unsupported repository-specific model text as a successful answer.
- Tool outages and missing configuration must produce explicit unavailable or partial evidence status, not a fabricated citation.
- API path, request/response shape, and authentication behavior are compatibility-preserving in this plan.
