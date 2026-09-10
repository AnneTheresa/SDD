# Voltio – Developer Setup

Getting the full stack running locally: frontend, backend, the chatbot's vector
database, and the optional GIC-rates MCP server.

## Prerequisites

- **Java 21** (the backend won't build on anything older — check with `java -version`)
- **Node.js** (for the React/Vite frontend)
- **Docker** (for the chatbot's Postgres/pgvector database)
- No separate Maven install needed — both `backend/` and `voltio-rates-mcp-server/`
  ship their own Maven wrapper (`mvnw`/`mvnw.cmd`)

## 1. Environment variables

Copy the example env file at the repo root and fill in a real Groq key:

```
cp .env.example .env
```

Then edit `.env` and set:

```
GROQ_API_KEY=<get one at https://console.groq.com/keys>
```

The other values (`CHATBOT_DB_URL`, `CHATBOT_DB_USERNAME`, `CHATBOT_DB_PASSWORD`)
already match the Docker Compose defaults below, so you don't need to touch them
for local dev. The backend reads this file automatically via `springboot4-dotenv`
(configured to look one directory up from `backend/`) — there's nothing to `export`
or source manually.

## 2. Start the chatbot's database

The savings chatbot uses a separate Postgres + pgvector instance (RAG knowledge
base + chat interaction log) from the main app's data:

```
docker compose up -d pgvector
```

This starts Postgres on `localhost:5433` (container name `banking-pgvector`).
The main app's own data (accounts, customers, transactions, etc.) uses a local
H2 file database and needs no setup — it's created automatically on first run.

## 3. (Optional but recommended) Start the GIC rates MCP server

The chatbot can answer GIC rate questions by calling a separate MCP server.
The backend degrades gracefully without it (chat still works, just without
live GIC rates), but for full functionality start it first:

```
cd voltio-rates-mcp-server
mvn spring-boot:run
```

Runs on port **8081**. Leave it running in its own terminal.

## 4. Start the backend

```
cd backend
mvn spring-boot:run
```

(Use `./mvnw spring-boot:run` instead of `mvnw.cmd` if you're on macOS/Linux.)

Runs on port **8080**. First startup will take a bit longer while it sets up
the local H2 database and downloads dependencies.

## 5. Start the frontend

From the repo root (a separate terminal):

```
npm install
npm run dev
```

Runs on port **5173** and proxies `/api` and related paths to the backend on
`8080` (see `vite.config.js` if you need to point it at a non-default backend
URL via `VITE_DEV_BACKEND_TARGET`).


## Running tests

Backend:
```
cd backend
mvnw.cmd test
```

Frontend:
```
npm test
```

## Known local-dev gotchas

- **H2 schema drift after pulling in schema changes.** The backend uses
  `spring.jpa.hibernate.ddl-auto=update`, not a migration tool, against a
  persistent local file (`backend/data/digitalbankdb.mv.db`). If you pull a
  branch that adds a new `NOT NULL` column to an existing table, Hibernate can
  silently fail to add it to your *existing* local database, and you'll see
  `Column "X" not found` or `NULL not allowed for column "X"` errors at
  runtime rather than at startup. If that happens: either delete
  `backend/data/digitalbankdb.*` and let it rebuild from scratch (loses local
  test data), or open the H2 console (`http://localhost:8080/h2-console`,
  JDBC URL `jdbc:h2:file:./data/digitalbankdb;AUTO_SERVER=TRUE`, user `sa`, no
  password) and add the missing column(s) manually with `ALTER TABLE`.
- **Two H2 data folders can exist** (`data/` at repo root and
  `backend/data/`) depending on which directory you launched
  `spring-boot:run` from. The one that matters is wherever your terminal's
  working directory was when you started the backend — always run it from
  inside `backend/`.
