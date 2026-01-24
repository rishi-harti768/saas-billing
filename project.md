# 🚀 Project PRD: Enterprise SaaS Billing Engine

**Focus:** Scalable Subscription Management, Automated Invoicing, and Usage Analytics.

## 1. Project Overview

This is a robust backend engine designed to handle multi-tenant subscription lifecycles. It manages plans, tracks usage-based billing, automates recurring payments, and provides financial insights for SaaS companies.

---

## 2. Strategic Feature Map (Max Score Alignment)

To hit that **15% Advanced Features** and **15% Security** weight, we’ve mapped the PRD requirements to specific SaaS Billing scenarios:

| PRD Requirement          | Enterprise SaaS Billing Engine Implementation (The "Score Booster")                       |
| ------------------------ | ----------------------------------------------------------------------------------------- |
| **Complex Queries**      | Monthly Recurring Revenue (MRR) & Churn Rate calculations using custom JPQL/Criteria API. |
| **Caching**              | Redis caching for "Active Subscription Plans" to reduce DB load on every request.         |
| **File Upload**          | Generation of PDF Invoices and uploading them to AWS S3 or a local file server.           |
| **Rate Limiting**        | Tiered API limits (e.g., Free tier gets 100 req/min, Pro gets 1000).                      |
| **External Integration** | **Stripe API** for payment processing & **JavaMailSender** for billing alerts.            |
| **Analytics APIs**       | Dashboard endpoints for Revenue Growth, User Distribution, and Plan Popularity.           |

---

## 3. Database Modeling (15% Weight)

_To score high, use a relational structure (PostgreSQL) to handle ACID transactions for financial data._

- **Users:** Admin (SaaS Owner), Customer (Subscriber).
- **Plans:** Tier name, price, billing cycle (Monthly/Yearly), features.
- **Subscriptions:** Linkage between User and Plan, Status (Active, Canceled, Past Due).
- **Invoices:** Amount, PDF Link, Payment Status, Timestamp.
- **Usage Logs:** For metered billing (e.g., "API calls used").

---

## 4. Technical Architecture (Standard Compliant)

You must follow the layered architecture strictly to secure the **10% Code Quality** score:

- `controller/`: SubscriptionController, InvoiceController, etc. (No logic here!)
- `dto/`: Request/Response objects (e.g., `SubscriptionRequestDTO`) to hide DB entities.
- `exception/`: `InsufficientBalanceException`, `SubscriptionExpiredException`.
- `config/`: SecurityConfig (JWT), RedisConfig, SwaggerConfig.

---

## 5. Mandatory API Endpoints

### 5.1 User & Auth

- `POST /api/v1/auth/register` - With Role-Based Access Control (RBAC).
- `POST /api/v1/auth/login` - Returns JWT.

### 5.2 Subscription Management (Core Domain)

- `GET /api/v1/plans` - Cached list of available tiers.
- `POST /api/v1/subscriptions/subscribe` - Links user to a plan.
- `PUT /api/v1/subscriptions/cancel/{id}` - Soft delete/cancellation logic.

### 5.3 Billing & Invoices

- `GET /api/v1/invoices/my-invoices` - Paginated & sorted list for the user.
- `GET /api/v1/invoices/download/{id}` - Triggers file download.

### 5.4 Admin Analytics

- `GET /api/v1/admin/analytics/mrr` - Complex query calculating revenue.
- `GET /api/v1/admin/analytics/usage-stats` - Aggregated data.

---

## 6. How to Ace the Viva (15% Weight)

The examiners will look for **"Engineering Maturity."** Be ready to explain:

1. **Concurrency:** "How do you handle two people subscribing at the same microsecond?" (Talk about Database Transactions `@Transactional`).
2. **Security:** "How is the JWT validated?" (Talk about the `OncePerRequestFilter`).
3. **Performance:** "Why did you use Redis for Plans?" (Explain that Plans change rarely but are read constantly).
4. **Error Handling:** Show off your `@ControllerAdvice`—this proves you handle errors globally and professionally.

---

## 7. Bonus Point Strategy (Optional ⭐)

If you want to go above and beyond:

- **Docker:** Provide a `docker-compose.yml` that starts Spring Boot, PostgreSQL, and Redis with one command.
- **Email:** Use a real SMTP (like Mailtrap) to send a "Payment Success" email with the invoice attached.
