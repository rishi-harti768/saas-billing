# Billing API - Multi-Tenant SaaS Authentication System

A production-ready Spring Boot 3.x application implementing JWT-based authentication with multi-tenant data isolation for a SaaS billing platform.

## 🚀 Features

### Core Functionality

- ✅ **User Registration & Login** - JWT-based stateless authentication
- ✅ **Multi-Tenancy** - Complete tenant data isolation using Hibernate filters
- ✅ **Role-Based Access Control** - ROLE_ADMIN and ROLE_USER with different permissions
- ✅ **Secure Password Management** - BCrypt hashing with work factor 12
- ✅ **Input Validation** - Comprehensive Bean Validation with user-friendly error messages
- ✅ **API Documentation** - Interactive Swagger UI for API exploration
- ✅ **Health Monitoring** - Spring Boot Actuator endpoints

### Security Features

- JWT token generation and validation (JJWT 0.12.3)
- BCrypt password hashing (work factor 12)
- Stateless authentication (no server-side sessions)
- Multi-tenant data isolation (Hibernate filters + AOP)
- Generic error messages (security best practice)
- HTTPS-ready configuration

### Technical Stack

- **Framework**: Spring Boot 3.x
- **Java Version**: 17+
- **Database**: PostgreSQL
- **Security**: Spring Security 6.x + JJWT
- **Migration**: Flyway
- **Documentation**: Springdoc OpenAPI 3.0
- **Testing**: JUnit 5, Mockito, AssertJ, MockMvc

## 📋 Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher

## 🛠️ Setup

### 1. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE billing_db;
CREATE USER billing_user WITH PASSWORD 'billing_pass';
GRANT ALL PRIVILEGES ON DATABASE billing_db TO billing_user;
```

### 2. Environment Variables

Set the following environment variables (or use defaults from `application.yml`):

```bash
# Database Configuration
export DB_USERNAME=billing_user
export DB_PASSWORD=billing_pass

# JWT Secret (MUST be changed in production!)
export JWT_SECRET=your-secure-random-secret-key-at-least-32-characters-long
```

**⚠️ IMPORTANT**: Never use the default JWT secret in production! Generate a secure secret:

```bash
# Linux/Mac
openssl rand -base64 32

# Windows PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

### 3. Build and Run

```bash
# Build the project
./mvnw clean package

# Run the application
./mvnw spring-boot:run

# Or run the JAR
java -jar target/billing-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## 📚 API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

### Quick API Reference

#### Register Admin User

```bash
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "securePassword123",
  "role": "ROLE_ADMIN",
  "firstName": "John",
  "lastName": "Doe"
}
```

#### Register Subscriber User (with Tenant)

```bash
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@acme.com",
  "password": "securePassword123",
  "role": "ROLE_USER",
  "tenantName": "Acme Corp",
  "firstName": "Jane",
  "lastName": "Smith"
}
```

#### Login

```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@example.com",
  "password": "securePassword123"
}
```

**Response:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 86400000
}
```

#### Using JWT Token

```bash
GET /api/v1/protected-endpoint
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## 🏗️ Architecture

### Layered Architecture

```
┌─────────────────────────────────────┐
│         Controller Layer            │  ← REST endpoints
├─────────────────────────────────────┤
│          Service Layer              │  ← Business logic
├─────────────────────────────────────┤
│        Repository Layer             │  ← Data access
├─────────────────────────────────────┤
│          Entity Layer               │  ← JPA entities
└─────────────────────────────────────┘
```

### Multi-Tenancy Flow

```
Request → JWT Filter → Extract tenantId → TenantContext
                                              ↓
                                    TenantFilterAspect
                                              ↓
                                    Hibernate Filter (WHERE tenant_id = ?)
                                              ↓
                                         Repository
```

### Security Flow

```
Login Request → AuthenticationService
                      ↓
              Verify Password (BCrypt)
                      ↓
              Generate JWT Token
                      ↓
              Return Token to Client
                      ↓
Client stores token → Subsequent requests include token
                      ↓
              JwtAuthenticationFilter validates token
                      ↓
              SecurityContext populated
                      ↓
              Request processed
```

## 🧪 Testing

### Run All Tests

```bash
./mvnw test
```

### Run Specific Test Class

```bash
./mvnw test -Dtest=UserServiceTest
```

### Test Coverage

The project includes comprehensive test coverage:

- **Unit Tests**: Service layer, Security components
- **Integration Tests**: Controller endpoints, Repository operations
- **Security Tests**: Password handling, JWT validation

## 📊 Monitoring

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

### Application Info

```bash
curl http://localhost:8080/actuator/info
```

## 🔒 Security Considerations

### Password Security

- ✅ BCrypt hashing with work factor 12
- ✅ Minimum password length: 8 characters
- ✅ Passwords never logged or exposed in error messages
- ✅ Timing attack resistance

### JWT Security

- ✅ HS256 algorithm with secure secret key
- ✅ Token expiration (24 hours default)
- ✅ Claims validation on every request
- ✅ Stateless design (no server-side session storage)

### Multi-Tenant Security

- ✅ Automatic tenant filtering on all queries
- ✅ Cross-tenant access prevention
- ✅ Admin users bypass tenant filter
- ✅ Thread-safe tenant context

## 🚀 Production Deployment

### Pre-Deployment Checklist

- [ ] Change JWT secret to a secure random value
- [ ] Update database credentials
- [ ] Enable HTTPS/TLS
- [ ] Configure CORS for your frontend domain
- [ ] Set appropriate logging levels
- [ ] Configure database connection pooling
- [ ] Set up database backups
- [ ] Configure monitoring and alerting
- [ ] Review and update Actuator endpoint exposure
- [ ] Perform security audit
- [ ] Load testing

### Environment-Specific Configuration

Create `application-prod.yml` for production:

```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10

logging:
  level:
    root: WARN
    org.gb.billing: INFO

management:
  endpoints:
    web:
      exposure:
        include: health,metrics
```

## 📝 Configuration Reference

### JWT Configuration

- `jwt.secret`: Secret key for JWT signing (min 32 characters)
- `jwt.expiration`: Token expiration in milliseconds (default: 86400000 = 24 hours)

### Database Configuration

- `spring.datasource.url`: PostgreSQL connection URL
- `spring.datasource.username`: Database username
- `spring.datasource.password`: Database password

### Logging Configuration

- `logging.level.root`: Root logging level
- `logging.level.org.gb.billing`: Application logging level

## 🤝 Contributing

1. Follow the existing code style and architecture
2. Write tests for all new features (TDD approach)
3. Update documentation for API changes
4. Ensure all tests pass before submitting

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

For issues, questions, or contributions, please contact the development team.

---

**Built with ❤️ using Spring Boot 3.x**
