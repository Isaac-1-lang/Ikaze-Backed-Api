# Ikaze API Documentation

## Register API

### Overview
The Register API allows new users to create an account in the Ikaze system. The API validates input data, checks for duplicate usernames and emails, encrypts passwords using BCrypt, and stores user information securely.

### Endpoint
```
POST /api/auth/register
```

### Request Headers
```
Content-Type: application/json
```

### Request Body
```json
{
  "username": "Elias",
  "email": "elias250@example.com",
  "password": "SecurePass123!"
}
```

### Request Parameters

| Field    | Type   | Required | Constraints                              | Description                    |
|----------|--------|----------|------------------------------------------|--------------------------------|
| username | string | Yes      | 3-50 characters, unique                  | Username for the new account   |
| email    | string | Yes      | Valid email format, unique               | Email address                  |
| password | string | Yes      | 8-100 characters                         | Password (will be encrypted)   |

### Response

#### Success Response (201 Created)
```json
{
  "message": "User registered successfully! Please check your email for verification.",
  "success": true,
  "statusCode": 201
}
```

#### Error Response - Validation Failed (400 Bad Request)
```json
{
  "timestamp": "2026-04-23T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid input data",
  "path": "/api/auth/register",
  "errors": [
    "username: Username must be between 3 and 50 characters",
    "email: Email must be valid"
  ]
}
```

#### Error Response - Username Already Taken (400 Bad Request)
```json
{
  "timestamp": "2026-04-23T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Username already taken!",
  "path": "/api/auth/register",
  "errors": null
}
```

#### Error Response - Email Already Registered (400 Bad Request)
```json
{
  "timestamp": "2026-04-23T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Email already registered!",
  "path": "/api/auth/register",
  "errors": null
}
```

### Example Usage

#### cURL
```bash
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "Elias",
    "email": "elias250@example.com",
    "password": "SecurePass123!"
  }'
```

#### JavaScript (Fetch API)
```javascript
fetch('http://localhost:3000/api/auth/register', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    username: 'Elias',
    email: 'elias250  @example.com',
    password: 'SecurePass123!'
  })
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error('Error:', error));
```

#### Python (Requests)
```python
import requests

url = "http://localhost:3000/api/auth/register"
payload = {
    "username": "john_doe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!"
}

response = requests.post(url, json=payload)
print(response.json())
```

## Swagger UI Documentation

### Accessing Swagger UI
Once your application is running, you can access the interactive API documentation at:

```
http://localhost:3000/swagger-ui.html
```

or

```
http://localhost:3000/swagger-ui/index.html
```

### OpenAPI JSON Specification
The raw OpenAPI specification is available at:

```
http://localhost:3000/v3/api-docs
```

### Features Available in Swagger UI
- Interactive API testing
- Request/response examples
- Schema definitions
- Authentication testing (when implemented)
- Try out API endpoints directly from the browser

## Security Features

1. **Password Encryption**: All passwords are encrypted using BCrypt before storage
2. **Input Validation**: Comprehensive validation on all input fields
3. **Duplicate Prevention**: Checks for existing usernames and emails
4. **CSRF Protection**: Disabled for stateless API (JWT-based authentication)
5. **Session Management**: Stateless session policy for REST API

## Running the Application

1. Make sure you have Java 17+ and Maven installed
2. Navigate to the project directory
3. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   or on Windows:
   ```bash
   mvnw.cmd spring-boot:run
   ```

4. The application will start on `http://localhost:3000`

## Testing the API

You can test the API using:
- Swagger UI (recommended for beginners)
- Postman
- cURL
- Any HTTP client library

## Next Steps

After implementing the register API, you may want to:
1. Implement email verification
2. Add login endpoint with JWT token generation
3. Implement password reset functionality
4. Add user profile management
5. Implement role-based access control
