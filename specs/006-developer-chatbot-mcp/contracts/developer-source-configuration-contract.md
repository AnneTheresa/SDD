# Developer Source Configuration Contract

Configuration is provided through environment variables or the local root `.env` file. The MCP server's local dotenv setup reads the repository-root file when launched from `mcpdevserver/`. Do not commit token values.

| Variable | Purpose | Default |
|---|---|---|
| `DOCUMENTATION_ROOT` | Markdown documentation root, relative to the MCP process working directory unless absolute | `../Documentation` |
| `GITHUB_API_BASE_URL` | GitHub-compatible API base URL | `https://api.github.com` |
| `GITHUB_REPOSITORY_OWNER` | Configured repository owner | unset; repository tools report unavailable |
| `GITHUB_REPOSITORY_NAME` | Configured repository name | unset; repository tools report unavailable |
| `GITHUB_REPOSITORY_BRANCH` | Repository branch to search/read | `main` |
| `GITHUB_READ_ONLY_TOKEN` | Read-only credential for repository API calls | unset; repository tools report unavailable |
| `MCP_DEV_SERVER_URL` | Backend connection URL to the MCP server | `http://localhost:8082` |

For this repository, the current configured scope is `bit-yottabyte/banking-platform` on its selected branch. Verify that the branch is the intended source of truth in each environment. A fine-grained token must have only the read-only repository permissions required for metadata, code search, and file contents.

Changing repository ownership alone does not update environment values or authorize an existing token. Verify the runtime owner/name/branch and credential access without printing or logging the token. Authentication failure, access denial/rate limiting, missing repository/path, and network failure must remain distinguishable unavailable outcomes.
