# Cyber Incident Reporting System (CIRS)

A secure, enterprise-grade **Cyber Incident Reporting System** built using **Java 17/18, Spring Boot 3.3.4, Spring Security 6, Spring Data JPA / Hibernate, MySQL, and a modern responsive dark cybersecurity-themed frontend**.

CIRS allows citizens and organizations to report cyber security attacks (phishing, ransomware, data breaches, financial fraud, malware), upload supporting digital evidence with Apache Tika magic-byte validation and SHA-256 integrity hashing, track case investigation status in real time, and securely collaborate with forensic investigators and security administrators.

---

## 🛡️ Core Highlights & Features

- **Strict Role-Based Access Control (RBAC)**: Segregated interfaces and authorization for `USER` (Citizen), `INVESTIGATOR`, and `ADMIN`.
- **Stateless JWT Authentication**: HMAC-SHA signed JSON Web Tokens with standard Bearer authorization.
- **Insecure Direct Object Reference (IDOR) Protection**: Server-side verification ensures citizens only access their own reports, and investigators can only access assigned cases.
- **Evidence Integrity & Anti-Tampering**:
  - Apache Tika deep inspection of file content magic numbers/signatures (rejects malicious executables masquerading with fake extensions).
  - Cryptographic **SHA-256 hash** calculated on stream ingestion to preserve evidentiary chain of custody.
  - Randomized stored filenames (`UUIDv4`) outside web-accessible roots to prevent directory traversal and execution vulnerabilities.
- **Controlled Incident Workflow (Finite State Machine)**: Strictly enforced sequential state transitions (`SUBMITTED` &rarr; `UNDER_REVIEW` &rarr; `ASSIGNED` &rarr; `INVESTIGATING` &rarr; `NEED_MORE_INFORMATION` &rarr; `RESOLVED` &rarr; `CLOSED`).
- **Information Segregation**:
  - `INTERNAL_NOTE`: Confidential forensic notes visible only to Investigators and Administrators.
  - `PUBLIC_UPDATE` / `INFO_REQUEST`: Status changes and feedback visible to reporting citizens.
- **Security Audit Logging**: Immutable database audit trail capturing all logins, submissions, investigator assignments, status changes, and evidence downloads.
- **Unique Incident Identifiers**: Format `CIR-YYYYMMDD-XXXXX` (e.g. `CIR-20260928-8D5YE`), unique and non-enumerable.
- **Embedded Responsive Cyber UI**: Dark-themed cybersecurity interface with telemetry dashboards, evidence dropzones, live search, and modal workflows.

---

## 🛠️ Technology Stack

| Layer | Technology |
| :--- | :--- |
| **Backend Framework** | Java 17/18, Spring Boot 3.3.4 |
| **Security & Auth** | Spring Security 6.3.3, JJWT 0.12.6, BCrypt (strength 12) |
| **Data & ORM** | Spring Data JPA, Hibernate 6.5.3, MySQL Connector/J |
| **Database** | MySQL 8.0 / MariaDB (production/dev), H2 in-memory (testing) |
| **Validation & Forensics** | Hibernate Validator, Apache Tika 2.9.2 (magic number detection) |
| **Build & Tooling** | Apache Maven 3.9.11 |
| **Frontend** | HTML5, CSS3 Custom Properties (Dark Cyber UI), Vanilla JavaScript (ES6+ Fetch) |

---

## 🗄️ Database Setup & Configuration

### 1. MySQL Prerequisites
Make sure MySQL is running on `localhost:3306`. If using XAMPP, start MySQL from the XAMPP Control Panel.

Create the database in MySQL:
```sql
CREATE DATABASE IF NOT EXISTS cyber_incident_db 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;
```

### 2. Environment Variables & Configuration
Database settings can be configured via environment variables or in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/cyber_incident_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true}
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
```

An example `.env.example` file is provided in the root directory.

---

## 🚀 How to Run the Application

### 1. Build and Run via Maven
From the project root directory, run:
```powershell
# Windows
.\mvn.cmd spring-boot:run

# Linux / Mac
./mvn spring-boot:run
```

Once started, open your browser and navigate to:
```
http://localhost:8080
```

---

## 👥 Default Development & Seed Accounts

The system automatically seeds initial threat categories and development accounts upon first launch:

| Role | Username | Password | Email | Full Name |
| :--- | :--- | :--- | :--- | :--- |
| **ADMIN** | `admin` | Set via `CIRS_ADMIN_PASSWORD` | `admin@cirs.gov` | Chief Information Security Officer |
| **INVESTIGATOR** | `investigator1` | Set via `CIRS_INVESTIGATOR_PASSWORD` | `lead.investigator@cirs.gov` | Sarah Connor |
| **USER (Citizen)** | `user1` | Set via `CIRS_USER_PASSWORD` | `alice.smith@example.com` | Alice Smith |

*Note: You can also register a new Citizen account anytime on `/register.html`, or click the quick-fill buttons on `/login.html`.*

---

## 🔄 Incident Lifecycle & Workflow

The incident state machine enforces the following transitions:

```
[Citizen Submits Report]
          │
          ▼
      SUBMITTED
          │
          ├── (Admin intake) ──► UNDER_REVIEW
          │                           │
          ▼                           ▼
       ASSIGNED (Assigned to Investigator)
          │
          ▼
     INVESTIGATING (Forensic analysis)
          │
          ├──► NEED_MORE_INFORMATION (Investigator queries Citizen)
          │           │
          │           └── (Citizen responds) ──► INVESTIGATING
          │
          ▼
      RESOLVED (Remediation completed)
          │
          ▼
       CLOSED (Administrative case archive)
```

### Transition Authority Matrix
- **`SUBMITTED` &rarr; `UNDER_REVIEW`**: Admin
- **`UNDER_REVIEW` / `SUBMITTED` &rarr; `ASSIGNED`**: Admin (designating an investigator)
- **`ASSIGNED` &rarr; `INVESTIGATING`**: Assigned Investigator or Admin
- **`INVESTIGATING` &rarr; `NEED_MORE_INFORMATION`**: Assigned Investigator
- **`NEED_MORE_INFORMATION` &rarr; `INVESTIGATING`**: Reporting Citizen (via reply form)
- **`INVESTIGATING` &rarr; `RESOLVED`**: Assigned Investigator or Admin
- **`RESOLVED` &rarr; `CLOSED`**: Admin

---

## 📡 REST API Overview

### 1. Authentication & Profile (`/api/v1/auth`)
- `POST /register`: Register a citizen account (`username`, `email`, `password`, `fullName`)
- `POST /login`: Authenticate and receive signed Bearer JWT token
- `GET /me`: Fetch authenticated user profile and permissions

### 2. Incident Management (`/api/v1/incidents`)
- `POST /`: Submit new incident report (supports multipart `multipart/form-data` with files)
- `GET /my`: Get current logged-in citizen's submitted reports (paginated)
- `GET /assigned`: Get current investigator's assigned cases (paginated)
- `GET /{incidentNumber}`: View incident file, evidence, and activity stream (enforces IDOR ownership)
- `PATCH /{incidentNumber}/status`: Transition workflow status with rationale
- `POST /{incidentNumber}/notes`: Append internal SOC note, public update, or information request
- `POST /{incidentNumber}/respond`: Citizen submits answers & extra evidence to resume investigation
- `GET /{incidentNumber}/attachments/{id}`: Secure stream download of evidence file

### 3. Category Management (`/api/v1/categories`)
- `GET /`: List all active threat categories (Public)
- `GET /all`: List all categories including inactive ones (Admin)
- `POST /`: Create new threat classification (Admin)
- `PUT /{id}`: Update category details (Admin)
- `PATCH /{id}/toggle`: Activate/deactivate category (Admin)

### 4. Admin Center (`/api/v1/admin`)
- `GET /incidents`: Global incident registry with search and category/status filtering
- `PUT /incidents/{incidentNumber}/assign`: Officially assign an incident to an investigator
- `GET /dashboard/stats`: Aggregated telemetry (status counts, category distribution, recent activity)
- `GET /users`: Paginated directory of all registered users and investigators
- `GET /investigators`: List active investigators available for assignment
- `POST /investigators`: Provision a new investigator account
- `PATCH /users/{id}/toggle-status`: Enable or disable an account
- `GET /audit-logs`: Paginated view of system security audit events

---

## 🔒 Security Architecture Details

1. **Password Hashing**: Uses `BCryptPasswordEncoder` configured with work factor 12. Plaintext passwords are never persisted or exposed in API responses.
2. **Horizontal Privilege Enforcement (IDOR Protection)**:
   - Methods annotated with `@PreAuthorize` combine standard Spring Security roles with service-level entity evaluation.
   - User A attempting to view or download evidence from User B's incident receives `403 Forbidden`.
3. **Magic Byte Evidence Inspection**:
   - `FileValidator` utilizes `org.apache.tika.Tika` to examine raw binary file headers. An executable renamed from `.exe` to `.pdf` or `.png` is detected and blocked with `400 Bad Request`.
4. **Filename Sanitization & Storage Isolation**:
   - Original filenames are stripped of path traversal sequences (`../`, `..\\`) and stored in MySQL.
   - Physical disk storage writes files using `UUID.randomUUID().toString() + "_" + sanitizedName` inside an isolated directory configured outside public static folders.
5. **Immutable Security Audit Log**:
   - High-value events (`LOGIN_SUCCESS`, `LOGIN_FAILED`, `SUBMIT_INCIDENT`, `STATUS_CHANGE`, `ASSIGN_INVESTIGATOR`, `UPLOAD_EVIDENCE`, `DOWNLOAD_EVIDENCE`, `CREATE_INVESTIGATOR`, `TOGGLE_USER_STATUS`) are recorded with user agent, client IP, target entity ID, and timestamp.

---

## 🧪 Testing & Verification

The test suite runs with an isolated H2 in-memory profile (`application-test.yml`), testing unit logic, security constraints, and full integration workflows.

To execute all tests:
```powershell
.\mvn.cmd test
```

### Test Suite Summary:
- **`IncidentWorkflowIntegrationTest`**: End-to-end integration test validating report submission with evidence, IDOR prevention, admin assignment, investigator workflow, confidential internal notes segregation, citizen response, resolution, and closure.
- **`IncidentStatusTransitionTest`**: Unit testing of workflow state machine valid vs prohibited status skips.
- **`FileValidatorTest`**: Tests Apache Tika magic-byte detection, path traversal sanitization, and file extension filtering.
- **`AuthControllerTest`**: Tests registration validation, token issuance, bad credentials handling (401), and unauthenticated access prevention.
- **`CategoryControllerTest`**: Tests public category listing and admin category creation.

**All 13 tests pass with 0 failures and 0 errors.**

---

## 📂 Project Directory Structure

```
cyber-incident-reporting-system/
├── pom.xml
├── README.md
├── .env.example
├── mvn.cmd
└── src/
    ├── main/
    │   ├── java/com/cirs/
    │   │   ├── CyberIncidentReportingApplication.java
    │   │   ├── config/
    │   │   │   ├── AuditingConfig.java
    │   │   │   ├── DataInitializer.java
    │   │   │   └── SecurityConfig.java
    │   │   ├── controller/
    │   │   │   ├── AdminController.java
    │   │   │   ├── AuthController.java
    │   │   │   ├── CategoryController.java
    │   │   │   └── IncidentController.java
    │   │   ├── dto/
    │   │   │   ├── request/
    │   │   │   │   ├── AssignInvestigatorRequest.java
    │   │   │   │   ├── CategoryRequest.java
    │   │   │   │   ├── IncidentCreateRequest.java
    │   │   │   │   ├── InvestigatorCreateRequest.java
    │   │   │   │   ├── LoginRequest.java
    │   │   │   │   ├── NoteCreateRequest.java
    │   │   │   │   ├── RegisterRequest.java
    │   │   │   │   └── StatusUpdateRequest.java
    │   │   │   └── response/
    │   │   │       ├── ApiErrorResponse.java
    │   │   │       ├── ApiResponse.java
    │   │   │       ├── AuditLogResponse.java
    │   │   │       ├── AuthResponse.java
    │   │   │       ├── CategoryResponse.java
    │   │   │       ├── DashboardStatsResponse.java
    │   │   │       ├── IncidentAttachmentResponse.java
    │   │   │       ├── IncidentNoteResponse.java
    │   │   │       ├── IncidentResponse.java
    │   │   │       ├── IncidentSummaryResponse.java
    │   │   │       └── UserProfileResponse.java
    │   │   ├── entity/
    │   │   │   ├── AuditLog.java
    │   │   │   ├── BaseEntity.java
    │   │   │   ├── Incident.java
    │   │   │   ├── IncidentAttachment.java
    │   │   │   ├── IncidentCategory.java
    │   │   │   ├── IncidentNote.java
    │   │   │   └── User.java
    │   │   ├── enums/
    │   │   │   ├── IncidentStatus.java
    │   │   │   ├── NoteType.java
    │   │   │   ├── Role.java
    │   │   │   └── Severity.java
    │   │   ├── exception/
    │   │   │   ├── BadRequestException.java
    │   │   │   ├── FileStorageException.java
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   ├── InvalidStatusTransitionException.java
    │   │   │   ├── ResourceNotFoundException.java
    │   │   │   └── UnauthorizedAccessException.java
    │   │   ├── repository/
    │   │   │   ├── AuditLogRepository.java
    │   │   │   ├── IncidentAttachmentRepository.java
    │   │   │   ├── IncidentCategoryRepository.java
    │   │   │   ├── IncidentNoteRepository.java
    │   │   │   ├── IncidentRepository.java
    │   │   │   └── UserRepository.java
    │   │   ├── security/
    │   │   │   ├── CustomAccessDeniedHandler.java
    │   │   │   ├── CustomUserDetails.java
    │   │   │   ├── CustomUserDetailsService.java
    │   │   │   ├── IncidentSecurityEvaluator.java
    │   │   │   ├── JwtAuthenticationEntryPoint.java
    │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   └── JwtTokenProvider.java
    │   │   ├── service/
    │   │   │   ├── AdminService.java
    │   │   │   ├── AuthService.java
    │   │   │   ├── AuditService.java
    │   │   │   ├── CategoryService.java
    │   │   │   ├── FileStorageService.java
    │   │   │   ├── IncidentService.java
    │   │   │   └── impl/
    │   │   │       ├── AdminServiceImpl.java
    │   │   │       ├── AuthServiceImpl.java
    │   │   │       ├── AuditServiceImpl.java
    │   │   │       ├── CategoryServiceImpl.java
    │   │   │       ├── FileStorageServiceImpl.java
    │   │   │       └── IncidentServiceImpl.java
    │   │   └── util/
    │   │       ├── FileValidator.java
    │   │       ├── HashUtils.java
    │   │       └── IncidentNumberGenerator.java
    │   └── resources/
    │       ├── application.yml
    │       └── static/
    │           ├── css/
    │           │   └── style.css
    │           ├── js/
    │           │   └── api.js
    │           ├── index.html
    │           ├── login.html
    │           ├── register.html
    │           ├── user/
    │           │   ├── dashboard.html
    │           │   ├── incident-details.html
    │           │   └── report-incident.html
    │           ├── investigator/
    │           │   ├── dashboard.html
    │           │   └── incident-workspace.html
    │           └── admin/
    │               ├── audit-logs.html
    │               ├── categories.html
    │               ├── dashboard.html
    │               ├── incidents.html
    │               └── users.html
    └── test/
        ├── java/com/cirs/
        │   ├── AuthControllerTest.java
        │   ├── CategoryControllerTest.java
        │   ├── FileValidatorTest.java
        │   ├── IncidentStatusTransitionTest.java
        │   └── IncidentWorkflowIntegrationTest.java
        └── resources/
            └── application-test.yml
```
