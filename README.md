# FX Exposure Management Platform

An enterprise-grade Foreign Exchange (FX) Exposure Management, Hedging, Derivatives Valuation, and Treasury Risk Analytics Platform built with **Spring Boot 3.3**, **Java 21**, **Spring Security + JWT**, and **Spring Data JPA**.

---

## 🌟 Key Features

1. **FX Exposure Management**:
   - Track underlying corporate cash flows, receivable invoices, payable bills, forecasted revenues/expenses, and balance sheet assets/liabilities across international subsidiaries.
   - Real-time conversion to Base Currency (`USD`).
   - Dynamic hedging status tracking (`UNHEDGED`, `PARTIALLY_HEDGED`, `FULLY_HEDGED`, `SETTLED`).

2. **Derivative Deal Capture & Valuation (Hedges)**:
   - Book and manage FX derivatives: **Spot**, **Forward**, **FX Swaps**, and **FX Options (Call / Put)**.
   - Real-time **Mark-to-Market (MtM)** fair-value revaluation against latest spot and forward curves.
   - Maturity settlement engine calculating realized P&L.

3. **Hedge Accounting & Allocation**:
   - Link derivative deals directly to underlying commercial exposures with multi-level capacity checks and currency matching.
   - Recalculates coverage ratios and unallocated hedge capacities in real-time.

4. **Quantitative Risk Analytics Engine**:
   - **Net Open Exposure (NOE)** & **Hedge Ratio (%)** calculated per currency and consolidated in portfolio base currency.
   - **Cash Flow Maturity Ladder**: Time-bucketed liquidity ladder (`0-30d`, `31-60d`, `61-90d`, `91-180d`, `180+d`).
   - **"What-If" Scenario Stress Testing**: Simulate uniform or currency-specific FX rate shocks (+/- 5%, 10%, 20%) to calculate net portfolio P&L impact.
   - **Value at Risk (VaR)**: 1-Month Parametric VaR computed at 95% and 99% confidence levels.

5. **Risk Policies & Automated Compliance Monitoring**:
   - Set corporate risk mandates for Maximum Open Exposure, Minimum Hedge Ratio (e.g. 70%), and Counterparty Bank limits.
   - Automated compliance alerts for threshold breaches, low hedge ratios, and contracts maturing within 7 days.

6. **Security & Role-Based Access Control (RBAC)**:
   - Stateless JWT Bearer Token Authentication.
   - Roles: `ADMIN`, `MANAGER`, `ANALYST`.

7. **Interactive Web Dashboard & OpenAPI Documentation**:
   - Built-in, responsive single-page Treasury Dashboard accessible at `http://localhost:8080/`.
   - Complete OpenAPI / Swagger UI documentation at `http://localhost:8080/swagger-ui/index.html`.

---

## 🚀 Getting Started

### Prerequisites
- **Java 21** or later
- **Maven 3.9+** (or use the configured Maven binary)

### Running the Application

1. **Run with In-Memory H2 (Default zero-config mode)**:
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-23_windows-x64_bin\jdk-23.0.1"
   & "D:\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
   ```

2. **Run with MySQL**:
   Create the `fx_exposure_db` database in MySQL, then set:
   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/fx_exposure_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "your-mysql-password"
   $env:DB_DRIVER = "com.mysql.cj.jdbc.Driver"
   & "D:\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
   ```

---

## 🔑 Pre-Seeded Demo Credentials

The platform initializes with pre-configured demo users:

| Name | Email | Password | Role | Permissions |
| :--- | :--- | :--- | :--- | :--- |
| **Admin User** | `admin@example.com` | `Admin@123` | `ADMIN` | Full access, user management, risk policies, deal settlements |
| **Treasury Manager** | `manager@example.com` | `Manager@123` | `MANAGER` | Book deals, link allocations, manage exposures & policies |
| **Risk Analyst** | `analyst@example.com` | `Analyst@123` | `ANALYST` | View dashboards, stress testing, create exposures |

---

## 🌐 Endpoints & UI Links

- **Interactive Treasury UI**: `http://localhost:8080/`
- **Swagger UI API Documentation**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON Docs**: `http://localhost:8080/v3/api-docs`
- **System Health Check**: `http://localhost:8080/api/health`
- **H2 Database Console**: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:fx_exposure_db`, User: `sa`, Password: empty)

---

## 🧪 Running Automated Tests

Run the test suite covering domain services, risk analytics, and REST API controllers:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-23_windows-x64_bin\jdk-23.0.1"
& "D:\apache-maven-3.9.9\bin\mvn.cmd" test
```
