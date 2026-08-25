# FX Exposure Management Platform

**An enterprise-inspired FX treasury and risk management platform for managing currency exposures, hedging strategies, derivative valuation, and quantitative FX risk analytics.**

Built with **Java 21**, **Spring Boot 3.3**, **Spring Security + JWT**, and **Spring Data JPA**.

The platform models a corporate treasury workflow from **underlying FX exposure → hedge allocation → derivative valuation → portfolio risk analytics → policy monitoring**.

---

## 🎯 What Problem Does It Solve?

Multinational companies are exposed to foreign-exchange risk through:

* Foreign-currency receivables and payables
* Forecasted revenues and expenses
* Intercompany transactions
* Foreign-currency assets and liabilities
* Existing FX hedges

This platform provides a centralized view of those exposures and helps treasury and risk teams understand:

* How much FX exposure remains unhedged
* Whether hedge coverage meets corporate policy
* How derivatives affect the overall FX position
* How the portfolio behaves under FX shocks
* Which exposures and derivatives are approaching maturity
* Whether risk limits or counterparty limits have been breached

---

## 🏗️ Platform Architecture

```text
                    ┌──────────────────────┐
                    │   Treasury Dashboard │
                    │     / REST APIs      │
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │   Spring Boot API    │
                    │ Security / JWT / RBAC │
                    └──────────┬───────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
┌───────▼────────┐   ┌─────────▼─────────┐   ┌────────▼────────┐
│ Exposure Engine │   │ Derivative Engine │   │   Risk Engine   │
│                │   │                   │   │                 │
│ Cash Flows     │   │ Forwards          │   │ NOE             │
│ Receivables    │   │ Swaps             │   │ Hedge Ratio     │
│ Payables       │   │ Options           │   │ VaR             │
│ Forecasts      │   │ MtM / P&L         │   │ Stress Testing  │
└───────┬────────┘   └─────────┬─────────┘   │ Maturity Ladder │
        │                      │             └────────┬────────┘
        └──────────────────────┼──────────────────────┘
                               │
                    ┌──────────▼───────────┐
                    │ Hedge Allocation &   │
                    │ Risk Policy Engine   │
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │    JPA / Database    │
                    │       H2 / MySQL     │
                    └──────────────────────┘
```

---

## 🌟 Key Features

### 1. FX Exposure Management

Track and manage underlying corporate FX exposures including:

* Receivable invoices
* Payable bills
* Forecasted revenues and expenses
* Balance-sheet assets and liabilities
* International subsidiary exposures
* Exposure currency and maturity information

The platform converts exposures into the configured **base currency (USD)** and dynamically tracks their hedging status:

`UNHEDGED → PARTIALLY_HEDGED → FULLY_HEDGED → SETTLED`

---

### 2. FX Derivative Deal Capture & Valuation

Capture and manage common FX derivative instruments:

* **Spot**
* **Forward**
* **FX Swap**
* **FX Option — Call / Put**

The valuation engine supports:

* Mark-to-Market (MtM) revaluation
* Spot-rate based valuation
* Forward-curve based valuation
* Maturity settlement
* Realized P&L calculation

---

### 3. Hedge Allocation

Derivatives can be allocated directly against underlying commercial exposures.

The allocation engine performs:

* Currency matching
* Hedge-capacity validation
* Exposure-level allocation
* Remaining/unallocated hedge capacity calculation
* Hedge coverage recalculation
* Hedge-ratio monitoring

This allows the platform to distinguish between **total derivative positions** and the portion actually assigned to underlying exposures.

---

### 4. Quantitative FX Risk Analytics

#### Net Open Exposure (NOE)

Calculates the remaining FX exposure after considering allocated hedges.

NOE can be analyzed:

* By currency
* By exposure
* By maturity
* At consolidated portfolio level

#### Hedge Ratio

Measures the percentage of exposure covered by eligible hedges.

```text
Hedge Ratio = Hedged Exposure / Gross Exposure × 100
```

#### Cash Flow Maturity Ladder

Exposures are grouped into maturity buckets:

| Bucket      | Period      |
| ----------- | ----------- |
| 0–30 days   | Near-term   |
| 31–60 days  | Short-term  |
| 61–90 days  | Short-term  |
| 91–180 days | Medium-term |
| 180+ days   | Long-term   |

#### FX Stress Testing

Run "What-If" scenarios against the portfolio using FX shocks such as:

* ±5%
* ±10%
* ±20%

The engine calculates the resulting estimated portfolio P&L impact.

Scenarios can be applied uniformly or to specific currencies.

#### Value at Risk (VaR)

The platform calculates **1-month parametric VaR** at:

* 95% confidence
* 99% confidence

VaR provides an estimate of potential portfolio loss under normal market conditions based on the configured risk model.

---

### 5. Treasury Risk Policies & Compliance Monitoring

Define corporate treasury risk mandates such as:

* Maximum Open Exposure
* Minimum Hedge Ratio
* Counterparty Bank Limits
* Near-term maturity thresholds

The platform automatically identifies conditions such as:

* Excessive open FX exposure
* Hedge ratio below policy
* Counterparty limit breaches
* Derivatives approaching maturity
* Other configured risk-policy violations

---

### 6. Security & Role-Based Access Control

Authentication is implemented using **stateless JWT Bearer Tokens**.

Supported roles:

| Role      | Example Responsibilities                                          |
| --------- | ----------------------------------------------------------------- |
| `ADMIN`   | User management, risk policies, settlements, full platform access |
| `MANAGER` | Book deals, allocate hedges, manage exposures and policies        |
| `ANALYST` | Analyze exposures, dashboards and stress scenarios                |

API endpoints are protected using role-based authorization.

---

### 7. Interactive Treasury Dashboard

The application includes a responsive web dashboard providing visibility into:

* Total FX exposure
* Hedged vs. unhedged exposure
* Hedge ratios
* Currency positions
* Maturity buckets
* Risk metrics
* Stress-test results
* Policy/compliance alerts

**Dashboard:**

`http://localhost:8080/`

**Swagger UI:**

`http://localhost:8080/swagger-ui/index.html`

---

## 🔄 Example Treasury Workflow

A typical workflow looks like this:

```text
1. Record underlying exposure
          ↓
2. Determine currency and maturity
          ↓
3. Calculate gross FX exposure
          ↓
4. Book FX hedge / derivative
          ↓
5. Allocate hedge against exposure
          ↓
6. Recalculate hedge ratio and NOE
          ↓
7. Revalue derivative / calculate MtM
          ↓
8. Run VaR and stress scenarios
          ↓
9. Check corporate risk policies
          ↓
10. Generate alerts / monitor maturity
```

### Example

A company has a **USD 5M receivable** due in 90 days.

The treasury team can:

1. Record the receivable as an FX exposure.
2. Book a USD/INR forward.
3. Allocate the forward against the receivable.
4. Recalculate the remaining open exposure.
5. Calculate the hedge ratio.
6. Revalue the forward using current market assumptions.
7. Stress the USD/INR rate by ±10%.
8. Check whether the resulting exposure remains within treasury policy.

---

## 🧮 Financial Risk Models

The platform currently focuses on the following core analytics:

| Metric            | Purpose                                 |
| ----------------- | --------------------------------------- |
| Net Open Exposure | Measures remaining unhedged FX position |
| Hedge Ratio       | Measures hedge coverage                 |
| MtM               | Estimates current derivative fair value |
| Realized P&L      | Calculates settlement P&L               |
| Maturity Ladder   | Shows timing of FX cash flows           |
| Stress Testing    | Estimates P&L under FX shocks           |
| Parametric VaR    | Estimates potential portfolio loss      |
| Risk Limits       | Monitors exposure against policy        |

> **Note:** The calculations are intended for portfolio/risk-management simulation and demonstration purposes and should not be interpreted as production trading or accounting valuations without appropriate market-data, model-validation, and accounting controls.

---

## 🛠️ Technology Stack

### Backend

* **Java 21**
* **Spring Boot 3.3**
* Spring Web
* Spring Data JPA
* Spring Security
* JWT Authentication
* Hibernate
* Maven

### Database

* **H2** — zero-configuration development/demo mode
* **MySQL** — persistent database option

### API & Documentation

* REST APIs
* OpenAPI
* Swagger UI

### Testing

* Automated unit/service tests
* REST/controller tests
* Risk analytics test coverage

---

## 🚀 Getting Started

### Prerequisites

* **Java 21+**
* **Maven 3.9+**
* MySQL 8+ *(optional)*

### Option 1 — Run with H2

The application can run using an in-memory H2 database without external database configuration.

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-23_windows-x64_bin\jdk-23.0.1"
& "D:\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
```

> Adjust `JAVA_HOME` and the Maven path according to your local installation.

### Option 2 — Run with MySQL

Create the database:

```sql
CREATE DATABASE fx_exposure_db;
```

Then configure:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/fx_exposure_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your-mysql-password"
$env:DB_DRIVER = "com.mysql.cj.jdbc.Driver"

& "D:\apache-maven-3.9.9\bin\mvn.cmd" spring-boot:run
```

---

## 🔑 Demo Credentials

The application initializes with pre-seeded demonstration users.

| User             | Email                 | Password      | Role      |
| ---------------- | --------------------- | ------------- | --------- |
| Admin User       | `admin@example.com`   | `Admin@123`   | `ADMIN`   |
| Treasury Manager | `manager@example.com` | `Manager@123` | `MANAGER` |
| Risk Analyst     | `analyst@example.com` | `Analyst@123` | `ANALYST` |

> **Security note:** These credentials are for local demonstration only. Change or remove seeded credentials before deploying to any non-development environment.

---

## 🌐 Application Endpoints

| Resource           | URL                                           |
| ------------------ | --------------------------------------------- |
| Treasury Dashboard | `http://localhost:8080/`                      |
| Swagger UI         | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON       | `http://localhost:8080/v3/api-docs`           |
| Health Check       | `http://localhost:8080/api/health`            |
| H2 Console         | `http://localhost:8080/h2-console`            |

### H2 Console

For the default H2 configuration:

```text
JDBC URL: jdbc:h2:mem:fx_exposure_db
User: sa
Password: <empty>
```

---

## 🧪 Running Tests

Run the complete test suite:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-23_windows-x64_bin\jdk-23.0.1"
& "D:\apache-maven-3.9.9\bin\mvn.cmd" test
```

The test suite covers areas including:

* Domain/service logic
* Exposure calculations
* Hedge allocation
* Risk analytics
* REST controllers

---

## 📚 API Documentation

Once the application is running, interactive API documentation is available through Swagger UI:

`http://localhost:8080/swagger-ui/index.html`

OpenAPI specification:

`http://localhost:8080/v3/api-docs`

Swagger can be used to explore and test the available REST endpoints.

---

## 🔐 Security Considerations

The application demonstrates:

* JWT-based authentication
* Stateless API security
* Role-based authorization
* Protected REST endpoints
* Separation of user permissions

For production deployment, additional controls would be required, including secure secret management, credential rotation, HTTPS, audit logging, hardened database configuration, monitoring, and production-grade identity management.

---

## 🔮 Future Enhancements

Potential extensions include:

* [ ] Live FX market-data integration
* [ ] Historical VaR and Monte Carlo VaR
* [ ] FX option pricing using Garman-Kohlhagen / Black-Scholes methodology
* [ ] Interest-rate curves for forward pricing
* [ ] Redis-based market-data caching
* [ ] Event-driven trade/exposure processing
* [ ] Complete trade audit history
* [ ] Counterparty credit-risk analytics
* [ ] Hedge effectiveness reporting
* [ ] IFRS 9 hedge-accounting workflows
* [ ] Docker / Docker Compose deployment
* [ ] CI/CD pipeline
* [ ] Production observability and metrics
* [ ] Advanced hedge recommendation/optimization engine

---

## 🎓 Project Objective

This project was built to explore the intersection of:

**Financial Markets + Treasury Management + Quantitative Risk + Backend Engineering**

The goal is to demonstrate how financial-domain concepts can be translated into a modular, secure, and testable backend system using modern Java and Spring Boot technologies.

---

## 📌 Disclaimer

This project is intended for **educational, portfolio, and demonstration purposes**.

Financial calculations and valuations are simplified models and should not be used for live trading, financial reporting, regulatory submissions, or accounting decisions without appropriate market data, model validation, controls, and professional review.
