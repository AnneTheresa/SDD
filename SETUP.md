# Voltio - Developer Setup

Run the local stack as separate processes: PostgreSQL/pgvector in Docker, the
developer MCP server, the banking backend, and the React frontend. Start each
application in its own terminal from the working directory shown below so
relative configuration and the repository-root `.env` file resolve correctly.

## Prerequisites

- **Java 21** (check with `java -version`)
- **Node.js/npm** (for the React/Vite frontend)
- **Docker Desktop** with Docker Compose
- No separate Maven install needed; `backend/`, `mcpdevserver/`, and
  `voltio-rates-mcp-server/` each include a Maven wrapper.

## 1. Configure local environment

From the repository root, copy `.env.example` to `.env`:

```powershell
Copy-Item .env.example .env
```

In `.env`, set `GROQ_API_KEY` for chat completions and `GITHUB_READ_ONLY_TOKEN`
for repository-backed developer chat. The GitHub token must be fine-grained,
read-only, and scoped to this repository. The example config points at
`bit-yottabyte/banking-platform` on `main`; change the owner, repository, or
branch variables if your checkout uses a different source. Never commit a real
token or paste it into chat.

The MCP server defaults to `../Documentation` for `DOCUMENTATION_ROOT`. This
relative path is resolved from the MCP process working directory, so start it
from `mcpdevserver/` as shown below. Override it only when your documentation
folder is elsewhere.

The backend and MCP server load the repository-root `.env` automatically when
started from their module directories. Docker Compose also reads it. The example
database values match the local Compose defaults.

## 2. Start the databases

Both application data (`banking_core`) and chatbot data (`banking_chat`, including
pgvector) use the PostgreSQL/pgvector service on `localhost:5433`. Start the
database and its one-time core-database initializer from the repository root:

```powershell
docker compose up -d pgvector create-banking-core
```

Wait for `pgvector` to become healthy before starting the backend. The backend
uses PostgreSQL for both datasources; it does not create a local H2 database.

## 3. (Optional) Start the GIC rates MCP server

The savings chatbot uses this separate server for live GIC rate lookups. It is
independent of the developer repository assistant and can be omitted if you do
not need GIC rate answers. Start it from its module directory:

```powershell
Set-Location .\voltio-rates-mcp-server
.\mvnw.cmd spring-boot:run
```

It listens on port **8081**. Keep it running in its own terminal.

## 4. Start the developer repository MCP server

The developer assistant uses the `mcpdevserver/` Spring application. It reads
Markdown recursively from `Documentation/` and accesses only the configured
GitHub repository and branch. Start it from `mcpdevserver/`; this is required
for `DOCUMENTATION_ROOT=../Documentation` and root `.env` discovery to resolve
as intended.

```powershell
Set-Location .\mcpdevserver
.\mvnw.cmd spring-boot:run
```

It listens on `127.0.0.1:8082` and exposes read-only documentation and GitHub
tools. It has no user-level authentication: keep it bound to loopback for local
development and restrict it to a private network if deploying it separately.
Keep it running in its own terminal.

## 5. Start the banking backend

After PostgreSQL is healthy and the developer MCP server is running, start the
backend from `backend/`:

```powershell
Set-Location .\backend
.\mvnw.cmd spring-boot:run
```

It listens on port **8080**. Flyway applies database migrations during startup.
The customer-facing chatbot uses the `banking_chat` database; the developer
assistant calls the MCP server at `MCP_DEV_SERVER_URL` (default
`http://localhost:8082`).
With `spring-boot:run`, the terminal should remain attached after Maven reports
`BUILD SUCCESS`; look for `Started DigitalBankingPlatformApplication` and the
Tomcat port-8080 startup message. A successful build alone does not mean the
server is running. If the command returns to the prompt, inspect the final
startup log for `APPLICATION FAILED TO START` or an exception before retrying.
The developer MCP startup diagnostic should report the available tool callback
count; for this repository's configured documentation/GitHub server, it should
be greater than zero. Verify the HTTP server separately with:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

The response should include `status: UP`. Keep the `spring-boot:run` terminal
open while using the frontend.

The developer assistant is available only to users with the `DEVELOPER` role;
public registration cannot grant this role. To use the local developer persona,
set `SEED_PERSONAS_ENABLED=true` in `.env` before starting the backend and sign
in as `seed.developer@voltio.test` with the shared seed password described
below. For other environments, accounts are provisioned through the protected
`POST /api/auth/provision` endpoint. Do not expose this provisioning endpoint
or the unauthenticated MCP service publicly.

## 6. Start the frontend

From the repository root in another terminal:

```powershell
npm install
npm run dev
```

It listens on port **5173** and proxies `/api` requests to the backend on
`8080`. Set `VITE_DEV_BACKEND_TARGET` to override the backend URL.

### macOS/Linux command equivalents

Use `cp .env.example .env` to create the environment file. Run each command
below in its own terminal, replacing `.\mvnw.cmd` with `./mvnw`:

```bash
docker compose up -d pgvector create-banking-core
```

```bash
cd mcpdevserver
./mvnw spring-boot:run
```

```bash
cd backend
./mvnw spring-boot:run
```

```bash
npm install
npm run dev
```

## Using the shared personas

The repository ships a small cast of seeded customers so you do not have to hand-build
one to try something out. They are **off by default** - nothing is seeded unless you ask.

Turn seeding on when you start the backend:

```bash
SEED_PERSONAS_ENABLED=true
```

Set this in the repository-root `.env` before starting the backend. This is
intended for local development, CI, QA, and demo environments only; never enable
persona seeding in production.

| Persona | Login | What it is for |
|---|---|---|
| `salaried` | `seed.salaried@voltio.test` | The ordinary customer. Regular income and spending, 18 months of history - risk scoring gives a real score and the chatbot personalises. |
| `goalSaver` | `seed.goalsaver@voltio.test` | Saving toward a near-term goal at 45% progress. Two accounts. |
| `operations` | `seed.operations@voltio.test` | Administrator. Owns a FROZEN account so restriction workflows can be demonstrated. |
| `sparse` | `seed.sparse@voltio.test` | Deliberately data-poor. Triggers the chatbot fallback and the risk `INSUFFICIENT_DATA` status. |
| `riskAnalyst` | `seed.riskanalyst@voltio.test` | Risk Analyst role. Also proves a non-administrator staff role does **not** inherit freeze/unfreeze powers. |
| `complianceObserver` | `seed.compliance@voltio.test` | Compliance/Audit Observer role, read-only oversight. |
| `developer` | `seed.developer@voltio.test` | Developer role for the repository-backed development assistant; has no banking customer or account data. |

Every role the platform recognizes has a persona holding it, and a catalogue invariant (C11)
makes that a startup failure if a role is ever added without one.

All seeded personas share the password on `PersonaSeeder.SEED_PASSWORD`. These credentials work only
in non-production environments - seeding refuses to run against a production-shaped
configuration, and is disabled by default everywhere else.

**Re-running the app does not reset them.** Seeding fills in what is missing and never
overwrites, so a scenario that moved money leaves the persona changed. To restore one:

```java
resetService.reset("goalSaver");   // just this persona
resetService.resetAll();           // all of them
```

Reset is scoped per persona on purpose: in a shared QA environment, restoring the persona
you are working on must not destroy a colleague's half-finished run.

**Running the seed tests requires a live PostgreSQL** on port 5433, because they run against
the real engine and Flyway-managed schema rather than an in-memory substitute:

```bash
docker compose up -d pgvector create-banking-core
cd backend && ./mvnw test -Dtest='com.group1.banking.seed.*Test'
```

Note the `*Test` suffix in that pattern - `com.group1.banking.seed.*` matches nothing and
surefire reports success having run zero tests.

Their full definitions - including the expected outcome for each of the four features -
live in [`backend/src/main/resources/personas/voltio-personas.yaml`](backend/src/main/resources/personas/voltio-personas.yaml).
That file is the single source of truth: the tests read their expected values from it, so
editing a persona without updating its expectations fails `PersonaCatalogueValidationTest`
rather than drifting silently. When writing tests, read values from the catalogue through
the `Personas` accessor instead of restating them.

## Running tests

Run Maven tests from each module directory in its own terminal:

```powershell
Set-Location .\mcpdevserver
.\mvnw.cmd test
```

```powershell
Set-Location .\backend
.\mvnw.cmd test
```

Run frontend tests from the repository root:

```powershell
npm test
```

On macOS/Linux, replace `Set-Location` with `cd` and `mvnw.cmd` with
`./mvnw`.

## Known local-dev gotchas

- **Database connection failures.** Confirm `docker compose ps` reports
  `banking-pgvector` as healthy and that `create-banking-core` completed. Check
  `APP_DB_URL`, `CHATBOT_DB_URL`, and credentials in `.env` against the Compose
  defaults. Do not delete the `pgvector-data` volume to resolve schema issues;
  the app schema is managed by Flyway.
- **Missing documentation citations.** Confirm the MCP process was started
  from `mcpdevserver/`, `DOCUMENTATION_ROOT` is `../Documentation` (or an
  existing Markdown directory), and contributor pages are under that root. A
  missing root is reported as an unavailable documentation source.
- **GitHub repository unavailable.** Confirm the configured owner, repository,
  branch, and fine-grained read-only token in `.env`. Ownership changes do not
  update local configuration or grant a token access. Authentication and
  permission failures are reported as unavailable source status, not verified
  code answers. The assistant can read only the configured branch; code on a
  local or feature branch is not visible until it is pushed and
  `GITHUB_REPOSITORY_BRANCH` is set to that branch. Restart `mcpdevserver/`
  after changing `.env` so it reloads the source settings.
- **Developer chat still reports unavailable sources after a code/config change.**
  Restart the backend after changing MCP client wiring or connection settings.
  Confirm the developer MCP server is listening at `MCP_DEV_SERVER_URL` and that
  its Documentation search returns a citation before retrying the chat. If the
  model request fails or GitHub is unavailable, the backend directly searches
  contributor Markdown and returns a relevant excerpt with its document citation
  when one is found; it does not claim that excerpt verifies current repository
  code.
- **Developer MCP diagnostic reports no tool callbacks.** Confirm the backend
  was restarted after MCP client configuration changes and that the client
  connection named `developer-server` is enabled in
  `backend/src/main/resources/application.properties`. The diagnostic should
  report the developer tools or a clear discovery warning, not silently report
  an empty provider.
