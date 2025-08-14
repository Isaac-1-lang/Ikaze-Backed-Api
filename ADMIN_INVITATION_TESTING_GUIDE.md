# Admin Invitation API Testing Guide

This guide provides comprehensive testing examples for all Admin Invitation endpoints using Postman.

## Base URL

```
http://localhost:8080/api/v1/admin-invitations
```

## Authentication

All endpoints require Bearer token authentication except for public endpoints (accept, decline, validate, get by token).

**Header:**

```
Authorization: Bearer <your_jwt_token>
```

## 1. Create Admin Invitation

**POST** `/api/v1/admin-invitations`

**Headers:**

```
Content-Type: application/json
Authorization: Bearer <admin_jwt_token>
```

**Body:**

```json
{
  "email": "newadmin@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "assignedRole": "ADMIN",
  "invitationMessage": "Welcome to our team! Please accept this invitation to join as an administrator.",
  "department": "IT",
  "position": "System Administrator",
  "phoneNumber": "+1234567890",
  "notes": "Experienced IT professional with 5+ years in system administration",
  "expiresAt": "2025-08-14T15:59:49"
}
```

**Expected Response (201):**

```json
{
  "success": true,
  "message": "Admin invitation created successfully",
  "data": {
    "invitationId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "newadmin@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "fullName": "John Doe",
    "assignedRole": "ADMIN",
    "invitationToken": "abc123def456ghi789jkl012mno345pqr678stu901",
    "status": "PENDING",
    "expiresAt": "2025-08-14T15:59:49",
    "createdAt": "2025-08-12T15:59:49",
    "updatedAt": "2025-08-12T15:59:49",
    "invitedById": "admin-user-id",
    "invitedByName": "Admin User",
    "invitedByEmail": "admin@example.com",
    "invitationMessage": "Welcome to our team! Please accept this invitation to join as an administrator.",
    "department": "IT",
    "position": "System Administrator",
    "phoneNumber": "+1234567890",
    "notes": "Experienced IT professional with 5+ years in system administration",
    "isExpired": false,
    "canBeAccepted": true,
    "canBeCancelled": true
  }
}
```

## 2. Update Admin Invitation

**PUT** `/api/v1/admin-invitations/{invitationId}`

**Headers:**

```
Content-Type: application/json
Authorization: Bearer <admin_jwt_token>
```

**Body:**

```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "assignedRole": "EMPLOYEE",
  "invitationMessage": "Updated invitation message",
  "department": "HR",
  "position": "HR Manager",
  "phoneNumber": "+1987654321",
  "notes": "Updated notes",
  "expiresAt": "2025-08-15T15:59:49"
}
```

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Admin invitation updated successfully",
  "data": {
    "invitationId": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "Jane",
    "lastName": "Smith",
    "assignedRole": "EMPLOYEE",
    "invitationMessage": "Updated invitation message",
    "department": "HR",
    "position": "HR Manager",
    "phoneNumber": "+1987654321",
    "notes": "Updated notes",
    "expiresAt": "2025-08-15T15:59:49"
  }
}
```

## 3. Get Invitation by ID

**GET** `/api/v1/admin-invitations/{invitationId}`

**Headers:**

```
Authorization: Bearer <admin_or_employee_jwt_token>
```

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Admin invitation retrieved successfully",
  "data": {
    "invitationId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "newadmin@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "fullName": "Jane Smith",
    "assignedRole": "EMPLOYEE",
    "invitationToken": "abc123def456ghi789jkl012mno345pqr678stu901",
    "status": "PENDING",
    "expiresAt": "2025-08-15T15:59:49",
    "createdAt": "2025-08-12T15:59:49",
    "updatedAt": "2025-08-12T16:30:00",
    "invitedById": "admin-user-id",
    "invitedByName": "Admin User",
    "invitedByEmail": "admin@example.com",
    "invitationMessage": "Updated invitation message",
    "department": "HR",
    "position": "HR Manager",
    "phoneNumber": "+1987654321",
    "notes": "Updated notes",
    "isExpired": false,
    "canBeAccepted": true,
    "canBeCancelled": true
  }
}
```

## 4. Get Invitation by Token (Public)

**GET** `/api/v1/admin-invitations/token/{invitationToken}`

**No authentication required**

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Admin invitation retrieved successfully",
  "data": {
    "invitationId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "newadmin@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "fullName": "Jane Smith",
    "assignedRole": "EMPLOYEE",
    "invitationToken": "abc123def456ghi789jkl012mno345pqr678stu901",
    "status": "PENDING",
    "expiresAt": "2025-08-15T15:59:49",
    "createdAt": "2025-08-12T15:59:49",
    "updatedAt": "2025-08-12T16:30:00",
    "invitedById": "admin-user-id",
    "invitedByName": "Admin User",
    "invitedByEmail": "admin@example.com",
    "invitationMessage": "Updated invitation message",
    "department": "HR",
    "position": "HR Manager",
    "phoneNumber": "+1987654321",
    "notes": "Updated notes",
    "isExpired": false,
    "canBeAccepted": true,
    "canBeCancelled": true
  }
}
```

## 5. Get All Invitations

**GET** `/api/v1/admin-invitations?page=0&size=10&sortBy=createdAt&sortDirection=desc`

**Headers:**

```
Authorization: Bearer <admin_or_employee_jwt_token>
```

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Admin invitations retrieved successfully",
  "data": [
    {
      "invitationId": "550e8400-e29b-41d4-a716-446655440000",
      "email": "newadmin@example.com",
      "firstName": "Jane",
      "lastName": "Smith",
      "fullName": "Jane Smith",
      "assignedRole": "EMPLOYEE",
      "invitationToken": "abc123def456ghi789jkl012mno345pqr678stu901",
      "status": "PENDING",
      "expiresAt": "2025-08-15T15:59:49",
      "createdAt": "2025-08-12T15:59:49",
      "updatedAt": "2025-08-12T16:30:00",
      "invitedById": "admin-user-id",
      "invitedByName": "Admin User",
      "invitedByEmail": "admin@example.com",
      "invitationMessage": "Updated invitation message",
      "department": "HR",
      "position": "HR Manager",
      "phoneNumber": "+1987654321",
      "notes": "Updated notes",
      "isExpired": false,
      "canBeAccepted": true,
      "canBeCancelled": true
    }
  ],
  "pagination": {
    "currentPage": 0,
    "totalPages": 1,
    "totalElements": 1,
    "size": 10,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

## 6. Search Invitations

**POST** `/api/v1/admin-invitations/search?page=0&size=10`

**Headers:**

```
Content-Type: application/json
Authorization: Bearer <admin_or_employee_jwt_token>
```

**Body:**

```json
{
  "email": "newadmin@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "assignedRole": "EMPLOYEE",
  "status": "PENDING",
  "department": "HR",
  "position": "HR Manager",
  "phoneNumber": "+1987654321",
  "invitationMessage": "Updated invitation message",
  "notes": "Updated notes",
  "createdFrom": "2025-08-12T00:00:00",
  "createdTo": "2025-08-12T23:59:59",
  "expiresFrom": "2025-08-15T00:00:00",
  "expiresTo": "2025-08-15T23:59:59",
  "sortBy": "createdAt",
  "sortDirection": "desc"
}
```

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Search completed successfully",
  "data": [
    {
      "invitationId": "550e8400-e29b-41d4-a716-446655440000",
      "email": "newadmin@example.com",
      "firstName": "Jane",
      "lastName": "Smith",
      "fullName": "Jane Smith",
      "assignedRole": "EMPLOYEE",
      "invitationToken": "abc123def456ghi789jkl012mno345pqr678stu901",
      "status": "PENDING",
      "expiresAt": "2025-08-15T15:59:49",
      "createdAt": "2025-08-12T15:59:49",
      "updatedAt": "2025-08-12T16:30:00",
      "invitedById": "admin-user-id",
      "invitedByName": "Admin User",
      "invitedByEmail": "admin@example.com",
      "invitationMessage": "Updated invitation message",
      "department": "HR",
      "position": "HR Manager",
      "phoneNumber": "+1987654321",
      "notes": "Updated notes",
      "isExpired": false,
      "canBeAccepted": true,
      "canBeCancelled": true
    }
  ],
  "pagination": {
    "currentPage": 0,
    "totalPages": 1,
    "totalElements": 1,
    "size": 10,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

## 7. Get Invitations by Status

**GET** `/api/v1/admin-invitations/status/PENDING?page=0&size=10`

**Headers:**

```
Authorization: Bearer <admin_or_employee_jwt_token>
```

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Admin invitations retrieved successfully",
  "data": [
    {
      "invitationId": "550e8400-e29b-41d4-a716-446655440000",
      "email": "newadmin@example.com",
      "firstName": "Jane",
      "lastName": "Smith",
      "fullName": "Jane Smith",
      "assignedRole": "EMPLOYEE",
      "invitationToken": "abc123def456ghi789jkl012mno345pqr678stu901",
      "status": "PENDING",
      "expiresAt": "2025-08-15T15:59:49",
      "createdAt": "2025-08-12T15:59:49",
      "updatedAt": "2025-08-12T16:30:00",
      "invitedById": "admin-user-id",
      "invitedByName": "Admin User",
      "invitedByEmail": "admin@example.com",
      "invitationMessage": "Updated invitation message",
      "department": "HR",
      "position": "HR Manager",
      "phoneNumber": "+1987654321",
      "notes": "Updated notes",
      "isExpired": false,
      "canBeAccepted": true,
      "canBeCancelled": true
    }
  ],
  "pagination": {
    "currentPage": 0,
    "totalPages": 1,
    "totalElements": 1,
    "size": 10,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

## 8. Accept Invitation (Public)

**POST** `/api/v1/admin-invitations/accept`

**Headers:**

```
Content-Type: application/json
```

**Body:**

```json
{
  "invitationToken": "abc123def456ghi789jkl012mno345pqr678stu901",
  "password": "SecurePassword123!",
  "phoneNumber": "+1987654321",
  "additionalNotes": "Looking forward to joining the team!"
}
```

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Admin invitation accepted successfully",
  "data": {
    "invitationId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "newadmin@example.com",
    "firstName": "Jane",
    "lastName": "Smith",
    "fullName": "Jane Smith",
    "assignedRole": "EMPLOYEE",
    "invitationToken": "abc123def456ghi789jkl012mno345pqr678stu901",
    "status": "ACCEPTED",
    "expiresAt": "2025-08-15T15:59:49",
    "acceptedAt": "2025-08-12T17:00:00",
    "createdAt": "2025-08-12T15:59:49",
    "updatedAt": "2025-08-12T17:00:00",
    "invitedById": "admin-user-id",
    "invitedByName": "Admin User",
    "invitedByEmail": "admin@example.com",
    "acceptedById": "new-user-id",
    "acceptedByName": "Jane Smith",
    "acceptedByEmail": "newadmin@example.com",
    "invitationMessage": "Updated invitation message",
    "department": "HR",
    "position": "HR Manager",
    "phoneNumber": "+1987654321",
    "notes": "Updated notes",
    "isExpired": false,
    "canBeAccepted": false,
    "canBeCancelled": false
  }
}
```

## 9. Decline Invitation (Public)

**POST** `/api/v1/admin-invitations/decline/{invitationToken}`

**No authentication required**

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Admin invitation declined successfully"
}
```

## 10. Validate Invitation (Public)

**GET** `/api/v1/admin-invitations/validate/{invitationToken}`

**No authentication required**

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Invitation validation completed",
  "data": {
    "isValid": true,
    "isExpired": false,
    "canBeAccepted": true
  }
}
```

## 11. Cancel Invitation

**POST** `/api/v1/admin-invitations/{invitationId}/cancel`

**Headers:**

```
Authorization: Bearer <admin_jwt_token>
```

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Admin invitation cancelled successfully"
}
```

## 12. Resend Invitation

**POST** `/api/v1/admin-invitations/{invitationId}/resend`

**Headers:**

```
Authorization: Bearer <admin_jwt_token>
```

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Admin invitation resent successfully"
}
```

## 13. Delete Invitation

**DELETE** `/api/v1/admin-invitations/{invitationId}`

**Headers:**

```
Authorization: Bearer <admin_jwt_token>
```

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Admin invitation deleted successfully"
}
```

## 14. Get Invitation Statistics

**GET** `/api/v1/admin-invitations/statistics`

**Headers:**

```
Authorization: Bearer <admin_or_employee_jwt_token>
```

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Admin invitation statistics retrieved successfully",
  "data": {
    "pending": 5,
    "accepted": 10,
    "declined": 2,
    "expired": 3,
    "cancelled": 1,
    "expiredPending": 1,
    "total": 21
  }
}
```

## 15. Mark Expired Invitations

**POST** `/api/v1/admin-invitations/expired/mark`

**Headers:**

```
Authorization: Bearer <admin_jwt_token>
```

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Expired invitations marked successfully"
}
```

## 16. Delete Expired Invitations

**DELETE** `/api/v1/admin-invitations/expired`

**Headers:**

```
Authorization: Bearer <admin_jwt_token>
```

**Expected Response (200):**

```json
{
  "success": true,
  "message": "Expired invitations deleted successfully"
}
```

## Error Responses

### 400 Bad Request

```json
{
  "success": false,
  "message": "Invalid request data"
}
```

### 401 Unauthorized

```json
{
  "success": false,
  "message": "Unauthorized - Admin access required"
}
```

### 403 Forbidden

```json
{
  "success": false,
  "message": "Forbidden - Insufficient privileges"
}
```

### 404 Not Found

```json
{
  "success": false,
  "message": "Invitation not found"
}
```

### 409 Conflict

```json
{
  "success": false,
  "message": "Invitation cannot be accepted"
}
```

### 500 Internal Server Error

```json
{
  "success": false,
  "message": "Failed to create admin invitation"
}
```

## Testing Scenarios

### 1. Authentication Testing

- Test with valid admin token
- Test with valid employee token
- Test with invalid token
- Test with missing token
- Test public endpoints without token

### 2. Authorization Testing

- Test admin-only endpoints with employee token
- Test employee-accessible endpoints with customer token
- Test public endpoints with various tokens

### 3. Validation Testing

- Test with invalid email format
- Test with missing required fields
- Test with invalid role values
- Test with invalid phone number format
- Test with future expiration dates

### 4. Business Logic Testing

- Test creating invitation for existing user
- Test accepting expired invitation
- Test accepting already accepted invitation
- Test updating non-pending invitation
- Test cancelling non-pending invitation

### 5. Edge Cases

- Test with very long text fields
- Test with special characters
- Test with empty strings
- Test with null values
- Test pagination with large datasets

## Email Functionality

### Email Types

1. **New User Invitation Email**: Sent when inviting a user who doesn't exist in the system

   - Contains invitation token in URL link
   - User clicks link to set up password and account
   - Includes role, department, position, and custom message

2. **Existing User Role Update Email**: Sent when inviting a user who already exists

   - No token or action required from user
   - Simply notifies about role change
   - Role is automatically updated in the system

3. **Invitation Resend Email**: Sent when resending an invitation
   - Contains new invitation token
   - Previous token is invalidated
   - Fresh 48-hour expiration

### Email Configuration

The system uses the following email configuration:

- **SMTP Host**: smtp.gmail.com
- **Port**: 587
- **Authentication**: Required
- **TLS**: Enabled
- **From Email**: ashraftuyubahe001@gmail.com

### Email Templates

Each email type has a customized template with:

- Personalized greeting with recipient's name
- Clear explanation of the invitation/role change
- Department and position information (if provided)
- Custom message from the admin (if provided)
- Expiration information (for new invitations)
- Contact information for questions

## Notes

1. **Token Generation**: Each invitation gets a unique 32-character token
2. **Expiration**: Invitations expire after 48 hours by default
3. **Role Assignment**: Supports ADMIN, EMPLOYEE, DELIVERY_AGENT roles
4. **User Creation**: Accepting invitation creates new user or updates existing user's role
5. **Security**: Only ADMIN users can create, update, delete, cancel, and resend invitations
6. **Public Endpoints**: Accept, decline, validate, and get by token are public
7. **Pagination**: All list endpoints support pagination with customizable page size and sorting
8. **Search**: Comprehensive search with multiple criteria and date ranges
9. **Statistics**: Real-time statistics for dashboard displays
10. **Cleanup**: Automatic marking and deletion of expired invitations
11. **Email Integration**: Automatic email sending for invitations and role updates
12. **Error Handling**: Email failures don't prevent invitation creation/updates
