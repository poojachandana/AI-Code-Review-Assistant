# AI Code Review Assistant

A full-stack, AI-powered code review tool built with **Spring Boot** (backend) and
**React + Tailwind CSS** (frontend). Users upload Java/Python/JavaScript files, a
`.zip` project, or paste a code snippet, and the system runs it through a
multi-stage pipeline — static analysis, complexity metrics, and an AI-generated
review — then displays a quality score, detailed findings, and exportable
reports.

**Live demo:** https://aicodereview-frontend-f6ug.onrender.com
**Backend API:** https://aicodereview-backend-adu2.onrender.com

---

## 1. Features

### Core Features
- **User Authentication** — Register, Login, Logout, Reset Password, Update Profile
- **Code Submission** — Upload Java/Python/JS files, a `.zip` project, or paste a snippet via a Monaco code editor
- **Static Code Analysis** — Checkstyle (coding standards), PMD (code smells/best practices), and a bug-pattern detector (SpotBugs-style checks on source, since true SpotBugs requires compiled bytecode)
- **AI-Powered Review** — bug reports, security findings, performance suggestions, refactoring recommendations, and naming improvements, generated via an LLM
- **Complexity Analysis** — number of classes, methods, cyclomatic complexity, lines of code, average method length, maintainability index
- **Documentation Generator** — auto-generated class/method docs, an API doc table, and a README summary
- **Review Dashboard** — view, search, filter, and delete past reviews; view detailed reports
- **Export Reports** — PDF, HTML, and Markdown

### Bonus Features
- Multi-language support (Java, Python, JavaScript)
- Monaco Code Editor (snippet input + AI-refactored code viewer)
- AI-powered code refactoring (rewrite a file per AI best practices)
- Code Quality Score (0–100)
- Repository Analytics Dashboard (score trend, findings by category/source, top recurring issues)
- Dark/Light theme
- Docker support (per-service Dockerfiles + a Render Blueprint for one-command cloud deployment)
- Team Workspaces (create teams, invite members, shared project visibility)
- Admin Dashboard (platform-wide user/project management; the first registered user is auto-promoted to admin)

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| Backend language/framework | Java 17, Spring Boot 3.2.5 |
| Security | Spring Security + JWT (stateless, session-only via `sessionStorage` on the frontend — closing the browser requires logging in again) |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL (production) / H2 (local zero-setup default) |
| Static analysis | Checkstyle 10.17.0, PMD 7.1.0 |
| AST parsing | JavaParser 3.25.10 |
| PDF generation | OpenPDF |
| AI provider | Groq (Llama 3.3 70B) via an OpenAI-compatible `chat/completions` endpoint — swappable to OpenAI, Gemini, or OpenRouter via config only |
| Frontend framework | React 18 + Vite |
| Styling | Tailwind CSS |
| Routing | React Router v6 |
| Charts | Recharts |
| Code editor | Monaco Editor |
| Icons | Lucide React |
| Deployment | Render (Blueprint-managed: Postgres + Docker web service + static site) |

---

## 3. Project Structure

```
AI-Code-Review-Assistant/
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/aicode/review/
│       │   ├── AiCodeReviewApplication.java
│       │   ├── config/          # Security + CORS config
│       │   ├── controller/      # Auth, Project, Review, Team, Admin, Analytics
│       │   ├── dto/             # Request/response DTOs
│       │   ├── entity/          # User, Project, Review, ReviewFinding, Team, TeamMember
│       │   ├── exception/       # Global exception handling
│       │   ├── repository/      # Spring Data JPA repositories
│       │   ├── security/        # JWT util + filter + UserDetailsService
│       │   └── service/
│       │       ├── analysis/    # CheckstyleAnalyzer, PmdAnalyzer, BugPatternAnalyzer
│       │       ├── AuthService.java
│       │       ├── FileProcessingService.java
│       │       ├── StaticAnalysisService.java
│       │       ├── ComplexityAnalysisService.java
│       │       ├── AIReviewService.java
│       │       ├── DocumentationGeneratorService.java
│       │       ├── PdfReportService.java
│       │       └── ReviewOrchestrationService.java
│       └── resources/
│           ├── application.properties
│           └── application-postgres.properties
├── frontend/
│   ├── package.json, vite.config.js, tailwind.config.js, Dockerfile
│   └── src/
│       ├── pages/       # Login, Register, ResetPassword, Dashboard, UploadCode,
│       │                # ReviewDetail, Profile, Analytics, Teams, AdminDashboard
│       ├── components/  # Navbar, ReviewCard, FindingItem, ComplexityChart,
│       │                # RefactorPanel, ScoreGauge, PasswordInput
│       ├── context/     # AuthContext (JWT + theme)
│       ├── utils/       # formatDate.js
│       └── services/api.js
├── docs/
│   ├── DATABASE_SCHEMA.md
│   ├── API_DOCUMENTATION.md
│   └── screenshots/
├── render.yaml           # Render Blueprint — provisions DB + backend + frontend
├── docker-compose.yml    # Local multi-container setup
└── README.md
```

---

## 4. Running Locally

### Quick start (zero setup, uses embedded H2)

```bash
# Terminal 1 — backend
cd backend
mvn spring-boot:run
# Runs on http://localhost:8080

# Terminal 2 — frontend
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173
```

To enable the AI review/refactor stage, set a Groq API key (free at
https://console.groq.com/keys) before starting the backend:
```bash
export GROQ_API_KEY=gsk_...your-key...
```

### Running with PostgreSQL instead of H2

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```
Edit `application-postgres.properties` with your local Postgres credentials first.

### Running everything with Docker Compose

```bash
docker compose up --build
```

---

## 5. Deployment

This project deploys to **Render** using a single Blueprint file (`render.yaml`)
that provisions the Postgres database, the Spring Boot backend (Docker), and
the React static frontend in one step, with the database credentials wired
automatically (no manual copy-pasting).

```bash
# 1. Push render.yaml to your repo
git push

# 2. Render Dashboard → New → Blueprint → connect your repo
# 3. Fill in the prompted secrets (GROQ_API_KEY, MAIL_USERNAME/PASSWORD if used, CORS origin)
# 4. Deploy Blueprint
```

See `render.yaml` at the repo root for the full resource definitions.

---

## 6. Documentation

- **[Database Schema](docs/DATABASE_SCHEMA.md)** — full entity-relationship reference
- **[API Documentation](docs/API_DOCUMENTATION.md)** — every endpoint, request/response shape, and auth requirement

---

## 7. Sample Test Case

**Test input:** a Java snippet submitted via the Monaco editor, intentionally
containing common bugs to demonstrate detection:

```java
public class Main {
    public static void main(String[] args) {
        int x = 10;
        if (x = 20) {                       // assignment instead of comparison
            System.out.println("x is 20");
        }
        String name = null;
        System.out.println(name.length());  // null pointer dereference
        int result = divide(10, 0);          // divide by zero
    }
}
```

### 1. Register a new account
![Register page](screenshots/01-register.png)

### 2. Login
![Login page](screenshots/02-login.png)

### 3. Submit code for review
![Submit code](screenshots/03-submit-code.png)

### 4. Static analysis + AI review results
![Review results](screenshots/04-review-results.png)

### 5. Review dashboard
![Dashboard](screenshots/05-dashboard.png)

*(Add your own screenshots to `docs/screenshots/` with these filenames, or update the paths above to match your files.)*

---

## 8. Known Limitations

- **SpotBugs** performs bytecode analysis, which isn't possible on raw uploaded
  source without a compile step. A JavaParser-based heuristic detector
  (`BugPatternAnalyzer`) substitutes common SpotBugs-style checks directly on
  source instead.
- **Free-tier hosting**: both the backend and frontend spin down after ~15
  minutes of inactivity on Render's free tier; the first request afterward can
  take 10–50 seconds to respond.
- **Email notifications** were removed — Render's free tier blocks outbound
  SMTP (port 587), so this feature could not be made reliable without a paid
  tier or a switch to an HTTP-based email API (e.g., Resend, SendGrid).

---

## 9. License

Add your license here (e.g., MIT).
