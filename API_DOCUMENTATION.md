# API Documentation

Base URL (local): `http://localhost:8080/api`
Base URL (production): `https://aicodereview-backend-adu2.onrender.com/api`

All endpoints except `/auth/**` require a JWT bearer token:
```
Authorization: Bearer <token>
```
Obtain a token via `POST /auth/login` or `POST /auth/register`.

All request/response bodies are JSON unless otherwise noted.

---

## 1. Authentication — `/auth`

### `POST /auth/register`
Create a new account. The first user ever registered is automatically
promoted to `ROLE_ADMIN`.

**Request:**
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "password": "at-least-6-characters"
}
```
**Response `200`:**
```json
{
  "token": "eyJhbGciOi...",
  "userId": 1,
  "name": "Jane Doe",
  "email": "jane@example.com",
  "role": "ROLE_USER"
}
```

### `POST /auth/login`
**Request:**
```json
{ "email": "jane@example.com", "password": "at-least-6-characters" }
```
**Response `200`:** same shape as register.

### `POST /auth/logout`
Stateless JWT — this is a no-op; the client simply discards the token.
**Response `200`:** `{ "message": "Logged out. Discard the token on the client." }`

### `POST /auth/reset-password`
**Request:**
```json
{ "email": "jane@example.com", "newPassword": "new-password" }
```
**Response `200`:** `{ "message": "Password reset successfully" }`

### `GET /auth/me`
Returns the current authenticated user.
**Response `200`:** `{ "userId": 1, "name": "...", "email": "...", "role": "..." }`

### `PUT /auth/profile`
Update name, email-notification preference, and/or password.
**Request:**
```json
{
  "name": "Jane D.",
  "currentPassword": "required only if changing password",
  "newPassword": "optional"
}
```
**Response `200`:** `{ "message": "Profile updated successfully" }`

---

## 2. Projects & Code Submission — `/projects`

### `POST /projects/upload`
Upload a file or `.zip` for review. `multipart/form-data`.

| Field | Type | Required |
|---|---|---|
| `file` | file | Yes |
| `teamId` | number | No — submits under a team workspace if provided |

**Response `200`:** a full `ReviewResponseDTO` (see §4).

### `POST /projects/snippet`
Submit a pasted code snippet.
**Request:**
```json
{
  "code": "public class Main { ... }",
  "fileName": "Main.java",
  "projectName": "optional label",
  "teamId": null
}
```
**Response `200`:** a full `ReviewResponseDTO`.

### `GET /projects?search=`
List the current user's projects, optionally filtered by name.
**Response `200`:**
```json
[
  {
    "projectId": 1,
    "projectName": "Main.java",
    "uploadType": "SNIPPET",
    "createdAt": "2026-07-16T04:15:28",
    "latestReviewId": 14,
    "latestScore": 40
  }
]
```

### `GET /projects/team/{teamId}?search=`
List projects submitted under a given team.

### `GET /projects/{projectId}/files`
List the source file names inside a project (used by the AI Refactor panel).

### `POST /projects/{projectId}/refactor?fileName=`
Request an AI-rewritten version of one file.
**Response `200`:**
```json
{ "fileName": "Main.java", "available": true, "refactoredCode": "...", "error": "" }
```

### `DELETE /projects/{projectId}`
Delete a project and all its reviews. Owner-only.

---

## 3. Reviews — `/reviews`

### `GET /reviews/{reviewId}`
Fetch full details for one review (see `ReviewResponseDTO` shape in §4).

### `GET /reviews/project/{projectId}`
List all reviews ever run for a given project.

### `GET /reviews/{reviewId}/export/pdf`
Downloads a PDF report. `Content-Type: application/pdf`.

### `GET /reviews/{reviewId}/export/html`
Returns a self-contained HTML report.

### `GET /reviews/{reviewId}/export/markdown`
Downloads a Markdown report.

### `GET /reviews/project/{projectId}/documentation`
Auto-generated class/method documentation (Markdown).

### `GET /reviews/project/{projectId}/api-docs`
Auto-generated API doc table of public methods (Markdown).

### `GET /reviews/project/{projectId}/readme-summary`
Auto-generated README summary based on the latest review's metrics (Markdown).

---

## 4. `ReviewResponseDTO` shape

Returned by upload/snippet submission and `GET /reviews/{id}`:
```json
{
  "reviewId": 14,
  "projectId": 9,
  "projectName": "Main.java",
  "reviewScore": 40,
  "summary": "Static analysis found 5 findings...",
  "numClasses": 1,
  "numMethods": 1,
  "linesOfCode": 10,
  "avgMethodLength": 8.0,
  "cyclomaticComplexity": 2.0,
  "maintainabilityIndex": 78.5,
  "createdAt": "2026-07-16T04:15:28",
  "findings": [
    {
      "severity": "CRITICAL",
      "category": "SECURITY",
      "source": "AI",
      "issue": "Possible SQL injection",
      "explanation": "...",
      "suggestion": "...",
      "fileName": "Main.java",
      "lineNumber": 12
    }
  ]
}
```

---

## 5. Teams — `/teams`

### `POST /teams`
Create a team (caller becomes `OWNER`).
**Request:** `{ "name": "My Team" }`

### `GET /teams`
List teams the current user belongs to, with role and member count.

### `POST /teams/{teamId}/members`
Add a member by email. Owner-only.
**Request:** `{ "email": "teammate@example.com" }`

### `DELETE /teams/{teamId}/members/{userId}`
Remove a member. Owner-only.

### `GET /teams/{teamId}/members`
List all members of a team.

---

## 6. Analytics — `/analytics`

### `GET /analytics/overview`
Aggregate stats across all of the current user's projects.
```json
{
  "totalProjects": 5,
  "totalReviews": 8,
  "totalFindings": 42,
  "averageScore": 63.5,
  "scoreTrend": [{ "date": "Jul 16", "score": 40 }],
  "severityBreakdown": { "CRITICAL": 2, "HIGH": 5 },
  "categoryBreakdown": { "SECURITY": 3, "BUG": 6 },
  "sourceBreakdown": { "AI": 10, "PMD": 8 },
  "topIssues": [{ "issue": "Possible SQL injection", "count": 3 }]
}
```

---

## 7. Admin — `/admin`

All endpoints require `ROLE_ADMIN`.

| Method | Path | Description |
|---|---|---|
| GET | `/admin/users` | List all users, with project counts |
| DELETE | `/admin/users/{userId}` | Delete a user and all their projects/reviews |
| GET | `/admin/projects` | List all projects platform-wide |
| DELETE | `/admin/projects/{projectId}` | Delete any project |
| GET | `/admin/stats` | Platform-wide stats (total users, projects, reviews, average score, severity breakdown) |

---

## 8. Error Format

All errors follow a consistent shape:
```json
{
  "timestamp": "2026-07-16T04:15:28",
  "status": 404,
  "message": "Project not found"
}
```
Validation errors (`400`) additionally include a field-level `errors` map:
```json
{
  "timestamp": "...",
  "status": 400,
  "errors": { "password": "Password must be at least 6 characters" }
}
```

---

## 9. Static Analysis Sources

Findings are tagged with a `source` field indicating which engine produced them:

| Source | Engine | Notes |
|---|---|---|
| `CHECKSTYLE` | Real Checkstyle (Sun conventions) | Java only |
| `PMD` | Real PMD (best practices, error-prone, design, performance, security rulesets) | Java only |
| `SPOTBUGS` | Custom JavaParser-based bug-pattern detector | Java only; a stand-in for true SpotBugs, which requires compiled bytecode |
| `AI` | LLM-generated review (Groq / OpenAI-compatible) | All supported languages (Java, Python, JavaScript) |
