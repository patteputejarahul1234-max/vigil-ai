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

## What's deliberately not here yet
Redis, Docker, JUnit tests, refresh-token rotation endpoint, and rate
limiting are Stage 3. This is Stage 1 only — get auth solid first.

## Note on verifying this build
This was written and reviewed without a live Maven/npm environment (no
network access in this session), so run `mvn clean compile` and
`npm run build` locally before you push — flag anything that doesn't
compile and it can be fixed directly.
