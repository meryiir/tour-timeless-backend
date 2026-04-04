# Tourisme Backend API

A complete REST API backend for a tourism platform built with Spring Boot and PostgreSQL. This backend provides comprehensive functionality for managing destinations, activities, bookings, reviews, and user authentication.

## 🚀 Features

- **Authentication & Authorization**: JWT-based authentication with role-based access control (CLIENT, ADMIN)
- **User Management**: Complete user CRUD operations with admin controls
- **Destinations**: Manage tourism destinations with rich descriptions and images
- **Activities**: Comprehensive activity management with filtering, search, and categorization
- **Bookings**: Full booking system with status management and reference generation
- **Reviews**: Review system with approval workflow and automatic rating calculation
- **Favorites**: Wishlist functionality for users
- **Settings**: Website configuration management
- **Dashboard**: Admin dashboard with statistics and analytics
- **Data Seeding**: Automatic seeding of realistic tourism data on startup

## 🛠️ Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** with JWT
- **Spring Data JPA**
- **PostgreSQL**
- **Lombok**
- **MapStruct** for DTO mapping
- **Swagger/OpenAPI** for API documentation
- **Maven** for dependency management

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 12+ (or Docker)
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## Deployment (VPS / Docker)

Production stack (PostgreSQL + Spring Boot + nginx static SPA) lives in [`deploy/`](deploy/README.md). Clone this repository next to the frontend repo on the server and use `deploy/docker-compose.prod.yml` with a `.env` file (see `deploy/env.example`).

## 🔧 Setup Instructions

### 1. Database Setup

#### Option A: Docker Compose (recommended — data survives restarts)

From the **repository root** (folder that contains `backend/` and `docker-compose.yml`):

```bash
docker compose up -d
```

This uses database `tourisme` on port `5432` with defaults matching `application.yml` (user `postgres`, password `5392`) and a **named volume** so data is not lost when you stop the container.

#### Option B: One-off Docker container (no named volume by default)

```bash
docker run --name tourisme-postgres \
  -e POSTGRES_DB=tourisme \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=5392 \
  -p 5432:5432 \
  -v tourisme_pgdata:/var/lib/postgresql/data \
  -d postgres:16-alpine
```

#### Option C: Local PostgreSQL

1. Create database `tourisme` (or set `DB_URL` to match your DB name).
2. Update `application.yml` or environment variables if credentials differ from defaults.

#### Restoring from a backup file (`pg_dump` / `latest.dump`)

Use **`docs/restore-database.md`** at the repo root: run `scripts/restore-pg-dump.ps1`, then set **`SEED_RUN_ON_STARTUP=false`** so the Java `DataSeeder` does not run against your restored database.

### 2. Configuration

The application uses environment variables for configuration. Default values are set in `application.yml`:

```yaml
DB_URL: jdbc:postgresql://localhost:5432/tourisme
DB_USERNAME: postgres
DB_PASSWORD: 5392
JWT_SECRET: your-256-bit-secret-key-change-this-in-production-minimum-32-characters
CORS_ORIGINS: http://localhost:5173,http://localhost:3000
SERVER_PORT: 8080
```

You can override these by setting environment variables or creating an `application-local.yml` file.

### 3. Build and Run

```bash
# Navigate to backend directory
cd backend

# Build the project
mvn clean install

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### PostgreSQL data backup and restore (no schema in the dump file)

Scripts export **only row data** (`pg_dump --data-only`): no `CREATE TABLE`, functions, or extensions. Restore assumes the **schema already exists** (e.g. tables created by Hibernate `ddl-auto: update` after you start the app once).

**Backup from the admin panel:** Dashboard or Settings → **Download PostgreSQL data (.sql)**. The Spring Boot host must have `pg_dump` available (or set environment variable `PG_DUMP_PATH` to the full path of `pg_dump` / `pg_dump.exe`).

**Backup (PowerShell, from repository root):**

```powershell
.\scripts\postgres-backup.ps1
```

- Writes `backups\tourisme-data-<timestamp>.sql` (gitignored).
- Add `-UseInserts` for `INSERT` statements instead of `COPY` (larger, easier to read).
- Override `-User`, `-Database`, `-Password` if they differ from `application.yml`.

Requires `pg_dump` on your PATH (e.g. `...\PostgreSQL\16\bin`).

**pgAdmin:** Backup → **Dump options #1** → enable **Data only** (no schema). Restore into a database that already has the tables.

**Restore (stop Spring Boot first):**

```powershell
.\scripts\postgres-restore.ps1 -SqlFile ".\backups\tourisme-data-YYYY-MM-DD-HHMMSS.sql"
```

If tables still have old rows and you get duplicate-key errors, clear data then reload:

```powershell
.\scripts\postgres-restore.ps1 -SqlFile ".\backups\..." -ClearExistingData
```

The admin panel **Download backup (JSON)** is a separate, app-level export (no real password hashes for users).

### 4. Verify Setup

- API Base URL: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/api-docs`

## 👤 Default Credentials

The application automatically seeds the database with test data on first startup:

### Admin Account
- **Email**: `admin@tourisme.com`
- **Password**: `admin123`
- **Role**: ADMIN

### Client Accounts
- **Email**: `john.doe@example.com`
- **Password**: `client123`
- **Role**: CLIENT

Additional test client accounts:
- `jane.smith@example.com` / `client123`
- `ahmed@example.com` / `client123`
- `sarah.johnson@example.com` / `client123`
- `mohammed@example.com` / `client123`

## 📚 API Endpoints

### Authentication

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/auth/register` | Register new client | No |
| POST | `/api/auth/login` | Login user | No |
| GET | `/api/auth/me` | Get current user | Yes |

### Destinations (Public)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/destinations` | Get all destinations | No |
| GET | `/api/destinations/{id}` | Get destination by ID | No |
| GET | `/api/destinations/slug/{slug}` | Get destination by slug | No |

### Activities (Public)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/activities` | Get all activities | No |
| GET | `/api/activities/{id}` | Get activity by ID | No |
| GET | `/api/activities/slug/{slug}` | Get activity by slug | No |
| GET | `/api/activities/featured` | Get featured activities | No |
| GET | `/api/activities/search?keyword={keyword}` | Search activities | No |
| GET | `/api/activities/filter` | Filter activities | No |

**Filter Parameters:**
- `destinationId` - Filter by destination
- `category` - Filter by category
- `minPrice` - Minimum price
- `maxPrice` - Maximum price
- `minRating` - Minimum rating
- `difficulty` - Difficulty level (EASY, MODERATE, HARD, EXTREME)
- `featured` - Featured activities only

### Bookings (Client/Admin)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/bookings` | Create booking | Yes (CLIENT/ADMIN) |
| GET | `/api/bookings/my-bookings` | Get user's bookings | Yes (CLIENT/ADMIN) |
| GET | `/api/bookings/{id}` | Get booking by ID | Yes (CLIENT/ADMIN) |

### Reviews (Client/Admin)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/reviews` | Create review | Yes (CLIENT/ADMIN) |
| GET | `/api/reviews/activity/{activityId}` | Get reviews for activity | No |
| DELETE | `/api/reviews/{id}` | Delete review | Yes (Owner/ADMIN) |

### Favorites (Client/Admin)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/api/favorites/{activityId}` | Add to favorites | Yes (CLIENT/ADMIN) |
| DELETE | `/api/favorites/{activityId}` | Remove from favorites | Yes (CLIENT/ADMIN) |
| GET | `/api/favorites/my-favorites` | Get user's favorites | Yes (CLIENT/ADMIN) |

### Settings (Public)

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/api/settings` | Get website settings | No |

### Admin Endpoints

All admin endpoints require `ROLE_ADMIN` authentication.

#### Users Management
- `GET /api/admin/users` - Get all users
- `GET /api/admin/users/{id}` - Get user by ID
- `PUT /api/admin/users/{id}` - Update user
- `DELETE /api/admin/users/{id}` - Delete user
- `PATCH /api/admin/users/{id}/status` - Update user status

#### Destinations Management
- `POST /api/admin/destinations` - Create destination
- `PUT /api/admin/destinations/{id}` - Update destination
- `DELETE /api/admin/destinations/{id}` - Delete destination

#### Activities Management
- `POST /api/admin/activities` - Create activity
- `PUT /api/admin/activities/{id}` - Update activity
- `DELETE /api/admin/activities/{id}` - Delete activity
- `PATCH /api/admin/activities/{id}/status` - Update activity status

#### Bookings Management
- `GET /api/admin/bookings` - Get all bookings
- `GET /api/admin/bookings/{id}` - Get booking by ID
- `PATCH /api/admin/bookings/{id}/status` - Update booking status
- `DELETE /api/admin/bookings/{id}` - Delete booking

#### Reviews Management
- `GET /api/admin/reviews` - Get all reviews
- `PATCH /api/admin/reviews/{id}/approve` - Approve review
- `PATCH /api/admin/reviews/{id}/reject` - Reject review
- `DELETE /api/admin/reviews/{id}` - Delete review

#### Settings Management
- `PUT /api/admin/settings` - Update website settings

#### Dashboard
- `GET /api/admin/dashboard/stats` - Get dashboard statistics

## 🔐 Authentication

The API uses JWT (JSON Web Tokens) for authentication. Include the token in the Authorization header:

```
Authorization: Bearer <your-jwt-token>
```

### Example Login Request

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@tourisme.com",
    "password": "admin123"
  }'
```

### Example Authenticated Request

```bash
curl -X GET http://localhost:8080/api/bookings/my-bookings \
  -H "Authorization: Bearer <your-jwt-token>"
```

## 📊 Seeded Data

The application automatically seeds the database with:

- **1 Admin user** and **5 Client users**
- **6 Destinations** (Sahara Desert, Marrakech, Atlas Mountains, Chefchaouen, Fes, Casablanca)
- **9 Activities** (Desert Safari, Quad Biking, City Tours, Cooking Classes, Mountain Trekking, etc.)
- **4 Bookings** with various statuses
- **7 Reviews** (some approved, some pending)
- **5 Favorites**
- **1 Settings** record

All activities include realistic descriptions, pricing, images, itineraries, and availability dates.

## 🗄️ Database Schema

### Entities

- **User**: Users with roles (CLIENT, ADMIN)
- **Destination**: Tourism destinations
- **Activity**: Tourism activities linked to destinations
- **Booking**: User bookings for activities
- **Review**: User reviews for activities
- **Favorite**: User favorite activities
- **Settings**: Website configuration

### Relationships

- Destination 1..* Activities
- User 1..* Bookings
- User 1..* Reviews
- User 1..* Favorites
- Activity 1..* Bookings
- Activity 1..* Reviews
- Activity 1..* Favorites

## 🧪 Testing the API

### Using Swagger UI

1. Start the application
2. Navigate to `http://localhost:8080/swagger-ui.html`
3. Use the "Authorize" button to add your JWT token
4. Test endpoints directly from the UI

### Using cURL

#### Register a new user:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "test@example.com",
    "password": "password123",
    "phone": "+1234567890"
  }'
```

#### Get all activities:
```bash
curl -X GET http://localhost:8080/api/activities
```

#### Create a booking (requires authentication):
```bash
curl -X POST http://localhost:8080/api/bookings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "activityId": 1,
    "travelDate": "2024-12-25",
    "numberOfPeople": 2,
    "specialRequest": "Vegetarian meals"
  }'
```

## 🐛 Troubleshooting

### Database Connection Issues

- Ensure PostgreSQL is running
- Check database credentials in `application.yml`
- Verify database exists: `CREATE DATABASE tourisme_db;`

### Port Already in Use

- Change the port in `application.yml`: `server.port: 8081`
- Or stop the process using port 8080

### JWT Token Issues

- Ensure `JWT_SECRET` is at least 32 characters
- Check token expiration time (default: 24 hours)
- Verify token is included in Authorization header

### CORS Issues

- Update `app.cors.allowed-origins` in `application.yml`
- Ensure frontend URL is included in allowed origins

## 📝 Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/tourisme/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── exception/       # Exception handling
│   │   │   ├── mapper/          # Entity-DTO mappers
│   │   │   ├── repository/      # JPA repositories
│   │   │   ├── security/        # Security configuration
│   │   │   ├── service/         # Business logic
│   │   │   └── util/            # Utility classes
│   │   └── resources/
│   │       └── application.yml  # Application configuration
│   └── test/                    # Test files
├── pom.xml                      # Maven configuration
└── README.md                    # This file
```

## 🔒 Security Features

- JWT-based authentication
- Password encryption with BCrypt
- Role-based access control (RBAC)
- CORS configuration
- CSRF disabled for REST API
- Input validation on all DTOs
- SQL injection protection via JPA

## 🚀 Production Considerations

Before deploying to production:

1. **Change JWT Secret**: Use a strong, randomly generated secret (minimum 32 characters)
2. **Update Database Credentials**: Use environment variables or secure configuration
3. **Enable HTTPS**: Configure SSL/TLS certificates
4. **Configure CORS**: Restrict allowed origins to your frontend domain
5. **Database Security**: Use connection pooling and secure database credentials
6. **Logging**: Configure proper logging levels and log aggregation
7. **Monitoring**: Set up application monitoring and health checks
8. **Backup Strategy**: Implement database backup and recovery procedures

## 📄 License

This project is part of a tourism platform application.

## 🤝 Support

For issues or questions, please contact: info@tourtimeless.com

---

**Happy Coding! 🎉**
