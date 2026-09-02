# ⚡ BuffTURF Backend — High-Concurrency Sports Booking API Engine

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot 3" />
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL 8" />
  <img src="https://img.shields.io/badge/Redis-7.x-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis" />
  <img src="https://img.shields.io/badge/Docker-Multi--stage-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/Razorpay-Integrated-0C2340?style=for-the-badge&logo=razorpay&logoColor=white" alt="Razorpay" />
</p>

---

## 🌟 Overview

The **BuffTURF Backend** is a high-throughput, enterprise-grade RESTful service architected to power real-time sports arena reservations, atomic slot concurrency locks, secure payment verification, and automated gate admission.

Built with **Java 21** and **Spring Boot 3**, it leverages **Redis** for distributed atomic locks (preventing race conditions and double-bookings during peak match scheduling), **Spring Security** with stateless **JWT tokens** for role-based access control (Admin, Turf Owner, Athlete), **Razorpay** payment gateway integration with cryptographic signature verification, and **Google Gemini Flash** for conversational AI assistance.

---

## 🚀 Key Architectural Highlights

### 1. 🔒 Distributed Slot Locking Engine (Redis-Backed)
* Solves the classic double-booking problem in high-demand sports venues.
* When a user initiates checkout, an atomic Redis lock is acquired for the selected slot IDs with an automated Time-To-Live (TTL, default 10 minutes).
* If payment succeeds, the lock is committed to MySQL; if cancelled or timed out, the Redis key automatically expires or releases, returning the slots back to the public pool without orphaned states.

### 2. 🛡️ Role-Based Access Control (RBAC) & Security
* Three distinct roles: `ATHLETE / USER`, `TURF_OWNER`, and `ADMIN`.
* Stateless JWT authentication with HMAC-SHA256 signature verification.
* Comprehensive audit logging (`AuditLog` entity) tracking security actions and administrative changes.

### 3. 💳 Razorpay Payment & Verification Lifecycle
* Server-side order creation (`POST /api/bookings/create-order`).
* Secure cryptographic signature verification (`POST /api/bookings/verify-payment`) ensuring zero payment tampering.
* Automatic slot assignment and participant registration upon verified capture.

### 4. 🎟️ Digital QR Pass Generation & Validation
* Issues high-contrast digital match passes with embedded cryptographic tokens.
* Scannable via venue gate scanners (`/api/qr/verify-code/{code}` and `/api/qr/verify/{token}`).
* Supports status lifecycle: `PENDING` ➔ `CHECKED_IN` (with timestamp), preventing duplicate pass re-use.

### 5. 🤖 AI Chatbot & Recommendation Pipeline
* Integrates Google Gemini 2.5 Flash for natural language sports recommendations and query resolution.
* Connects to dedicated FastAPI ML service for collaborative filtering venue suggestions (`/api/recommend`).

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Runtime & Framework** | Java 21 LTS, Spring Boot 3.4.x |
| **Security & Auth** | Spring Security 6, JJWT (io.jsonwebtoken 0.12.x), BCrypt |
| **Database & Cache** | MySQL 8.0, Spring Data JPA, Hibernate, HikariCP, Redis 7 (Lettuce) |
| **Payment Gateway** | Razorpay Java SDK (`com.razorpay:razorpay-java:1.4.8`) |
| **Barcode & QR** | Google ZXing (`com.google.zxing:javase:3.5.3`) |
| **Email Service** | Spring Boot Starter Mail (JavaMailSender / Gmail SMTP) |
| **Testing & Quality** | JUnit 5, Mockito, JaCoCo Code Coverage |
| **Containerization** | Docker multi-stage build (Eclipse Temurin JDK 21 Alpine) |

---

## 📋 Core API Specifications

### Authentication (`/api/auth`)
* `POST /api/auth/register` — Register new athlete squad account
* `POST /api/auth/login` — Authenticate and receive JWT bearer token + role
* `POST /api/auth/forgot-password` — Initiate email OTP recovery
* `POST /api/auth/verify-otp` — Validate 6-digit OTP
* `POST /api/auth/reset-password` — Set new password with verified token

### Turfs & Venues (`/api/turfs`)
* `GET /api/turfs/search?location=&sportType=` — Filter active turfs
* `GET /api/turfs/{id}` — Retrieve detailed turf profile and amenities
* `POST /api/turfs` *(Admin/Owner)* — Create new turf venue listing
* `PUT /api/turfs/{id}` *(Admin/Owner)* — Update turf parameters
* `DELETE /api/turfs/{id}` *(Admin)* — Decommission turf venue

### Slots & Availability (`/api/slots`)
* `GET /api/slots/turf/{turfId}?date=YYYY-MM-DD` — Real-time slot availability matrix
* `POST /api/slots/reserve` — Acquire atomic Redis lock on slots (10-minute hold)
* `POST /api/slots/release` — Manually release held slots on checkout exit
* `POST /api/slots/generate-day` *(Owner/Admin)* — Bulk generate time slots for a calendar day

### Bookings & Payments (`/api/bookings`)
* `POST /api/bookings/create-order` — Create Razorpay order for reserved slots
* `POST /api/bookings/verify-payment` — Verify signature and confirm booking
* `GET /api/bookings/my-bookings` — Retrieve authenticated user's match history
* `POST /api/bookings/cancel/{id}` — Cancel booking and process release

### QR Pass & Admission (`/api/qr`)
* `GET /api/qr/booking/{bookingId}` — Fetch participant QR pass tokens
* `GET /api/qr/verify-code/{code}` — Verify pass by human-readable match code
* `POST /api/qr/check-in` — Execute gate check-in and mark pass consumed

### Owner Analytics (`/api/owner`)
* `GET /api/owner/dashboard` — Turf owner revenue, active bookings, and utilization analytics
* `GET /api/owner/turfs` — Turf facilities managed by authenticated owner

### Platform Administration (`/api/admin`)
* `GET /api/admin/dashboard` — Platform overview metrics
* `GET /api/admin/earnings` — Detailed revenue aggregation across all arenas
* `GET /api/admin/audit-logs` — Security and transactional audit logs
* `POST /api/admin/create-owner` — Provision verified turf owner accounts

---

## 💻 Local Setup & Execution

### Prerequisites
* **Java 21 JDK** installed (`java -version`)
* **MySQL 8.0** running on port 3306 (or via Docker)
* **Redis** running on port 6379 (or via Docker)
* **Maven 3.9+** (or use included `mvnw.cmd` / `./mvnw`)

### 1. Environment Configuration
Copy `.env.example` to `.env` or provide environment variables:
```properties
DB_URL=jdbc:mysql://localhost:3306/buffturf_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=YourSuperSecretKeyForJWTSigningAtLeast256BitsLong123456789
RAZORPAY_KEY_ID=your_razorpay_key_id
RAZORPAY_KEY_SECRET=your_razorpay_key_secret
GEMINI_API_KEY=your_gemini_api_key
GMAIL_USERNAME=your_email@gmail.com
GMAIL_APP_PASSWORD=your_app_password
```

### 2. Build & Run
```bash
# Compile and run unit tests
./mvnw clean test

# Run application locally
./mvnw spring-boot:run
```
The backend will launch at `http://localhost:8080`.

---

## 🐳 Docker Deployment

The application includes a multi-stage Docker build with a lightweight Alpine JRE runtime:

```bash
# Build Docker image
docker build -t buffturf-backend .

# Run container
docker run -d -p 8080:8080 \
  -e DB_URL="jdbc:mysql://host.docker.internal:3306/buffturf_db?useSSL=false&serverTimezone=Asia/Kolkata" \
  -e DB_USERNAME="root" \
  -e DB_PASSWORD="your_password" \
  -e REDIS_HOST="host.docker.internal" \
  --name buffturf_backend buffturf-backend
```

---

## 🧪 Testing & Code Coverage

Execute the automated test suite with JaCoCo report generation:
```bash
./mvnw clean test jacoco:report
```
HTML coverage reports are generated at `target/site/jacoco/index.html`.

---

## 🔒 Security Compliance

* **No Credentials in Code:** Zero hardcoded API keys, database credentials, or email secrets are stored in version control.
* **Safe Fallbacks:** Production builds expect secrets injected via container environment variables or CI/CD secrets.
* **Tamper-Proof Verification:** Razorpay signatures verified via HMAC-SHA256 before state persistence.

---

## 📄 License

Distributed under the **MIT License**.
