# Database Schema

The application uses **PostgreSQL** in production (Render-managed) and
**H2** (file-based) for zero-setup local development. Schema is managed by
Hibernate (`spring.jpa.hibernate.ddl-auto=update`) based on the JPA entities
below — tables are created/updated automatically on startup.

---

## Entity-Relationship Overview

```
users                teams                team_members
┌──────────────┐    ┌──────────────┐    ┌──────────────────┐
│ id (PK)      │    │ id (PK)      │    │ id (PK)          │
│ name         │    │ name         │    │ team_id (FK)     │──┐
│ email        │◄───┤ owner_id (FK)│    │ user_id (FK)     │──┼──┐
│ password     │    │ created_at   │    │ role             │  │  │
│ role         │    └──────────────┘    │ joined_at        │  │  │
│ email_notif. │                        └──────────────────┘  │  │
│ created_at   │                                               │  │
└──────┬───────┘                                               │  │
       │◄──────────────────────────────────────────────────────┘  │
       │◄─────────────────────────────────────────────────────────┘
       │
       │ 1
       │
       │ *
projects
┌──────────────────┐
│ id (PK)          │
│ user_id (FK)     │──► users.id
│ project_name     │
│ upload_type      │  (FILE | ZIP | SNIPPET)
│ storage_path     │
│ team_id (FK, nullable) │──► teams.id
│ created_at       │
└──────┬───────────┘
       │ 1
       │
       │ *
reviews
┌──────────────────────┐
│ id (PK)              │
│ project_id (FK)      │──► projects.id
│ review_score         │  (0-100)
│ summary               │  TEXT
│ num_classes           │
│ num_methods           │
│ lines_of_code         │
│ avg_method_length     │
│ cyclomatic_complexity │
│ maintainability_index │
│ created_at            │
└──────┬───────────────┘
       │ 1
       │
       │ *
review_findings
┌──────────────────┐
│ id (PK)          │
│ review_id (FK)   │──► reviews.id
│ severity         │  (CRITICAL | HIGH | MEDIUM | LOW | INFO)
│ category         │  (BUG | SECURITY | CODE_SMELL | PERFORMANCE | STYLE | MAINTAINABILITY)
│ source            │  (CHECKSTYLE | PMD | SPOTBUGS | AI)
│ issue             │  TEXT
│ explanation       │  TEXT
│ suggestion        │  TEXT
│ file_name         │
│ line_number       │
└──────────────────┘
```

---

## Table Definitions

### `users`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `name` | VARCHAR(100) | NOT NULL |
| `email` | VARCHAR(150) | NOT NULL, UNIQUE |
| `password` | VARCHAR | NOT NULL (BCrypt hash) |
| `role` | VARCHAR | NOT NULL, default `ROLE_USER` (or `ROLE_ADMIN`) |
| `email_notifications` | BOOLEAN | NOT NULL, default `true` |
| `created_at` | TIMESTAMP | NOT NULL, not updatable |

**Notes:** The first user ever registered is automatically promoted to
`ROLE_ADMIN`. Passwords are stored as BCrypt hashes, never in plaintext.

---

### `teams`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `name` | VARCHAR | NOT NULL |
| `owner_id` | BIGINT | NOT NULL — logical FK to `users.id` |
| `created_at` | TIMESTAMP | NOT NULL, not updatable |

---

### `team_members`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `team_id` | BIGINT | NOT NULL — FK to `teams.id` |
| `user_id` | BIGINT | NOT NULL — FK to `users.id` |
| `role` | VARCHAR(20) | NOT NULL, default `MEMBER` (or `OWNER`) |
| `joined_at` | TIMESTAMP | NOT NULL, not updatable |

**Relationship:** Many-to-many bridge table between `users` and `teams`.

---

### `projects`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `user_id` | BIGINT | NOT NULL — logical FK to `users.id` |
| `project_name` | VARCHAR | NOT NULL |
| `upload_type` | VARCHAR | NOT NULL (`FILE`, `ZIP`, or `SNIPPET`) |
| `storage_path` | VARCHAR | Path to the stored source on disk |
| `team_id` | BIGINT | Nullable — FK to `teams.id`, set only when submitted under a team workspace |
| `created_at` | TIMESTAMP | NOT NULL, not updatable |

---

### `reviews`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `project_id` | BIGINT | NOT NULL — logical FK to `projects.id` |
| `review_score` | INTEGER | 0–100 code quality score |
| `summary` | TEXT | AI-generated or fallback summary |
| `num_classes` | INTEGER | Complexity metric |
| `num_methods` | INTEGER | Complexity metric |
| `lines_of_code` | INTEGER | Complexity metric |
| `avg_method_length` | DOUBLE | Complexity metric |
| `cyclomatic_complexity` | DOUBLE | Complexity metric (average per method) |
| `maintainability_index` | DOUBLE | Complexity metric (0–100 heuristic) |
| `created_at` | TIMESTAMP | NOT NULL, not updatable |

---

### `review_findings`

| Column | Type | Constraints |
|---|---|---|
| `id` | BIGINT | PK, auto-increment |
| `review_id` | BIGINT | NOT NULL — logical FK to `reviews.id` |
| `severity` | VARCHAR(30) | `CRITICAL`, `HIGH`, `MEDIUM`, `LOW`, `INFO` |
| `category` | VARCHAR(50) | `BUG`, `SECURITY`, `CODE_SMELL`, `PERFORMANCE`, `STYLE`, `MAINTAINABILITY` |
| `source` | VARCHAR(40) | `CHECKSTYLE`, `PMD`, `SPOTBUGS`, `AI` |
| `issue` | TEXT | Short title of the finding |
| `explanation` | TEXT | Why it matters |
| `suggestion` | TEXT | Concrete fix/refactor |
| `file_name` | VARCHAR | Nullable |
| `line_number` | INTEGER | Nullable |

**Design note:** `summary`, `issue`, `explanation`, and `suggestion` are mapped
as `@Column(columnDefinition = "TEXT")` rather than `@Lob`. On PostgreSQL,
`@Lob` on a `String` maps to an OID-based Large Object, which cannot be
accessed outside an explicit transaction/auto-commit mode — using `TEXT`
avoids this entirely and behaves identically on H2 for local development.

---

## Relationships Summary

| Relationship | Type |
|---|---|
| `users` → `projects` | One-to-many (a user owns many projects) |
| `users` ↔ `teams` (via `team_members`) | Many-to-many |
| `teams` → `projects` | One-to-many (optional — a project may belong to a team) |
| `projects` → `reviews` | One-to-many (a project can be re-analyzed, producing multiple reviews over time) |
| `reviews` → `review_findings` | One-to-many (one review has many findings) |

All foreign keys are enforced at the application layer via repository lookups
rather than database-level `FOREIGN KEY` constraints, keeping the schema
simple and Hibernate-managed (`ddl-auto=update`).
