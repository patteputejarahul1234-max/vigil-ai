# Vigil AI — Stage 1 (Foundation)

Auth & identity foundation for the Vigil AI accountability app: JWT auth,
Google/GitHub OAuth, email verification, forgot/reset password,
role-based access, and profile management.

## Structure
- `backend/` — Spring Boot 3 (Java 21), Spring Security, JPA, JWT (jjwt), OAuth2 client, Swagger, H2 (dev) / MySQL (prod-ready)
- `frontend/` — React 18 + TypeScript + Vite + Tailwind

## Running the backend
```bash
cd backend
mvn spring-boot:run
```
Uses an in-memory H2 database by default — nothing to install to try it out.
Swagger UI: http://localhost:8080/swagger-ui.html
H2 console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:vigilai`)

To use real Google/GitHub login, set env vars before starting:
```
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
MAIL_USERNAME=...        # Gmail address for sending verification/reset emails
MAIL_PASSWORD=...        # Gmail App Password, not your normal password
JWT_SECRET=...           # any long random string, 32+ chars
```
Without these, register/login/JWT still work end to end — only the OAuth
buttons and outbound emails need them (emails fail silently to a log line
in dev so registration/reset flows aren't blocked).

## Running the frontend
```bash
cd frontend
npm install
cp .env.example .env
npm run dev
```
Visit http://localhost:5173

## API surface (Stage 1)
| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | /api/auth/register | public | Create account, sends verification email |
| POST | /api/auth/login | public | Returns access + refresh JWT |
| GET | /api/auth/verify-email?token= | public | Confirms email |
| POST | /api/auth/resend-verification?email= | public | Re-sends verification link |
| POST | /api/auth/forgot-password | public | Sends reset link |
| POST | /api/auth/reset-password | public | Sets new password from token |
| GET  | /oauth2/authorization/google | public | Kicks off Google OAuth |
| GET  | /oauth2/authorization/github | public | Kicks off GitHub OAuth |
| GET  | /api/users/me | JWT | Get current profile |
| PUT  | /api/users/me | JWT | Update profile |

## Stage 2 (Core Features) — added

| Method | Path | Purpose |
|---|---|---|
| POST | /api/workspaces | Create a workspace (creator becomes OWNER) |
| GET | /api/workspaces | List workspaces you belong to |
| GET | /api/workspaces/{id} | Get one workspace |
| POST | /api/workspaces/{id}/members | Invite an existing user by email (ADMIN+ only) |
| GET | /api/workspaces/{id}/members | List members |
| POST | /api/workspaces/{id}/projects | Create a project |
| GET | /api/workspaces/{id}/projects | List projects in a workspace |
| GET/PUT/DELETE | /api/projects/{id} | Get, update, or delete a project |
| POST | /api/projects/{id}/tasks | Create a task |
| GET | /api/projects/{id}/tasks | List tasks in a project |
| GET/PUT/DELETE | /api/tasks/{id} | Get, update, or delete a task |
| GET | /api/tasks/search | Filter tasks by status/priority/assignee/keyword/due date |
| POST | /api/tasks/{id}/attachments | Upload a file to a task (multipart, 10MB max) |
| GET | /api/tasks/{id}/attachments | List a task's attachments |
| GET | /api/files/{id}/download | Download a file |
| GET | /api/notifications | List your notifications (`?unreadOnly=true` to filter) |
| PUT | /api/notifications/{id}/read | Mark one as read |
| PUT | /api/notifications/read-all | Mark all as read |
| GET | /api/workspaces/{id}/activity | Audit trail for a workspace |
| GET | /api/workspaces/{id}/analytics | Task/completion stats for a workspace |

**Access control:** every workspace-scoped endpoint checks the caller is
a member (`WorkspaceService.assertMember`); inviting a member requires
ADMIN or OWNER role. Task/project/file access is checked transitively
through the workspace the resource belongs to.

**Notifications fire automatically** on task assignment and status
change — no manual trigger needed, just create/update a task with an
`assigneeId` set.

**File storage** is local disk (`backend/uploads/` by default, configurable
via `UPLOAD_DIR`) for Stage 2. `FileStorageService` is the only place
that talks to the filesystem, so swapping to S3/Cloud Storage in a later
stage is a one-file change.

Add `backend/uploads/` to your `.gitignore` — uploaded files shouldn't
be committed.

## Stage 3 (Production Ready) — added

**Run everything with Docker (recommended way to try Stage 3):**
```bash
docker compose up --build
```
This starts MySQL, Redis, the backend, and the frontend together —
backend on :8080, frontend on :5173. First run takes a few minutes
(image builds); after that it's fast.

**Or keep running locally without Docker** (`mvn spring-boot:run` +
`npm run dev`) — nothing about local dev changed. Cache defaults to
in-memory (no Redis needed) unless you explicitly set `CACHE_TYPE=redis`.

**Redis Cache** — workspace lookups, member lists, and analytics are
cached (`CacheConfig`), with eviction wired into every write path so
cached data never goes stale. TTLs: workspaces 10min, members 5min,
analytics 2min.

**Swagger** — visit `/swagger-ui.html`, click **Authorize**, paste an
`accessToken` from `/api/auth/login`, and every protected endpoint
becomes testable from the browser.

**JUnit** — run tests with:
```bash
cd backend
mvn test
```
Covers: JWT generation/validation/expiry (`JwtServiceTest`),
registration/login business rules (`AuthServiceTest`), workspace
access-control rules (`WorkspaceServiceTest`), and a full
register→verify→login→protected-route integration test
(`AuthFlowIntegrationTest`) against a real Spring context + H2.

**Logging** — structured logs to console + `backend/logs/vigilai.log`
(rotated daily, 14-day retention). Every request logs method/path/
status/duration. Set `VIGILAI_LOG_LEVEL=DEBUG` for more detail locally.

**Email Service** — now `@Async`; a slow/misconfigured SMTP server
never blocks a request.

**Cloud Deployment** — two new Spring profiles:
- `docker` (`application-docker.yml`) — used automatically by
  docker-compose, points at real MySQL/Redis by service name.
- `prod` (`application-prod.yml`) — for platforms like Render/Railway
  that inject `DB_URL`/`REDIS_HOST` directly without docker-compose.
  Uses `ddl-auto: validate` instead of `update` — for a real
  production deploy you'd want Flyway/Liquibase migrations instead of
  Hibernate auto-DDL; that's a good Stage 5 addition, not done here.

## What's deliberately not here yet
Kafka, Kubernetes, CI/CD pipelines, and multi-service split are Stage 5.
Database migrations (Flyway/Liquibase) would be a natural next step
before a real production deploy — Stage 3 uses Hibernate's `ddl-auto`
for simplicity, which is fine for a portfolio project but not what
you'd want managing a real production schema long-term.

## Note on verifying this build
This was written and reviewed without a live Maven/npm environment (no
network access in this session), so run `mvn clean compile` and
`npm run build` locally before you push — flag anything that doesn't
compile and it can be fixed directly.

## Stage 4 (AI Integration) — Proof of Execution added

**Proof of Execution** is the core AI feature: submit a photo as proof
a task was actually completed, and Google Gemini Vision verifies it —
not just a checkbox tap, an actual judgment call on whether the photo
plausibly shows the task being done.

| Method | Path | Purpose |
|---|---|---|
| POST | /api/tasks/{taskId}/proof | Submit a photo; AI verifies it and auto-completes the task if accepted |

**How it works:** the photo is sent to Gemini in memory, verified, and
never written to disk — only the verdict (`verified: true/false` + a
one-sentence reason) is stored in the `proof_submission` table. This
matches the original design goal of the feature: the photo itself is
never retained, only proof that a check happened.

**Setup required:** get a free API key from
[Google AI Studio](https://aistudio.google.com/apikey), then set it as
an environment variable before starting the backend (never commit it
or hardcode it):
```bash
$env:GEMINI_API_KEY="your-key-here"   # PowerShell
mvn spring-boot:run
```
Without a key set, proof submissions are safely auto-rejected with a
clear message rather than the app crashing.

**Verified working:** correctly rejects stock/reused photos ("this
appears to be a stock photo rather than an original photo") and
accepts genuine photos, auto-marking the task `DONE` on acceptance.

**Not yet built:** AI Assistant, Smart Suggestions, Resume Analyzer,
Learning Roadmap, and AI Project Documentation — the remaining Stage 4
features, planned as a smaller follow-on addition.