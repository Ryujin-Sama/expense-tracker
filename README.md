# Expense Tracker Backend

A robust, enterprise-grade backend API for managing personal and business expenses. Built with Spring Boot 4.1.0, this application provides secure user authentication, role-based access control, and comprehensive expense management capabilities.

## 📋 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [Database Migrations](#database-migrations)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Security](#security)
- [Development](#development)

## 🎯 Overview

The Expense Tracker Backend is a full-featured REST API designed to provide secure, scalable expense management. It implements industry-standard authentication using JWT tokens with refresh token rotation, role-based authorization, and database-backed persistence.

**Current Version:** 0.1.0 (Sprint 1: Foundation and Security)

## 🛠 Tech Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| **Framework** | Spring Boot | 4.1.0 |
| **Language** | Java | 17 |
| **ORM** | Spring Data JPA | Latest (via Spring Boot) |
| **Security** | Spring Security | Latest (via Spring Boot) |
| **Authentication** | JWT (JJWT) | 0.13.0 |
| **Database** | MySQL | 8.0+ |
| **Migrations** | Flyway | Latest (via Spring Boot) |
| **API Docs** | SpringDoc OpenAPI | 3.1.0 |
| **Build Tool** | Maven | 3.8+ |
| **Containerization** | Docker & Docker Compose | Latest |

## ✨ Features

### Security & Authentication
- ✅ **JWT-based Authentication** - Stateless, token-based user authentication
- ✅ **Refresh Token Rotation** - Long-lived refresh tokens with automatic rotation
- ✅ **Password Encryption** - BCrypt password hashing with salt
- ✅ **Role-Based Access Control (RBAC)** - User and Admin roles with permission management
- ✅ **CORS Support** - Configurable cross-origin resource sharing

### User Management
- ✅ **User Registration** - Secure account creation with validation
- ✅ **User Authentication** - Login with email and password
- ✅ **Refresh Token Management** - Automatic token renewal
- ✅ **Role Assignment** - User and Admin role support

### Database
- ✅ **Automated Schema Migrations** - Flyway-based version control for database changes
- ✅ **Data Validation** - Bean validation with JSR-380
- ✅ **Relational Data Integrity** - Foreign keys and constraints

## 📋 Prerequisites

Before setting up the project, ensure you have:

- **Java Development Kit (JDK)** 17 or higher
- **Maven** 3.8.0 or higher
- **MySQL** 8.0 or higher (or via Docker)
- **Docker & Docker Compose** (for containerized setup)
- **Git** (for version control)

### Verify Installation
```bash
java -version          # Should show Java 17+
mvn -version          # Should show Maven 3.8+
mysql --version       # Should show MySQL 8.0+
docker --version      # Should show Docker version
```

## 🚀 Installation & Setup

### Option 1: Local Development Setup

#### 1. Clone the Repository
```bash
git clone <repository-url>
cd expense-tracker
```

#### 2. Set Up MySQL Database
Create a database and user for the application:

```sql
CREATE DATABASE expense_tracker_db;
CREATE USER 'expense_user'@'localhost' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON expense_tracker_db.* TO 'expense_user'@'localhost';
FLUSH PRIVILEGES;
```

#### 3. Configure Environment Variables
Create a `.env` file in the project root:

```bash
# MySQL Configuration
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_DATABASE=expense_tracker_db
MYSQL_USER=expense_user
MYSQL_PASSWORD=your_secure_password

# JWT Configuration
JWT_SECRET=your_very_long_secret_key_at_least_256_bits_recommended
JWT_ACCESS_EXPIRATION=15m
JWT_REFRESH_EXPIRATION=7d

# Server Configuration
SERVER_PORT=8080

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

#### 4. Install Dependencies
```bash
mvn clean install
```

### Option 2: Docker Setup (Recommended)

#### 1. Build and Run with Docker Compose
```bash
docker-compose up -d
```

This will:
- Start a MySQL container
- Build and run the Spring Boot application
- Apply database migrations automatically
- Expose the API on `http://localhost:8080`

#### 2. Verify Services are Running
```bash
docker-compose ps
docker-compose logs -f expense-tracker
```

#### 3. Stop Services
```bash
docker-compose down
```

## ⚙️ Configuration

### Application Configuration
The application uses Spring Profiles for environment-specific configuration:

**Profiles Available:**
- `dev` - Development environment (default)
- `prod` - Production environment

**Key Configuration Files:**
- `src/main/resources/application.yml` - Base configuration
- `src/main/resources/application-dev.yml` - Development overrides
- `src/main/resources/application-prod.yml` - Production overrides

### JWT Configuration
```yaml
jwt:
  access:
    expiration: 15m  # Access token validity period
  refresh:
    expiration: 7d   # Refresh token validity period
  secret: ${JWT_SECRET}  # Set via environment variable
```

### Database Configuration
```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DATABASE}
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate  # Never auto-generate schema; Flyway manages it
    show-sql: false
```

## 🏃 Running the Application

### Using Maven (Local)
```bash
# Development mode
mvn spring-boot:run

# Production mode
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=prod"
```

### Using Docker
```bash
docker-compose up
```

### Using Built JAR
```bash
# Build the application
mvn clean package

# Run the JAR
java -jar target/expense-tracker-backend-0.1.0-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

## 📊 Database Migrations

Flyway automatically manages database schema versions. Migration files are located in:
```
src/main/resources/db/migration/
```

**Current Migrations:**
- `V1__init_Users_Table.sql` - Initial users table with role support
- `V2__Init_Refresh_Tokens_Table.sql` - Refresh tokens table for token rotation

### Adding New Migrations
1. Create a new file: `V3__your_migration_name.sql`
2. Write your SQL migration
3. Restart the application (Flyway will auto-apply)

**Migration Naming Convention:**
- Prefix: `V` (version indicator)
- Version: Sequential number (V1, V2, V3)
- Separator: Double underscore (`__`)
- Description: Snake_case description

## 📚 API Documentation

### OpenAPI/Swagger Documentation
Once the application is running, access the interactive API documentation:

```
http://localhost:8080/swagger-ui.html
```

Or view the OpenAPI specification:
```
http://localhost:8080/v3/api-docs
```

### Authentication Endpoints

#### Register User
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePassword123!"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePassword123!"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

#### Refresh Token
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Using Access Token
Include the access token in all protected API requests:

```http
GET /api/expenses
Authorization: Bearer <your_access_token>
```

## 📁 Project Structure

```
expense-tracker/
├── src/
│   ├── main/
│   │   ├── java/com/expenseTracker/
│   │   │   ├── ExpenseTrackerApplication.java      # Spring Boot entry point
│   │   │   ├── config/
│   │   │   │   └── PasswordEncoderConfig.java      # Security configuration
│   │   │   ├── model/
│   │   │   │   ├── dto/                             # Data Transfer Objects
│   │   │   │   │   ├── AuthRequest.java
│   │   │   │   │   ├── AuthResponse.java
│   │   │   │   │   └── RegisterRequest.java
│   │   │   │   └── entity/                          # JPA Entities
│   │   │   │       ├── User.java
│   │   │   │       ├── Role.java
│   │   │   │       └── RefreshToken.java
│   │   │   ├── repository/                          # Data access layer
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── RefreshTokenRepository.java
│   │   │   ├── security/                            # Security & JWT
│   │   │   │   ├── JwtUtil.java                     # JWT token generation/validation
│   │   │   │   ├── CustomUserDetailsService.java    # User loading service
│   │   │   │   └── UserPrincipal.java               # Principal implementation
│   │   │   └── controller/                          # REST controllers (future)
│   │   └── resources/
│   │       ├── application.yml                      # Base configuration
│   │       └── db/migration/                        # Flyway migrations
│   │           ├── V1__init_Users_Table.sql
│   │           └── V2__Init_Refresh_Tokens_Table.sql
│   └── test/
│       └── java/com/expenseTracker/
│           ├── repository/
│           │   └── AuthRepositoryIT.java            # Integration tests
│           └── security/
│               ├── JwtUtilTest.java                 # JWT tests
│               └── CustomUserDetailsServiceTest.java
├── pom.xml                                          # Maven configuration
├── docker-compose.yaml                             # Docker setup
└── README.md                                        # This file
```

## 🧪 Testing

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=JwtUtilTest
```

### Run Integration Tests Only
```bash
mvn verify -Dtest=*IT
```

### View Test Reports
Test results are available in:
```
target/surefire-reports/
```

### Test Coverage
Tests are included for:
- ✅ JWT token generation and validation
- ✅ User authentication and authorization
- ✅ Custom user details service
- ✅ Database repository operations

## 🔒 Security

### Authentication Flow
1. User registers with email and password
2. Password is hashed using BCrypt
3. User logs in with credentials
4. Server issues JWT access token (15m expiry) and refresh token (7d expiry)
5. Client uses access token for API requests
6. Access token expires → Client uses refresh token to get new token
7. Refresh token expires → User must log in again

### Best Practices Implemented
- ✅ **Password Hashing** - BCrypt with automatic salt generation
- ✅ **JWT Security** - HS256 signing with strong secret key
- ✅ **Token Expiration** - Short-lived access tokens, longer-lived refresh tokens
- ✅ **CORS Security** - Configurable allowed origins
- ✅ **Input Validation** - Bean validation on all DTOs
- ✅ **Role-Based Access** - User and Admin role enforcement

### Security Headers
The application includes standard Spring Security headers:
- X-Content-Type-Options: nosniff
- X-Frame-Options: DENY
- X-XSS-Protection: 1; mode=block

## 👨‍💻 Development

### Build the Project
```bash
mvn clean install
```

### Run in Development Mode
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Create a New Migration
1. Create file: `src/main/resources/db/migration/V3__description.sql`
2. Write SQL changes
3. Flyway will auto-apply on startup

### Code Style
- Follow Spring Framework conventions
- Use meaningful variable names
- Add JavaDoc for public methods
- Write unit tests for business logic

### Adding a New Feature
1. Create entity in `model/entity/`
2. Create repository in `repository/`
3. Create service if needed in `service/`
4. Create controller in `controller/`
5. Add database migration if needed
6. Write tests

### Debugging
Run with debug logging enabled:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--logging.level.com.expenseTracker=DEBUG"
```

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 Support

For issues, questions, or suggestions:
- Open a GitHub Issue
- Check existing documentation
- Review test cases for usage examples

---

**Happy Tracking! 🎯**
