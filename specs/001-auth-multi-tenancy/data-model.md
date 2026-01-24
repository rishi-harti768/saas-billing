# Data Model: Authentication & Multi-Tenancy Security

**Feature**: 001-auth-multi-tenancy  
**Date**: 2026-01-24  
**Purpose**: Define core entities, relationships, and persistence strategy for authentication and multi-tenant isolation

## Core Entities

### 1. Tenant

**Purpose**: Represents an organization or tenant in the multi-tenant SaaS system. Each tenant is a logical grouping of users and their associated data.

**Attributes**:

- `id` (Long): Primary key, auto-generated
- `name` (String): Tenant/organization name (e.g., "Acme Corp")
- `createdDate` (LocalDateTime): Timestamp when tenant was created
- `lastModifiedDate` (LocalDateTime): Timestamp of last modification
- `active` (Boolean): Whether tenant is active (for future soft-delete support)

**Validation Rules**:

- `name`: NOT NULL, max length 255 characters
- `createdDate`: NOT NULL, auto-populated
- `lastModifiedDate`: NOT NULL, auto-updated

**Business Rules**:

- Tenant name must be unique within the system
- Tenant cannot be deleted if it has associated users (referential integrity)
- New tenants are active by default

**Persistence**:

```sql
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT true,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenants_name ON tenants(name);
CREATE INDEX idx_tenants_active ON tenants(active);
```

---

### 2. User

**Purpose**: Represents a system user (either admin or subscriber). Aggregate root for authentication and authorization.

**Attributes**:

- `id` (Long): Primary key, auto-generated
- `email` (String): User's email address (unique identifier for login)
- `password` (String): BCrypt-hashed password (60 characters)
- `role` (Role enum): User role (ROLE_ADMIN or ROLE_USER)
- `tenantId` (Long): Foreign key to Tenant (nullable for ROLE_ADMIN)
- `firstName` (String): User's first name (optional)
- `lastName` (String): User's last name (optional)
- `active` (Boolean): Whether user account is active
- `createdDate` (LocalDateTime): Account creation timestamp
- `lastModifiedDate` (LocalDateTime): Last modification timestamp
- `lastLoginDate` (LocalDateTime): Last successful login timestamp (nullable)
- `version` (Long): Optimistic locking version for concurrent updates

**Validation Rules**:

- `email`: NOT NULL, UNIQUE, valid email format, max 255 characters
- `password`: NOT NULL, BCrypt hash (60 characters)
- `role`: NOT NULL, must be ROLE_ADMIN or ROLE_USER
- `tenantId`: NULL for ROLE_ADMIN, NOT NULL for ROLE_USER
- `firstName`: Optional, max 100 characters
- `lastName`: Optional, max 100 characters
- `active`: NOT NULL, default true

**Business Rules**:

- Email must be unique across all users (enforced by database constraint)
- ROLE_ADMIN users have no tenant association (tenantId = null)
- ROLE_USER users must have a valid tenantId
- Password must be BCrypt-hashed before storage (never store plain text)
- Inactive users cannot log in
- Concurrent updates use optimistic locking (version field)

**Relationships**:

- **Many-to-One with Tenant**: User → Tenant (nullable for admins)
  - Fetch type: LAZY (tenant loaded only when needed)
  - Cascade: None (tenant lifecycle independent of users)
  - Orphan removal: No (users don't own tenants)

**Persistence**:

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(60) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ROLE_ADMIN', 'ROLE_USER')),
    tenant_id BIGINT REFERENCES tenants(id),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT true,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_date TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_user_tenant CHECK (
        (role = 'ROLE_ADMIN' AND tenant_id IS NULL) OR
        (role = 'ROLE_USER' AND tenant_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(active);
```

**Hibernate Filter** (for tenant isolation):

```java
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = Long.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
```

---

## Value Objects

### Role (Enum)

**Purpose**: Represents user roles in the system for authorization.

**Values**:

- `ROLE_ADMIN`: SaaS platform owner with full system access
- `ROLE_USER`: Subscriber with tenant-scoped access

**Usage**: Stored as VARCHAR in database, mapped to Java enum

---

## Entity Relationships

```
┌─────────────────┐
│     Tenant      │
│─────────────────│
│ id (PK)         │
│ name (UNIQUE)   │
│ active          │
│ createdDate     │
│ lastModifiedDate│
└─────────────────┘
         ▲
         │
         │ Many-to-One (nullable)
         │
┌─────────────────┐
│      User       │
│─────────────────│
│ id (PK)         │
│ email (UNIQUE)  │
│ password        │
│ role            │
│ tenantId (FK)   │ ──────────┐
│ firstName       │            │
│ lastName        │            │
│ active          │            │
│ createdDate     │            │
│ lastModifiedDate│            │
│ lastLoginDate   │            │
│ version         │            │
└─────────────────┘            │
                               │
                               ▼
                        Tenant Isolation
                        (Hibernate Filter)
```

**Relationship Details**:

- **User → Tenant**: Many users can belong to one tenant
- **Cardinality**: 0..1 to Many (tenant is optional for admins)
- **Fetch Strategy**: LAZY (avoid N+1 queries)
- **Cascade**: None (independent lifecycles)

---

## Data Access Patterns

### 1. User Registration

**Flow**:

1. Validate RegisterRequest DTO (email format, password length)
2. Check if email already exists (UserRepository.findByEmail)
3. If ROLE_USER, validate or create Tenant
4. Hash password with BCrypt (work factor 12)
5. Create User entity with hashed password
6. Save User (database enforces UNIQUE constraint on email)
7. Handle DataIntegrityViolationException → DuplicateEmailException

**Queries**:

```sql
-- Check email existence
SELECT id FROM users WHERE email = ?;

-- Create tenant (if new)
INSERT INTO tenants (name, active, created_date, last_modified_date)
VALUES (?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Create user
INSERT INTO users (email, password, role, tenant_id, first_name, last_name, active, created_date, last_modified_date, version)
VALUES (?, ?, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
```

---

### 2. User Login

**Flow**:

1. Validate LoginRequest DTO (email, password not blank)
2. Find user by email (UserRepository.findByEmail)
3. If user not found → InvalidCredentialsException
4. If user inactive → AccountDisabledException
5. Verify password with BCrypt (PasswordEncoder.matches)
6. If password incorrect → InvalidCredentialsException
7. Update lastLoginDate
8. Generate JWT token with claims (userId, email, role, tenantId)
9. Return AuthResponse with token

**Queries**:

```sql
-- Find user by email (with tenant for ROLE_USER)
SELECT u.id, u.email, u.password, u.role, u.tenant_id, u.active, u.version
FROM users u
WHERE u.email = ? AND u.active = true;

-- Update last login
UPDATE users
SET last_login_date = CURRENT_TIMESTAMP, last_modified_date = CURRENT_TIMESTAMP, version = version + 1
WHERE id = ? AND version = ?;
```

---

### 3. Tenant-Filtered Queries (Future Endpoints)

**Flow**:

1. JWT filter extracts tenantId from token
2. TenantContext.setTenantId(tenantId)
3. Hibernate filter automatically applied to all queries
4. Repository methods execute with WHERE tenant_id = ? added

**Example**:

```sql
-- Original query (in code)
SELECT * FROM subscriptions WHERE user_id = ?;

-- Actual SQL (with Hibernate filter)
SELECT * FROM subscriptions WHERE user_id = ? AND tenant_id = ?;
```

---

## State Transitions

### User Account States

```
┌─────────────┐
│  CREATED    │ (active = true, lastLoginDate = null)
└─────────────┘
      │
      │ First login
      ▼
┌─────────────┐
│   ACTIVE    │ (active = true, lastLoginDate != null)
└─────────────┘
      │
      │ Admin deactivates
      ▼
┌─────────────┐
│  INACTIVE   │ (active = false)
└─────────────┘
      │
      │ Admin reactivates
      ▼
┌─────────────┐
│   ACTIVE    │ (active = true)
└─────────────┘
```

**Transitions**:

- **CREATED → ACTIVE**: First successful login updates `lastLoginDate`
- **ACTIVE → INACTIVE**: Admin sets `active = false` (future feature)
- **INACTIVE → ACTIVE**: Admin sets `active = true` (future feature)

**Business Rules**:

- Inactive users cannot log in (checked in AuthenticationService)
- Deactivation does not delete user data (soft delete)
- Reactivation preserves all user data and history

---

## Indexing Strategy

### Performance Indexes

1. **users.email** (UNIQUE): Primary lookup for login (most frequent query)
2. **users.tenant_id**: Tenant-filtered queries (all ROLE_USER operations)
3. **users.role**: Role-based queries (admin user management)
4. **users.active**: Filter inactive users (login checks)
5. **tenants.name** (UNIQUE): Tenant lookup during registration

### Index Rationale

- **Email index**: Login is most frequent operation, email is primary key for authentication
- **Tenant index**: All user queries will be filtered by tenantId (multi-tenancy)
- **Role index**: Admin dashboards will query users by role
- **Active index**: Filtering inactive users is common (login, user lists)

---

## Data Integrity Constraints

### Database-Level Constraints

1. **PRIMARY KEY**: Auto-generated IDs for all entities
2. **UNIQUE**: Email (users), Name (tenants)
3. **NOT NULL**: Email, password, role, active, timestamps
4. **FOREIGN KEY**: User.tenantId → Tenant.id (with ON DELETE RESTRICT)
5. **CHECK**: Role must be ROLE_ADMIN or ROLE_USER
6. **CHECK**: ROLE_ADMIN must have tenantId = NULL, ROLE_USER must have tenantId NOT NULL

### Application-Level Validation

1. **Email Format**: Bean Validation @Email annotation
2. **Password Length**: @Size(min=8, max=100)
3. **BCrypt Hash**: Always 60 characters (enforced by PasswordEncoder)
4. **Optimistic Locking**: @Version for concurrent updates

---

## Audit Trail

### Audit Fields

All entities include audit fields for compliance and debugging:

- `createdDate`: When entity was created (immutable)
- `lastModifiedDate`: When entity was last updated (auto-updated)
- `version`: Optimistic locking version (prevents lost updates)

### Future Enhancements

- `createdBy`: User who created the entity (requires security context)
- `lastModifiedBy`: User who last modified the entity
- Separate audit log table for sensitive operations (login attempts, password changes)

---

## Migration Scripts

### V001\_\_create_tenant_table.sql

```sql
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT true,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenants_name ON tenants(name);
CREATE INDEX idx_tenants_active ON tenants(active);

COMMENT ON TABLE tenants IS 'Organizations/tenants in the multi-tenant SaaS system';
COMMENT ON COLUMN tenants.id IS 'Primary key';
COMMENT ON COLUMN tenants.name IS 'Tenant/organization name (unique)';
COMMENT ON COLUMN tenants.active IS 'Whether tenant is active (soft delete support)';
```

### V002\_\_create_user_table.sql

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(60) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ROLE_ADMIN', 'ROLE_USER')),
    tenant_id BIGINT REFERENCES tenants(id) ON DELETE RESTRICT,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT true,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_date TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_user_tenant CHECK (
        (role = 'ROLE_ADMIN' AND tenant_id IS NULL) OR
        (role = 'ROLE_USER' AND tenant_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(active);

COMMENT ON TABLE users IS 'System users (admins and subscribers)';
COMMENT ON COLUMN users.email IS 'User email (unique login identifier)';
COMMENT ON COLUMN users.password IS 'BCrypt-hashed password (60 characters)';
COMMENT ON COLUMN users.role IS 'User role: ROLE_ADMIN or ROLE_USER';
COMMENT ON COLUMN users.tenant_id IS 'Tenant association (NULL for admins, NOT NULL for users)';
COMMENT ON COLUMN users.version IS 'Optimistic locking version';
COMMENT ON CONSTRAINT chk_user_tenant ON users IS 'Admins have no tenant, users must have tenant';
```

---

## Testing Strategy

### Unit Tests (Repository Layer)

1. **UserRepository**:
   - `findByEmail()`: Returns user when email exists, empty when not found
   - `save()`: Creates new user with auto-generated ID
   - `save()`: Throws exception when email already exists (UNIQUE constraint)
   - Optimistic locking: Concurrent updates increment version correctly

2. **TenantRepository**:
   - `findByName()`: Returns tenant when name exists
   - `save()`: Creates new tenant with auto-generated ID
   - `save()`: Throws exception when name already exists (UNIQUE constraint)

### Integration Tests (Data Layer)

1. **Multi-Tenant Isolation**:
   - Hibernate filter correctly adds WHERE tenant_id = ? to queries
   - Cross-tenant data access is blocked
   - Admin users (tenantId = null) bypass tenant filter

2. **Constraint Validation**:
   - Duplicate email registration fails with DataIntegrityViolationException
   - ROLE_USER without tenantId fails CHECK constraint
   - ROLE_ADMIN with tenantId fails CHECK constraint

3. **Audit Trail**:
   - `createdDate` auto-populated on insert
   - `lastModifiedDate` auto-updated on update
   - `version` increments on each update

---

## Performance Considerations

### Query Optimization

- **Email lookup**: O(log n) with UNIQUE index (B-tree)
- **Tenant filtering**: O(log n) with tenant_id index
- **Lazy loading**: Tenant entity loaded only when accessed (avoid N+1)

### Expected Data Volume

- **Users**: 10,000+ (indexed queries remain fast)
- **Tenants**: 1,000+ (small table, fully cacheable)
- **Login queries**: 100+ req/s (email index ensures fast lookup)

### Scalability

- **Horizontal scaling**: Stateless authentication supports multiple app instances
- **Database connection pooling**: HikariCP with appropriate pool size
- **Read replicas**: Login queries can use read replicas (eventual consistency acceptable)

---

## Security Considerations

### Data Protection

- **Password hashing**: BCrypt with work factor 12 (never store plain text)
- **Tenant isolation**: Hibernate filters prevent cross-tenant data leaks
- **Optimistic locking**: Prevents lost updates in concurrent scenarios

### Compliance

- **GDPR**: User data can be deleted (future feature)
- **Audit trail**: Created/modified timestamps for compliance
- **Data retention**: No automatic deletion (manual admin action required)

---

## Summary

This data model provides:

- ✅ **Secure authentication**: BCrypt password hashing, email-based login
- ✅ **Multi-tenant isolation**: Tenant entity with Hibernate filter enforcement
- ✅ **Role-based access**: ROLE_ADMIN and ROLE_USER with database constraints
- ✅ **Data integrity**: UNIQUE constraints, CHECK constraints, foreign keys
- ✅ **Audit trail**: Created/modified timestamps, optimistic locking
- ✅ **Performance**: Strategic indexing for fast queries
- ✅ **Scalability**: Stateless design, horizontal scaling support
