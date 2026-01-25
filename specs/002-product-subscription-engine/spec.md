# Feature Specification: Product & Subscription Engine

**Feature Branch**: `002-product-subscription-engine`  
**Created**: 2026-01-25  
**Status**: Draft  
**Input**: Phase 2: Product & Subscription Engine (Core Domain) - Plan Management (CRUD for Billing Plans: Free, Pro, Enterprise with price, billing cycle, and feature limits) and Subscription Lifecycle (subscribe, upgrade, cancel with Active, Past Due, and Canceled states)

## User Scenarios & Testing _(mandatory)_

### User Story 1 - SaaS Owner Creates and Manages Billing Plans (Priority: P1)

A SaaS platform owner needs to create, view, update, and delete billing plans (Free, Pro, Enterprise) to define the pricing structure and feature offerings for their platform. This is the foundation for the entire subscription business model.

**Why this priority**: Without billing plans, there's nothing for customers to subscribe to. This is the absolute minimum viable functionality needed to offer subscription services.

**Independent Test**: Can be fully tested by creating multiple billing plans with different tiers, prices, and billing cycles, then retrieving, updating, and deleting them through the management interface.

**Acceptance Scenarios**:

1. **Given** a SaaS owner is authenticated as ROLE_ADMIN, **When** they create a new billing plan with name "Pro", price $29.99, billing cycle MONTHLY, and feature limits, **Then** the plan is saved and assigned a unique identifier
2. **Given** existing billing plans in the system, **When** a SaaS owner requests a list of all plans, **Then** all plans are returned with their complete details including price, billing cycle, and feature limits
3. **Given** an existing billing plan, **When** a SaaS owner updates the plan's price from $29.99 to $39.99, **Then** the plan is updated and the new price is reflected for future subscriptions
4. **Given** an existing billing plan with no active subscriptions, **When** a SaaS owner deletes the plan, **Then** the plan is removed from the system
5. **Given** an existing billing plan with active subscriptions, **When** a SaaS owner attempts to delete the plan, **Then** the system prevents deletion and returns an error message indicating active subscriptions exist

---

### User Story 2 - Customer Subscribes to a Billing Plan (Priority: P1)

A customer needs to subscribe to a billing plan to gain access to the SaaS platform's features and services. This is the core revenue-generating action for the business.

**Why this priority**: This is equally critical as plan management because it enables the core business model - converting prospects into paying customers. Without subscriptions, there's no revenue.

**Independent Test**: Can be fully tested by selecting an available plan, initiating a subscription, and verifying the subscription is created with "Active" status and correct plan details.

**Acceptance Scenarios**:

1. **Given** an authenticated customer (ROLE_USER) and an available billing plan, **When** the customer initiates a subscription to the plan, **Then** a new subscription is created with status "Active" and linked to both the customer and the plan
2. **Given** a customer with an active subscription, **When** they request their subscription details, **Then** the system returns the subscription with plan information, status, start date, and next billing date
3. **Given** a customer without any subscription, **When** they attempt to access premium features, **Then** the system denies access and prompts them to subscribe
4. **Given** a customer with an active subscription to the "Free" plan, **When** they attempt to subscribe to the "Pro" plan without canceling the existing subscription, **Then** the system prevents duplicate subscriptions and suggests upgrading instead
5. **Given** a new subscription is created, **When** the subscription becomes active, **Then** the next billing date is automatically calculated based on the billing cycle (30 days for MONTHLY, 365 days for YEARLY)

---

### User Story 3 - Customer Upgrades Subscription Plan (Priority: P1)

A customer needs to upgrade their subscription from a lower tier (e.g., Free) to a higher tier (e.g., Pro or Enterprise) to access additional features and increased limits.

**Why this priority**: Upgrades are a critical revenue growth mechanism and a common customer journey. This must work seamlessly to maximize customer lifetime value.

**Independent Test**: Can be fully tested by creating a subscription to a lower-tier plan, initiating an upgrade to a higher-tier plan, and verifying the subscription is updated with the new plan and status remains "Active".

**Acceptance Scenarios**:

1. **Given** a customer with an active "Free" plan subscription, **When** they upgrade to the "Pro" plan, **Then** the subscription is updated to reference the "Pro" plan and status remains "Active"
2. **Given** a customer upgrades from "Free" to "Pro" plan, **When** the upgrade is processed, **Then** the next billing date is recalculated based on the new plan's billing cycle
3. **Given** a customer upgrades from a MONTHLY plan to a YEARLY plan, **When** the upgrade is processed, **Then** the billing cycle is updated and the next billing date is adjusted accordingly
4. **Given** a customer with a "Past Due" subscription, **When** they attempt to upgrade to a higher tier, **Then** the system prevents the upgrade and requires payment of outstanding balance first
5. **Given** a customer upgrades to a higher-tier plan, **When** the upgrade is successful, **Then** the customer immediately gains access to the new plan's features and limits

---

### User Story 4 - Customer Cancels Subscription (Priority: P1)

A customer needs to cancel their subscription to stop recurring billing and terminate their access to premium features.

**Why this priority**: Providing a clear cancellation path is essential for customer trust, legal compliance, and reducing involuntary churn. This is a mandatory feature for any subscription service.

**Independent Test**: Can be fully tested by creating an active subscription, initiating cancellation, and verifying the subscription status changes to "Canceled" and billing stops.

**Acceptance Scenarios**:

1. **Given** a customer with an "Active" subscription, **When** they request to cancel the subscription, **Then** the subscription status is changed to "Canceled"
2. **Given** a subscription is canceled, **When** the cancellation is processed, **Then** no further billing occurs and the next billing date is cleared
3. **Given** a subscription is canceled, **When** the current billing period ends, **Then** the customer's access to premium features is revoked
4. **Given** a subscription is canceled mid-billing cycle, **When** the cancellation is processed, **Then** the customer retains access until the end of the current paid period
5. **Given** a customer with a "Canceled" subscription, **When** they attempt to cancel again, **Then** the system returns an error indicating the subscription is already canceled
6. **Given** a customer cancels their subscription, **When** they later want to resubscribe, **Then** they can create a new subscription to any available plan

---

### User Story 5 - System Manages Subscription State Transitions (Priority: P2)

The system needs to automatically manage subscription state transitions (Active → Past Due → Canceled) based on payment status and business rules to maintain accurate billing records.

**Why this priority**: While critical for production operations, this is a supporting feature that enhances P1 stories. The system can function with manual state management initially, but automation is essential for scale.

**Independent Test**: Can be fully tested by simulating payment failures, successful payments, and time-based transitions, then verifying the subscription status changes correctly.

**Acceptance Scenarios**:

1. **Given** an "Active" subscription with a failed payment, **When** the payment failure is recorded, **Then** the subscription status is changed to "Past Due"
2. **Given** a "Past Due" subscription, **When** a successful payment is processed, **Then** the subscription status is changed back to "Active"
3. **Given** a "Past Due" subscription, **When** the grace period expires without payment, **Then** the subscription status is changed to "Canceled"
4. **Given** a subscription in any state, **When** a state transition occurs, **Then** the transition is logged with timestamp and reason
5. **Given** a subscription state transition, **When** the transition is invalid (e.g., Canceled → Active without new subscription), **Then** the system prevents the transition and returns an error

---

### User Story 6 - Admin Views Subscription Analytics (Priority: P2)

A SaaS owner needs to view subscription analytics including active subscriptions by plan, churn rate, and subscription trends to make informed business decisions.

**Why this priority**: This provides valuable business insights but is not blocking for core subscription functionality. It can be added after basic subscription operations work.

**Independent Test**: Can be fully tested by creating multiple subscriptions across different plans and states, then verifying the analytics endpoints return accurate counts and metrics.

**Acceptance Scenarios**:

1. **Given** multiple active subscriptions across different plans, **When** an admin requests subscription distribution, **Then** the system returns the count of active subscriptions for each plan
2. **Given** subscriptions with various states, **When** an admin requests subscription status summary, **Then** the system returns counts for Active, Past Due, and Canceled subscriptions
3. **Given** subscriptions created and canceled over time, **When** an admin requests churn rate for a period, **Then** the system calculates and returns the percentage of canceled subscriptions
4. **Given** subscription data over multiple months, **When** an admin requests subscription growth trends, **Then** the system returns time-series data showing new subscriptions, cancellations, and net growth

---

### Edge Cases

- What happens when a customer tries to subscribe to a plan that has been deleted?
- How does the system handle subscription upgrades when the target plan is no longer available?
- What happens when a customer tries to downgrade from a higher tier to a lower tier?
- How does the system handle billing cycle changes during an upgrade (MONTHLY to YEARLY)?
- What happens when a subscription is in "Past Due" state and the customer attempts to upgrade?
- How does the system prevent race conditions when multiple subscription operations occur simultaneously for the same user?
- What happens when a billing plan's price is updated - does it affect existing subscriptions?
- How does the system handle subscription renewals at the end of a billing cycle?
- What happens when a customer's tenant is deleted but they have an active subscription?

## Requirements _(mandatory)_

### Functional Requirements

#### Plan Management

- **FR-001**: System MUST allow ROLE_ADMIN users to create billing plans with name, description, price, billing cycle (MONTHLY/YEARLY), and feature limits
- **FR-002**: System MUST assign a unique identifier to each billing plan upon creation
- **FR-003**: System MUST allow ROLE_ADMIN users to retrieve a list of all billing plans
- **FR-004**: System MUST allow ROLE_ADMIN users to retrieve a single billing plan by its identifier
- **FR-005**: System MUST allow ROLE_ADMIN users to update billing plan details including price, description, and feature limits
- **FR-006**: System MUST allow ROLE_ADMIN users to delete billing plans that have no active subscriptions
- **FR-007**: System MUST prevent deletion of billing plans that have active subscriptions
- **FR-008**: System MUST support at least three standard plan tiers: Free, Pro, and Enterprise
- **FR-009**: System MUST validate that plan prices are non-negative decimal values
- **FR-010**: System MUST validate that plan names are unique within the system
- **FR-011**: System MUST validate that billing cycles are either MONTHLY or YEARLY
- **FR-012**: System MUST allow plans to define feature limits (e.g., API call limits, user limits, storage limits)

#### Subscription Lifecycle

- **FR-013**: System MUST allow authenticated ROLE_USER customers to subscribe to an available billing plan
- **FR-014**: System MUST create subscriptions with an initial status of "Active" upon successful subscription
- **FR-015**: System MUST link each subscription to both a customer (user) and a billing plan
- **FR-016**: System MUST prevent customers from having multiple active subscriptions simultaneously
- **FR-017**: System MUST calculate and store the next billing date based on the plan's billing cycle when a subscription is created
- **FR-018**: System MUST allow customers to retrieve their current subscription details
- **FR-019**: System MUST allow customers to upgrade their subscription to a higher-tier plan
- **FR-020**: System MUST update the subscription's plan reference and recalculate the next billing date when an upgrade occurs
- **FR-021**: System MUST maintain "Active" status when a subscription is upgraded
- **FR-022**: System MUST allow customers to cancel their active subscriptions
- **FR-023**: System MUST change subscription status to "Canceled" when cancellation is requested
- **FR-024**: System MUST stop future billing when a subscription is canceled
- **FR-025**: System MUST support three subscription states: "Active", "Past Due", and "Canceled"
- **FR-026**: System MUST transition subscriptions from "Active" to "Past Due" when payment fails
- **FR-027**: System MUST transition subscriptions from "Past Due" to "Active" when payment succeeds
- **FR-028**: System MUST transition subscriptions from "Past Due" to "Canceled" after a grace period expires
- **FR-029**: System MUST prevent invalid state transitions (e.g., Canceled to Active without new subscription)
- **FR-030**: System MUST log all subscription state transitions with timestamp and reason
- **FR-031**: System MUST enforce tenant isolation for subscriptions (customers can only access their own subscriptions)
- **FR-032**: System MUST prevent subscription operations on plans that have been deleted
- **FR-033**: System MUST allow customers to retain access to features until the end of the current billing period after cancellation

#### Analytics and Reporting

- **FR-034**: System MUST provide ROLE_ADMIN users with subscription count by plan
- **FR-035**: System MUST provide ROLE_ADMIN users with subscription count by status
- **FR-036**: System MUST calculate churn rate as the percentage of canceled subscriptions over a specified period
- **FR-037**: System MUST provide subscription growth trends over time

### Key Entities

- **Billing Plan**: Represents a subscription tier with attributes including unique identifier, name (e.g., "Free", "Pro", "Enterprise"), description, price (decimal), billing cycle (MONTHLY/YEARLY), feature limits (e.g., API calls per month, max users), creation timestamp, and last updated timestamp

- **Subscription**: Represents a customer's active or historical subscription to a billing plan with attributes including unique identifier, customer reference (user ID), billing plan reference (plan ID), subscription status (Active/Past Due/Canceled), start date, next billing date, cancellation date (if applicable), and tenant ID for multi-tenant isolation

- **Subscription State**: Represents the current status of a subscription with three possible values:
  - **Active**: Customer has paid and has full access to plan features
  - **Past Due**: Payment has failed but customer is in grace period
  - **Canceled**: Subscription has been terminated and billing has stopped

- **Feature Limit**: Represents usage or access restrictions defined by a billing plan with attributes including limit type (e.g., "api_calls", "max_users", "storage_gb"), limit value (numeric), and associated billing plan

- **SubscriptionTransitionLog**: Represents a historical record of subscription state changes with attributes including unique identifier, subscription reference, previous state, new state, transition timestamp, and transition reason

## Success Criteria _(mandatory)_

### Measurable Outcomes

- **SC-001**: SaaS owners can create a new billing plan in under 1 minute with all required details
- **SC-002**: Customers can view all available billing plans in under 2 seconds
- **SC-003**: Customers can complete a subscription to a plan in under 30 seconds
- **SC-004**: Subscription upgrades are processed in under 5 seconds with immediate feature access
- **SC-005**: Subscription cancellations are processed in under 3 seconds
- **SC-006**: 100% of subscription state transitions are logged with accurate timestamps and reasons
- **SC-007**: System correctly enforces tenant isolation with 100% accuracy (customers can only access their own subscriptions)
- **SC-008**: System prevents 100% of invalid subscription operations (duplicate subscriptions, invalid state transitions, operations on deleted plans)
- **SC-009**: Admin analytics endpoints return results in under 3 seconds for datasets up to 10,000 subscriptions
- **SC-010**: System correctly calculates next billing dates with 100% accuracy based on billing cycle
- **SC-011**: System supports at least 1,000 concurrent subscription operations while maintaining p95 response time <3 seconds
- **SC-012**: 95% of subscription operations complete successfully on first attempt without errors
- **SC-013**: Churn rate calculations are accurate within 0.1% margin of error
- **SC-014**: System maintains referential integrity with 100% accuracy (no orphaned subscriptions after plan deletion attempts)

## Assumptions _(optional)_

- Customers are already authenticated and have valid JWT tokens with ROLE_USER before attempting subscription operations
- Payment processing is handled by a separate system/feature (Phase 3) - this feature focuses on subscription lifecycle management
- The "grace period" for Past Due subscriptions before automatic cancellation is 7 days (configurable)
- Billing plan prices are in USD (currency handling is out of scope for this feature)
- Feature limits are stored as key-value pairs and enforced by other system components (not enforced within this feature)
- Downgrade functionality (moving from higher tier to lower tier) is intentionally excluded from initial scope and will be addressed in a future iteration
- Subscription renewals at the end of billing cycles are handled by automated billing processes (Phase 3)
- When a plan's price is updated, existing subscriptions maintain their original price (grandfathering) - price changes only affect new subscriptions
- Customers can only have one active subscription at a time per tenant
- The system uses database-level constraints (unique indexes, foreign keys) to prevent race conditions and maintain data integrity
- Soft deletion is used for billing plans (marked as deleted but not physically removed) to maintain historical subscription data integrity

## Dependencies _(optional)_

- **Authentication & Multi-Tenancy (Phase 1)**: This feature requires the JWT authentication system and multi-tenant isolation to be fully implemented. Specifically:
  - ROLE_ADMIN and ROLE_USER roles must be defined and enforced
  - JWT tokens must include tenantId for ROLE_USER accounts
  - User authentication endpoints must be operational

- **Database Infrastructure**: Relational database (PostgreSQL) must be configured with ACID transaction support for financial data integrity

- **Future Integration with Payment Processing (Phase 3)**: While this feature manages subscription lifecycle, actual payment processing and invoice generation will be handled by Phase 3. The subscription state transitions (Active → Past Due → Canceled) will be triggered by payment success/failure events from the payment system.
