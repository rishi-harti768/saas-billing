## 🏗️ Phase 1: Authentication & Multi-Tenancy (Security & Design)

This phase secures your **15% Security** and **20% API Design** scores.

- **Feature 1.1: JWT Security Framework**
- **Spec:** Implement Spring Security with a stateless JWT filter.
- **Details:** Support roles: `ROLE_ADMIN` (SaaS Owner) and `ROLE_USER` (Subscriber).
- **Score Booster:** Implement a `CustomUserDetails` to store `tenantId` or `organizationId` for multi-tenant isolation.

- **Feature 1.2: Identity Management**
- **Spec:** Registration and Login endpoints with password encoding (BCrypt).
- **Validation:** Use `@Email`, `@NotBlank`, and `@Size` on DTOs.

---

## 📊 Phase 2: Product & Subscription Engine (Core Domain)

This phase satisfies the **Core Domain APIs** and **Database Design** requirements.

- **Feature 2.1: Plan Management**
- **Spec:** CRUD for Billing Plans (Free, Pro, Enterprise).
- **Details:** Define price, billing cycle (MONTHLY/YEARLY), and feature limits.

- **Feature 2.2: Subscription Lifecycle**
- **Spec:** Endpoints to subscribe, upgrade, or cancel.
- **Logic:** Implement logic to handle "Active," "Past Due," and "Canceled" states.
- **Database:** Use JPA Relationships (`@OneToMany`) between Users and Subscriptions.

---

## 💸 Phase 3: Billing, Payments & External Integration

This fulfills the **Mandatory Integrations** and **File Handling** requirements.

- **Feature 3.1: External Payment Integration**
- **Spec:** Integrate with **Stripe** or **PayPal** (Mocking is allowed, but using a real Sandbox scores higher).
- **Details:** Webhook listener to update subscription status when a payment succeeds.

- **Feature 3.2: Automated Invoicing**
- **Spec:** Generate a PDF invoice after every successful transaction.
- **File Storage:** Save the PDF to a `service/uploads` directory or cloud storage and provide a download link.

- **Feature 3.3: Email Notifications**
- **Spec:** Use `JavaMailSender` to send an automated "Payment Successful" email with the invoice attached.

---

## ⚡ Phase 4: Performance & Advanced Utilities

This phase locks in the **15% Advanced Features** and **10% Performance** scores.

- **Feature 4.1: Tiered Rate Limiting**
- **Spec:** Use **Bucket4j** or Spring Cloud Gateway to limit API calls based on the user's plan.
- **Details:** Free users: 10 req/min; Pro users: 100 req/min.

- **Feature 4.2: Caching Strategy**
- **Spec:** Implement `@Cacheable` using Redis or Caffeine for the "List Plans" API.

- **Feature 4.3: Global Exception Handling**
- **Spec:** A `@ControllerAdvice` class to catch `SubscriptionNotFoundException` or `PaymentFailedException` and return clean JSON responses.

---

## 📈 Phase 5: Analytics & Documentation (Viva Readiness)

This prepares you for the **Viva**, **Demo**, and **Documentation** scores.

- **Feature 5.1: Revenue Analytics Dashboard**
- **Spec:** Specialized endpoints for Admin to see Monthly Recurring Revenue (MRR) and Churn Rate.
- **Complex Query:** Use JPQL or Native SQL with `SUM` and `GROUP BY` to aggregate monthly earnings.

- **Feature 5.2: API Documentation**
- **Spec:** Fully annotated **Swagger/OpenAPI** UI.
- **Detail:** Include descriptions for every parameter and example request bodies.
