# Developer MCP Tools Contract

## Transport and scope

The backend connects to the standalone `mcpdevserver` using the configured MCP SSE connection (`MCP_DEV_SERVER_URL`, default `http://localhost:8082`). MCP tools are exposed only to the developer chat client; savings/customer chat tools must not receive tools whose names start with `developer_`.

The tools are read-only and bound to one configured documentation root and one configured GitHub owner/repository/branch.

## Tools

| Tool | Input | Behavior |
|---|---|---|
| `developer_search_documentation` | Search query | Searches Markdown recursively beneath the configured Documentation root and returns bounded excerpts with relative source paths |
| `developer_read_documentation` | Relative Markdown path | Reads one Markdown file only if it resolves beneath the configured Documentation root |
| `developer_search_repository` | Search query | Searches code in the one configured GitHub repository and branch; query qualifiers cannot override repository scope |
| `developer_read_repository_file` | Repository-relative path | Reads one file from the configured repository branch; traversal paths are rejected |

## Result format

Evidence results include `SOURCE: type=<documentation|github>; path=...; url=...; revision=...` and content/excerpt text. A local document uses `revision=working-tree`; GitHub source uses `revision=branch:<configured-branch>`.

No-evidence and failures use explicit `SOURCE_STATUS: no_evidence` or `SOURCE_STATUS: unavailable` results with a source and reason. The backend evidence tracker consumes these markers to determine answer citations/status.

## Safety and failure behavior

- Documentation files are Markdown-only, recursively searched, bounded, redacted, and treated as untrusted evidence.
- GitHub credentials are sent only in the HTTP authorization header and must never appear in logs or returned tool content.
- Do not add arbitrary repository selection, write operations, shell execution, commits, or pull-request actions.
- Keep the MCP listener on loopback for local use; any remote deployment requires private-network isolation and must not expose the unauthenticated MCP endpoint publicly.
