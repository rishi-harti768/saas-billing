# Feature Specification: Analytics & Documentation

**Feature Branch**: `004-analytics-documentation`
**Created**: 2026-01-31
**Status**: Draft
**Input**: User description: "Phase 5: Analytics & Documentation (Viva Readiness)"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Revenue Analytics Dashboard (Priority: P1)

As an Admin, I want to view Monthly Recurring Revenue (MRR) and Churn Rate so that I can track the business performance.

**Why this priority**: Essential for business owners to monitor financial health and meets Phase 5 requirements.

**Independent Test**: Can be tested by seeding subscription data and querying the analytics endpoints.

**Acceptance Scenarios**:

1. **Given** a database with active monthly subscriptions, **When** Admin requests MRR, **Then** the system returns the sum of all active plan prices.
2. **Given** a database with cancelled subscriptions, **When** Admin requests Churn Rate, **Then** the system returns the correct percentage of cancellations vs total users.
3. **Given** a non-admin user, **When** they attempt to access analytics endpoints, **Then** the system returns 403 Forbidden.

---

### User Story 2 - API Documentation (Priority: P1)

As a Developer, I want to view interactive API documentation so that I can understand and integrate with the backend endpoints.

**Why this priority**: a Critical requirement for Viva Readiness and documentation score.

**Independent Test**: Can be tested by navigating to the Swagger UI URL.

**Acceptance Scenarios**:

1. **Given** the application is running, **When** user navigates to `/swagger-ui.html` (or configured path), **Then** the Swagger UI loads successfully.
2. **Given** the Swagger UI, **When** user inspects an endpoint (e.g., `POST /api/v1/auth/login`), **Then** they see description, parameters, and example request body.

---

### Edge Cases

- **No Data**: If no subscriptions exist, MRR and Churn endpoints MUST return 0 (or appropriately structured empty response) with 200 OK status, not an error.
- **Large Dataset**: If aggregation takes longer than 5 seconds, the request SHOULD timeout gracefully with a 503 or 504 error, preventing database lockup.
- **Unauthorized Access**: Users without `ROLE_ADMIN` attempting to access analytics MUST receive a 403 Forbidden response.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a REST API endpoint (e.g., `/api/v1/analytics/mrr`) to retrieve Monthly Recurring Revenue.
- **FR-002**: System MUST provide a REST API endpoint (e.g., `/api/v1/analytics/churn`) to retrieve Churn Rate.
- **FR-003**: The Revenue Analytics endpoints MUST be secured and accessible ONLY to users with `ROLE_ADMIN`.
- **FR-004**: System MUST use optimized database queries (e.g., JPQL `SUM`, `GROUP BY`) to aggregate revenue data.
- **FR-005**: System MUST expose OpenAPI v3 documentation at a standard URL (e.g `/swagger-ui.html` and `/v3/api-docs`).
- **FR-006**: API documentation MUST include operation descriptions, parameter descriptions, and schema examples for all public endpoints.

### Key Entities

- **RevenueStats**: Aggregated data object containing period (Month/Year) and Total Amount.
- **ChurnStats**: Data object containing total users, cancelled users, and churn percentage.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Analytics queries (MRR/Churn) execute in under 500ms for a dataset of 10,000 subscriptions.
- **SC-002**: 100% of public Controllers and DTOs are documented with Swagger annotations.
- **SC-003**: Swagger UI loads with no console errors and allows successful execution of "Try it out" requests.
