# 🚀 Quick Start Guide - SaaS Billing Platform

**Last Updated:** January 31, 2026  
**Application:** Spring Boot 3.2.2 Billing Platform  
**Time to Run:** ~5 minutes

---

## 📋 Prerequisites

Before starting, ensure you have:

| Requirement    | Version      | Check Command    |
| -------------- | ------------ | ---------------- |
| **Java**       | 17 or higher | `java -version`  |
| **Maven**      | 3.6+         | `mvn -version`   |
| **PostgreSQL** | 12+          | `psql --version` |
| **Git**        | Any          | `git --version`  |

---

## 🗄️ Step 1: Database Setup

### Option A: Using PostgreSQL CLI

```bash
# 1. Connect to PostgreSQL
psql -U postgres

# 2. Create database
CREATE DATABASE billing_db;

# 3. Create user (optional)
CREATE USER billing_user WITH PASSWORD 'your_password';

# 4. Grant privileges
GRANT ALL PRIVILEGES ON DATABASE billing_db TO billing_user;

# 5. Exit
\q
```

### Option B: Using pgAdmin

1. Open pgAdmin
2. Right-click **Databases** → **Create** → **Database**
3. Name: `billing_db`
4. Save

---

## ⚙️ Step 2: Configure Application

### Update `application.yml`

Navigate to `src/main/resources/application.yml` and update:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/billing_db
    username: postgres # Change if using custom user
    password: your_password # YOUR POSTGRES PASSWORD

  jpa:
    hibernate:
      ddl-auto: validate # Keep as 'validate' (Flyway manages schema)
```

### Environment Variables (Optional)

Alternatively, set environment variables:

```bash
# Windows PowerShell
$env:DB_URL="jdbc:postgresql://localhost:5432/billing_db"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your-secret-key-min-32-characters-long"

# Linux/Mac
export DB_URL="jdbc:postgresql://localhost:5432/billing_db"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_password"
export JWT_SECRET="your-secret-key-min-32-characters-long"
```

---

## 🔨 Step 3: Build the Application

```bash
# Navigate to project root
cd c:\Users\rishi\OneDrive\Desktop\spring\billing

# Clean and compile
mvn clean compile

# Run tests (optional)
mvn test

# Package JAR (optional)
mvn clean package -DskipTests
```

**Expected Output:**

```
[INFO] BUILD SUCCESS
[INFO] Total time: 8-10 seconds
```

---

## ▶️ Step 4: Run the Application

### Method 1: Using Maven (Recommended for Development)

```bash
mvn spring-boot:run
```

### Method 2: Using JAR (Production-like)

```bash
# Build JAR first
mvn clean package -DskipTests

# Run JAR
java -jar target/billing-0.0.1-SNAPSHOT.jar
```

### Method 3: Using IDE

**IntelliJ IDEA / Eclipse:**

1. Open project
2. Navigate to `src/main/java/org/gb/billing/BillingApplication.java`
3. Right-click → **Run 'BillingApplication'**

---

## ✅ Step 5: Verify Application is Running

### Check Logs

Look for these lines in console:

```
Started BillingApplication in X.XXX seconds
Tomcat started on port 8080
```

### Test Health Endpoint

```bash
# PowerShell
Invoke-RestMethod -Uri "http://localhost:8080/actuator/health"

# cURL (if installed)
curl http://localhost:8080/actuator/health
```

**Expected Response:**

```json
{
  "status": "UP"
}
```

### Access Swagger UI

Open browser and navigate to:

```
http://localhost:8080/swagger-ui.html
```

You should see the interactive API documentation.

---

## 🔐 Step 6: Create Your First User

### Using Swagger UI

1. Go to **Auth Controller** section
2. Expand **POST /api/v1/auth/register**
3. Click **Try it out**
4. Use this request body:

```json
{
  "email": "admin@example.com",
  "password": "Admin@123",
  "firstName": "Admin",
  "lastName": "User",
  "role": "ADMIN"
}
```

5. Click **Execute**
6. Copy the JWT token from response

### Using PowerShell

```powershell
$registerBody = @{
    email = "admin@example.com"
    password = "Admin@123"
    firstName = "Admin"
    lastName = "User"
    role = "ADMIN"
} | ConvertTo-Json

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/register" `
    -Method POST `
    -ContentType "application/json" `
    -Body $registerBody

# Save token
$token = $response.token
Write-Host "JWT Token: $token"
```

---

## 🧪 Step 7: Test Protected Endpoints

### Get All Plans (Protected)

```powershell
# Use token from previous step
$headers = @{
    "Authorization" = "Bearer $token"
}

$plans = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/plans" `
    -Method GET `
    -Headers $headers

Write-Host "Plans: $($plans | ConvertTo-Json -Depth 3)"
```

### Create a Plan

```powershell
$planBody = @{
    name = "Starter Plan"
    description = "Perfect for small businesses"
    price = 29.99
    billingCycle = "MONTHLY"
    features = @("5 Users", "10GB Storage", "Email Support")
    isActive = $true
} | ConvertTo-Json

$newPlan = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/plans" `
    -Method POST `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $planBody

Write-Host "Created Plan: $($newPlan | ConvertTo-Json)"
```

---

## 📁 Step 8: Test File Upload

### Upload a File

```powershell
# Prepare a test file (create a simple text file)
"Test invoice content" | Out-File -FilePath ".\test-invoice.txt"

# Upload file
$uploadHeaders = @{
    "Authorization" = "Bearer $token"
}

$form = @{
    file = Get-Item -Path ".\test-invoice.txt"
    entityType = "INVOICE"
    entityId = "test-invoice-123"
    description = "Test invoice document"
}

$uploadResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/files/upload" `
    -Method POST `
    -Headers $uploadHeaders `
    -Form $form

Write-Host "Upload Response: $($uploadResponse | ConvertTo-Json)"
```

### Check Uploaded Files

```powershell
$myFiles = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/files/my-files" `
    -Method GET `
    -Headers $headers

Write-Host "My Files: $($myFiles | ConvertTo-Json)"
```

---

## 🎯 Common Workflows

### Workflow 1: Create Subscription

```powershell
# 1. Get available plans
$plans = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/plans" `
    -Method GET -Headers $headers

$planId = $plans[0].id

# 2. Create subscription
$subscriptionBody = @{
    planId = $planId
    startDate = (Get-Date).ToString("yyyy-MM-dd")
} | ConvertTo-Json

$subscription = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/subscriptions" `
    -Method POST `
    -Headers $headers `
    -ContentType "application/json" `
    -Body $subscriptionBody

Write-Host "Created Subscription: $($subscription | ConvertTo-Json)"
```

### Workflow 2: View Analytics

```powershell
# Get MRR (Monthly Recurring Revenue)
$mrr = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/analytics/mrr" `
    -Method GET -Headers $headers

Write-Host "MRR: `$$($mrr.value)"

# Get subscription growth
$growth = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/analytics/subscription-growth?days=30" `
    -Method GET -Headers $headers

Write-Host "Growth: $($growth | ConvertTo-Json)"
```

---

## 🐛 Troubleshooting

### Issue 1: Application Won't Start

**Error:** `Unable to connect to database`

**Solution:**

1. Check PostgreSQL is running:

   ```bash
   # Windows
   Get-Service postgresql*

   # If not running, start it
   Start-Service postgresql-x64-14  # Adjust version number
   ```

2. Verify credentials in `application.yml`
3. Test database connection:
   ```bash
   psql -U postgres -d billing_db
   ```

---

### Issue 2: Flyway Migration Fails

**Error:** `Flyway migration validation failed`

**Solution:**

1. Drop and recreate database:
   ```sql
   DROP DATABASE billing_db;
   CREATE DATABASE billing_db;
   ```
2. Restart application (Flyway will create tables)

---

### Issue 3: Port 8080 Already in Use

**Error:** `Port 8080 is already in use`

**Solution:**

**Option A:** Stop conflicting process

```powershell
# Find process using port 8080
netstat -ano | findstr :8080

# Kill process (replace PID)
taskkill /PID <PID> /F
```

**Option B:** Change port in `application.yml`

```yaml
server:
  port: 8081 # Use different port
```

---

### Issue 4: JWT Token Invalid

**Error:** `401 Unauthorized`

**Solution:**

1. Check token hasn't expired (default: 24 hours)
2. Re-login to get fresh token
3. Verify JWT secret is configured correctly
4. Ensure token is prefixed with "Bearer " in Authorization header

---

### Issue 5: File Upload Fails

**Error:** `Could not create upload directory`

**Solution:**

1. Check write permissions on `./uploads` folder
2. Create directory manually:
   ```powershell
   New-Item -Path ".\uploads" -ItemType Directory -Force
   ```
3. Verify `file.upload.dir` in `application.yml`

---

## 📊 Application Endpoints Summary

| Category          | Method | Endpoint                            | Auth Required |
| ----------------- | ------ | ----------------------------------- | ------------- |
| **Auth**          | POST   | `/api/v1/auth/register`             | ❌            |
|                   | POST   | `/api/v1/auth/login`                | ❌            |
| **Plans**         | GET    | `/api/v1/plans`                     | ✅            |
|                   | POST   | `/api/v1/plans`                     | ✅ Admin      |
|                   | PUT    | `/api/v1/plans/{id}`                | ✅ Admin      |
|                   | DELETE | `/api/v1/plans/{id}`                | ✅ Admin      |
| **Subscriptions** | GET    | `/api/v1/subscriptions`             | ✅            |
|                   | POST   | `/api/v1/subscriptions`             | ✅            |
|                   | PUT    | `/api/v1/subscriptions/{id}`        | ✅            |
|                   | DELETE | `/api/v1/subscriptions/{id}`        | ✅            |
| **Files**         | POST   | `/api/v1/files/upload`              | ✅            |
|                   | GET    | `/api/v1/files/my-files`            | ✅            |
|                   | GET    | `/api/v1/files/download/{filename}` | ✅            |
|                   | DELETE | `/api/v1/files/{filename}`          | ✅ Admin      |
| **Analytics**     | GET    | `/api/v1/analytics/mrr`             | ✅            |
|                   | GET    | `/api/v1/analytics/arr`             | ✅            |
|                   | GET    | `/api/v1/analytics/churn-rate`      | ✅            |

---

## 🎓 Next Steps

### For Development

1. ✅ Explore Swagger UI: http://localhost:8080/swagger-ui.html
2. ✅ Test all endpoints with different scenarios
3. ✅ Check database tables in pgAdmin
4. ✅ Review logs for caching behavior
5. ✅ Test rate limiting (make 100+ requests)

### For Demo Video

1. ✅ Follow script in `SUBMISSION.md`
2. ✅ Demonstrate authentication flow
3. ✅ Show CRUD operations
4. ✅ Highlight advanced features (caching, rate limiting)
5. ✅ Display file upload functionality

### For Submission

1. ✅ Complete pre-submission checklist in `CHECKLIST.md`
2. ✅ Record demo video (5-10 minutes)
3. ✅ Test viva questions from `SUBMISSION.md`
4. ✅ Verify all endpoints work
5. ✅ Submit project!

---

## 📞 Quick Reference

### Important URLs

- **Application:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API Docs:** http://localhost:8080/api-docs
- **Health Check:** http://localhost:8080/actuator/health

### Important Files

- **Configuration:** `src/main/resources/application.yml`
- **Main Class:** `src/main/java/org/gb/billing/BillingApplication.java`
- **Migrations:** `src/main/resources/db/migration/`
- **Upload Directory:** `./uploads/`

### Important Commands

```bash
# Start application
mvn spring-boot:run

# Run tests
mvn test

# Build JAR
mvn clean package

# Check health
curl http://localhost:8080/actuator/health
```

---

## 🎉 Success Checklist

After completing this guide, you should have:

- ✅ PostgreSQL database running
- ✅ Application started successfully
- ✅ Swagger UI accessible
- ✅ Test user created
- ✅ JWT token obtained
- ✅ Protected endpoints tested
- ✅ File upload working
- ✅ Database populated with test data

**You're ready to record your demo video and submit!** 🚀

---

**Need Help?** Check:

- `README.md` - Project overview
- `SUBMISSION.md` - Complete submission guide
- `CHECKLIST.md` - Pre-submission verification
- `API_TESTING_GUIDE.md` - Detailed API testing

**Good Luck! 🎓**
