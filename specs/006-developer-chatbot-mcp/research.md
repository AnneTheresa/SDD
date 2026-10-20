# Research: Developer Chatbot MCP

## Decision: Search the contributor documentation folder

- **Decision**: Configure the MCP server's default documentation root as `../Documentation` when launched from `mcpdevserver/`. Keep `DOCUMENTATION_ROOT` as an environment override and document that relative paths resolve from the MCP process working directory.
- **Rationale**: `DocumentationTools` recursively searches Markdown under its configured root, but `mcpdevserver/src/main/resources/application.properties` currently defaults to `../specs`. `SETUP.md` repeats that default. The repository has a `Documentation/README.md` and a nested architecture guide, so the current configuration searches the wrong corpus for the contributor documentation use case.
- **Alternatives considered**: Add the documentation folder to the specs search root; rejected because feature specifications and contributor documentation have different purposes and should be identifiable as distinct evidence sources. Hardcode an absolute path; rejected because local, CI, and deployed working directories differ.

## Decision: Keep documentation search read-only and Markdown-based

- **Decision**: Continue to scan only Markdown files recursively beneath the configured documentation root and retain path containment checks, bounded results, redaction, and relative-path citations.
- **Rationale**: The current `DocumentationTools` already implements these properties and the current `Documentation/` content is Markdown. A root-path correction is the smallest reliable fix. Tests must verify nested files, containment, missing-root status, and source citations using the configured root.
- **Alternatives considered**: Introduce embeddings, a vector index, or a new documentation database; rejected for this release because it increases operational and consistency risk without evidence that it is needed to make the current folder visible.

## Decision: Make source use observable and answer quality verifiable

- **Decision**: For contributor setup and architecture questions, guide the developer assistant to search documentation first; use GitHub tools for code-specific questions and both sources when a question crosses the boundary. Keep evidence-tracked response semantics and verify tool invocation and citations with source-backed test cases.
- **Rationale**: The developer system prompt currently directs the model to use both tools but does not establish this distinction. `DeveloperChatService` rejects unsupported answers without evidence, and tool outputs carry document or repository paths. That behavior should remain the final safeguard rather than adding uncited fallback answers.
- **Alternatives considered**: Return model text when no tool evidence was captured; rejected because it would turn source failures into success-shaped, unverifiable answers. Force every question through both sources; rejected because it adds latency and unnecessary GitHub calls for documentation-only guidance.

## Decision: Treat GitHub access configuration independently from repository ownership

- **Decision**: Configure the current repository scope through `GITHUB_REPOSITORY_OWNER`, `GITHUB_REPOSITORY_NAME`, and `GITHUB_REPOSITORY_BRANCH`; configure `GITHUB_READ_ONLY_TOKEN` outside committed source. Use a fine-grained token with read-only repository content access, verify the configured owner/name/branch and token against GitHub's API, and never log token values.
- **Rationale**: The MCP client scopes code search and file reads using the configured owner, repository, and branch. Changing repository ownership does not update those runtime values or grant token access. Existing code translates authentication, access/rate-limit, not-found, and network failures into explicit unavailable source states.
- **Alternatives considered**: Put the token or repository identity in source code; rejected by the security and environment-parity requirements. Allow arbitrary repositories from user queries; rejected because it exceeds the configured repository boundary.

## Decision: Preserve current chat and MCP contracts

- **Decision**: Keep `POST /api/developer/chat`, its request/response DTOs, the four read-only MCP tool names, and the separate developer-tool isolation. Add focused behavior/configuration tests; change a contract only if those tests demonstrate that current behavior cannot satisfy the feature requirements.
- **Rationale**: The API, frontend chat page, MCP server tools, access checks, and evidence tracker already exist. The issue under investigation is primarily source selection/configuration and answer grounding, not a need for a new public endpoint.
- **Alternatives considered**: Add a separate documentation endpoint or expose MCP tools directly to the browser; rejected because the backend is already the authenticated chat boundary.

## Resolved implementation unknowns

- The MCP module is a standalone Java 21 / Spring Boot 4.1.1 / Spring AI 2.0.1 application.
- Documentation citations use repository-relative paths and `revision=working-tree`; GitHub citations use a configured branch and clickable file URLs.
- The default local GitHub API is `https://api.github.com`; repository API base URL, scope, branch, and token are environment-configurable.
- The current documentation folder contains Markdown and is suitable for the existing reader/search tools.
- Existing chat conversations are held in memory per authenticated user and session; no new persistence schema is required for the documentation-root correction.
