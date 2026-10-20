# Quickstart: Developer Chatbot MCP

## Prerequisites

- Java 21 and the Maven wrapper
- Node.js/npm for the web client
- Access to the configured GitHub repository and an environment-supplied, read-only GitHub token
- A valid developer account and the normal local banking-platform services needed by the application

## Configure sources

In the repository-root `.env` file, set the repository scope and read-only token:

```dotenv
GITHUB_REPOSITORY_OWNER=bit-yottabyte
GITHUB_REPOSITORY_NAME=banking-platform
GITHUB_REPOSITORY_BRANCH=main
GITHUB_READ_ONLY_TOKEN=<read-only-token-from-your-secret-store>
DOCUMENTATION_ROOT=../Documentation
```

Do not paste token values into chat, commit them, or include them in logs. Confirm the selected branch is the intended one. `DOCUMENTATION_ROOT` is relative to the MCP server process working directory; the default is valid when launched from `mcpdevserver/`.

## Run focused tests

Run each command from the indicated module directory in its own PowerShell
terminal (the Maven wrappers resolve their configuration from the module):

```powershell
Set-Location .\mcpdevserver
.\mvnw.cmd test
```

```powershell
Set-Location .\backend
.\mvnw.cmd test
```

From the repository root, run the frontend tests:

```powershell
npm test -- src/pages/DeveloperChatPage.test.jsx
```

Expected outcomes: the MCP tests find nested Markdown in `Documentation/`, return safe relative citations, and report a missing root as unavailable; backend tests verify evidence status, access controls, and developer-tool isolation; frontend tests preserve answer and citation rendering.

## Start and validate locally

Start the MCP server in a terminal:

```powershell
Set-Location .\mcpdevserver
.\mvnw.cmd spring-boot:run
```

Start the backend and frontend using the repository's regular local setup instructions in separate terminals. Log in as a `DEVELOPER` and open the developer assistant.

Run these scenarios:

1. Ask, “What does the architecture overview say about the backend request flow?” The answer should cite `onboarding/project-overview/architecture-overview.md` with a `documentation` source label and `working-tree` revision.
2. Ask, “Where is the developer chat endpoint implemented?” The answer should cite the GitHub controller path with its configured branch.
3. Ask, “How does a browser chat request reach the backend, and where is that route implemented?” The answer should distinguish documentation guidance from repository observations and return sources from both when both are relevant.
4. Temporarily make one source unavailable in a local test environment. The response must disclose the unavailable source, use the other only when it provides sufficient evidence, and report `PARTIAL` or `UNAVAILABLE` as appropriate.
5. Ask a question unsupported by both sources. The answer must state that it cannot substantiate the answer and must not provide invented citations or code claims.

Verify that an unauthorized role cannot call `/api/developer/chat`, public registration cannot grant privileged roles, and the MCP server remains bound to loopback for local use.
