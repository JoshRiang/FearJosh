# FearJosh Backend

Backend API untuk game FearJosh yang menyimpan skor pemain dan menampilkan leaderboard.

## Tech Stack
- Java 17
- Spring Boot 3.2.0
- PostgreSQL (Neon Cloud)
- Maven

## Menjalankan Backend

```bash
cd FearJosh_Backend
mvn spring-boot:run
```

Server akan berjalan di `http://localhost:8080/api`

## API Endpoints

### Base URL
```
http://localhost:8080/api
```

### 1. Submit Score (Saat Player Berhasil Escape)
**POST** `/scores`

Request Body:
```json
{
    "playerId": "unique-player-id-123",
    "username": "PlayerName",
    "difficulty": "NORMAL",
    "completionTimeSeconds": 3600
}
```

Response:
```json
{
    "success": true,
    "message": "Score submitted successfully!",
    "data": {
        "id": 1,
        "playerId": "unique-player-id-123",
        "username": "PlayerName",
        "difficulty": "NORMAL",
        "completionTimeSeconds": 3600,
        "completionTimeFormatted": "01:00:00",
        "completedAt": "2025-12-21T10:30:00",
        "rank": null
    }
}
```

### 2. Get Leaderboard by Difficulty
**GET** `/scores/leaderboard?difficulty=NORMAL&limit=10`

Query Parameters:
- `difficulty` (optional): EASY, NORMAL, HARD, NIGHTMARE, atau kosong untuk semua
- `limit` (optional, default: 10): Jumlah maksimal hasil

Response:
```json
{
    "success": true,
    "message": "Success",
    "data": {
        "difficulty": "NORMAL",
        "totalPlayers": 150,
        "leaderboard": [
            {
                "id": 1,
                "playerId": "player-1",
                "username": "FastPlayer",
                "difficulty": "NORMAL",
                "completionTimeSeconds": 1800,
                "completionTimeFormatted": "30:00",
                "completedAt": "2025-12-21T10:30:00",
                "rank": 1
            }
        ]
    }
}
```

### 3. Get Global Leaderboard
**GET** `/scores/leaderboard/global?limit=10`

### 4. Get Player Rank
**GET** `/scores/rank/{playerId}`

Response:
```json
{
    "success": true,
    "message": "Success",
    "data": {
        "playerId": "unique-player-id-123",
        "username": "PlayerName",
        "difficulty": "NORMAL",
        "completionTimeSeconds": 3600,
        "completionTimeFormatted": "01:00:00",
        "rank": 5,
        "totalPlayers": 150
    }
}
```

### 5. Get Player Score
**GET** `/scores/player/{playerId}`

### 6. Check Player Exists
**GET** `/scores/exists/{playerId}`

### 7. Search by Username
**GET** `/scores/search?username=john`

### 8. Health Check
**GET** `/scores/health`

### 9. Delete Score (Admin)
**DELETE** `/scores/{playerId}`

## Database Schema

Tabel `game_scores`:
| Column | Type | Description |
|--------|------|-------------|
| id | BIGSERIAL | Primary key |
| player_id | VARCHAR | Unique player identifier |
| username | VARCHAR | Player display name |
| difficulty | VARCHAR | EASY, NORMAL, HARD, NIGHTMARE |
| completion_time_seconds | BIGINT | Time in seconds |
| completion_time_formatted | VARCHAR | Formatted time (MM:SS or HH:MM:SS) |
| completed_at | TIMESTAMP | When the game was completed |

## Difficulty Values
- `EASY`
- `NORMAL`
- `HARD`
- `NIGHTMARE`

Spring Boot backend application for FearJosh project.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+ (or use H2 for development)

## Setup

1. **Clone the repository**

2. **Configure Database**
   - Edit `src/main/resources/application.properties`
   - Update database credentials:
     ```properties
     spring.datasource.url=jdbc:mysql://localhost:3306/fearjosh_db
     spring.datasource.username=your_username
     spring.datasource.password=your_password
     ```

3. **Create Database**
   ```sql
   CREATE DATABASE fearjosh_db;
   ```

## Running the Application

### Using Maven

```bash
mvnw spring-boot:run
```

Or if Maven is installed globally:

```bash
mvn spring-boot:run
```

### Using IDE

Run the `FearJoshBackendApplication` class directly from your IDE.

## API Endpoints

Base URL: `http://localhost:8080/api`

### User Endpoints

- `GET /users` - Get all users
- `GET /users/{id}` - Get user by ID
- `POST /users` - Create new user
- `PUT /users/{id}` - Update user
- `DELETE /users/{id}` - Delete user

### Example Request (Create User)

```json
POST /api/users
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "password123",
  "fullName": "John Doe"
}
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── fearjosh/
│   │           ├── FearJoshBackendApplication.java
│   │           ├── config/
│   │           │   └── WebConfig.java
│   │           ├── controller/
│   │           │   └── UserController.java
│   │           ├── dto/
│   │           │   └── UserDTO.java
│   │           ├── exception/
│   │           │   └── GlobalExceptionHandler.java
│   │           ├── model/
│   │           │   └── User.java
│   │           ├── repository/
│   │           │   └── UserRepository.java
│   │           └── service/
│   │               └── UserService.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/
```

## Technologies Used

- Spring Boot 3.2.0
- Spring Data JPA
- MySQL / H2 Database
- Lombok
- Maven

## Development

The application includes:
- ✅ RESTful API architecture
- ✅ JPA/Hibernate for database operations
- ✅ DTO pattern for data transfer
- ✅ Service layer for business logic
- ✅ Exception handling
- ✅ CORS configuration
- ✅ Lombok for reducing boilerplate code

## License

This project is for educational purposes.
