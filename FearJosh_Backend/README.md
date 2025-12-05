# FearJosh Backend

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
