# SIP Banking Application

A full-stack Systematic Investment Plan (SIP) banking application built with Spring Boot and React + Tailwind CSS.

---

## Tech Stack

| Layer      | Technology                                      |
|------------|-------------------------------------------------|
| Backend    | Spring Boot 3.2, Spring Security, JPA, JWT      |
| Database   | MySQL 8                                         |
| Frontend   | React 18, Vite, Tailwind CSS, Axios             |
| Scheduler  | Spring `@Scheduled` (daily cron)                |

---

## Project Structure

```
sip-app/
├── backend/
│   └── src/main/java/com/sipapp/
│       ├── SipBankingApplication.java
│       ├── config/         # SecurityConfig, JwtUtil, JwtAuthFilter, CorsConfig
│       ├── controller/     # AuthController, SipController
│       ├── dto/            # AuthDto, SipDto, TransactionDto, DashboardDto
│       ├── entity/         # User, Passbook, Sip, Transaction
│       ├── enums/          # Frequency, SipStatus, TransactionStatus
│       ├── exception/      # GlobalExceptionHandler, custom exceptions
│       ├── repository/     # UserRepository, PassbookRepository, SipRepository, TransactionRepository
│       ├── scheduler/      # SipScheduler
│       └── service/        # AuthService, SipService, DashboardService, UserDetailsServiceImpl
└── frontend/
    └── src/
        ├── App.jsx
        ├── context/        # AuthContext
        ├── services/       # api.js, authService.js, sipService.js
        ├── components/     # Navbar, SipCard, ProgressBar, StatusBadge
        └── pages/          # LoginPage, SignupPage, DashboardPage, CreateSipPage, SipDetailsPage
```

---

## Prerequisites

- Java 17+
- Maven 3.8+
- MySQL 8
- Node.js 18+ and npm

---

## Backend Setup (IntelliJ IDEA)

### 1. Configure MySQL

Open MySQL and create the database:

```sql
CREATE DATABASE sip_banking;
```

### 2. Update application.properties

Edit `backend/src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/sip_banking?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

Also set your own JWT secret (any 256-bit base64 string):

```properties
app.jwt.secret=YOUR_BASE64_SECRET_KEY_HERE
```

### 3. Open in IntelliJ

1. Open IntelliJ IDEA
2. File → Open → select the `backend/` folder
3. IntelliJ will auto-detect Maven — let it download dependencies
4. Run `SipBankingApplication.java`

The server starts on `http://localhost:8080`

### 4. Swagger UI

Open `http://localhost:8080/swagger-ui.html` to explore all APIs interactively.

---

## Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

The app opens at `http://localhost:5173`

---

## API Reference

### Auth

| Method | Endpoint          | Body                             | Auth |
|--------|-------------------|----------------------------------|------|
| POST   | /api/auth/signup  | `{name, email, password}`        | No   |
| POST   | /api/auth/login   | `{email, password}`              | No   |

**Signup response** includes your auto-generated `passbookId` (e.g. `ALPHA123`).

**Login response** includes a `token` — pass it as `Authorization: Bearer <token>` on all subsequent requests.

### SIP

| Method | Endpoint          | Description                              | Auth |
|--------|-------------------|------------------------------------------|------|
| POST   | /api/sip/create   | Create new SIP + first transaction       | Yes  |
| GET    | /api/sip/all      | Get all SIPs for logged-in user          | Yes  |
| GET    | /api/sip/{id}     | Get SIP details with all transactions    | Yes  |
| GET    | /api/dashboard    | Summary + all SIPs                       | Yes  |

### Create SIP payload example

```json
{
  "passbookId": "ALPHA123",
  "amount": 5000,
  "frequency": "MONTHLY",
  "totalInstallments": 12,
  "startDate": "2024-04-01",
  "trust": true,
  "interestRate": 8.5,
  "isSip": true
}
```

- `trust: false` → system default interest rate (5.0%) is used
- `trust: true` → you must supply `interestRate` between 0 and 15

---

## Business Logic

### Passbook ID Generation
Each user gets one passbook on signup. The ID is auto-generated as 5 uppercase letters + 3 digits (e.g. `ALPHA123`), guaranteed unique.

### SIP Creation Flow
1. Validate passbookId (mandatory, must belong to authenticated user)
2. Validate totalInstallments ≥ 1
3. If trust = true, validate interestRate in range 0–15; otherwise use default from config
4. Save the SIP
5. Create the **first transaction only** (remaining are created by the scheduler)

### Interest Calculation (Compound)
```
Interest = P × ((1 + r)^n - 1)
```
Where:
- P = installment amount
- r = annual interest rate / 100
- n = installment number

Interest and total amount are stored per transaction row.

### Scheduler
Runs every day at midnight via `@Scheduled(cron = "0 0 0 * * *")`.

For each ACTIVE SIP:
1. Find the current PENDING transaction
2. If today ≥ transaction date → mark it COMPLETED
3. If all installments done → mark SIP as COMPLETED
4. Otherwise → create the next transaction with calculated next date

**Date arithmetic (frequency-aware, Feb-safe):**
- DAILY  → `plusDays(1)`
- WEEKLY → `plusWeeks(1)`
- MONTHLY → `plusMonths(1)` ← Java handles Jan 31 → Feb 28 correctly
- YEARLY → `plusYears(1)`

Each SIP is processed in its own try-catch so one failure never stops others.

### Log format
```
[SIP] ALPHA123 → Installment 3 completed (SIP ID: 7)
```

---

## Running the Full App

1. Start MySQL
2. Start Spring Boot backend (`SipBankingApplication`)
3. Start React frontend (`npm run dev`)
4. Open `http://localhost:5173`
5. Sign up → note your Passbook ID → Login → Create SIP → View Details

---

## Default Configuration

| Setting               | Value                    |
|-----------------------|--------------------------|
| Server port           | 8080                     |
| Frontend port         | 5173                     |
| Default interest rate | 5.0%                     |
| JWT expiry            | 24 hours                 |
| Scheduler cron        | Every day at midnight    |
| JPA DDL               | auto-update (auto-creates tables) |

---

## Troubleshooting

**MySQL connection refused**
- Ensure MySQL service is running
- Check username/password in `application.properties`

**CORS errors**
- Frontend must run on `localhost:5173` or `localhost:3000` (configured in `CorsConfig.java`)

**JWT 401 errors**
- Token may have expired (24h). Log out and log in again.

**Tables not created**
- `spring.jpa.hibernate.ddl-auto=update` creates tables automatically on first run.
