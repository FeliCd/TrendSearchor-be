# TrendSearchor API — Frontend Developer Guide

## Table of Contents

1. [Base URL & Common Headers](#1-base-url--common-headers)
2. [Authentication Flow](#2-authentication-flow)
3. [Auth Endpoints](#3-auth-endpoints)
   - [3.1 Register](#31-post-apiauthregister)
   - [3.2 Login](#32-post-apiauthlogin)
   - [3.3 Get Current User](#33-get-apiauthme)
   - [3.4 Change Password](#34-post-apiauthchange-password)
   - [3.5 Forgot Password](#35-post-apiauthforgot-password)
   - [3.6 Logout](#36-post-apiauthlogout)
4. [Admin User Management Endpoints](#4-admin-user-management-endpoints)
5. [Error Handling](#5-error-handling)
6. [Enums Reference](#6-enums-reference)
7. [Validation Rules](#7-validation-rules)

---

## 1. Base URL & Common Headers

| Environment | Base URL |
|-------------|----------|
| Production | `https://trendsearchor-be-production.up.railway.app` |
| Local | `http://localhost:8080` |

**Headers required for every authenticated request:**

```
Authorization: Bearer <accessToken>
Content-Type: application/json
```

> After login, save the `accessToken` from the response and send it as `Authorization: Bearer <token>` in all subsequent requests.

**Public endpoints** (no token required):
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/forgot-password`

---

## 2. Authentication Flow

### 2.1 Register → Login → Store Token

```
FE: POST /api/auth/register  (public)
    ↓ 201 Created
FE: POST /api/auth/login     (public)
    ↓ 200 + { accessToken, user }
FE: Store token in localStorage / cookie
    ↓
FE: Use token in Authorization header for all protected requests
```

### 2.2 Token Lifecycle

- Token expires in **1 day** (`86400000ms`)
- On logout, token is invalidated server-side (added to blocklist)
- On 401 response, clear stored token and redirect to login

### 2.3 Storing the Token (React example)

```typescript
// After successful login:
localStorage.setItem('accessToken', response.data.accessToken);
localStorage.setItem('user', JSON.stringify(response.data.user));

// On every API call:
const token = localStorage.getItem('accessToken');
fetch('/api/auth/me', {
  headers: { Authorization: `Bearer ${token}` }
});

// On 401 response:
localStorage.removeItem('accessToken');
window.location.href = '/login';
```

---

## 3. Auth Endpoints

### 3.1 POST `/api/auth/register`

**Public — No token required.**

Register a new user account.

**Request Body:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `username` | string | ✅ | 3–50 characters, unique |
| `password` | string | ✅ | Min 9 chars, 1 uppercase, 1 number, 1 special (`@$!%*?&`) |
| `confirmPassword` | string | ✅ | Must match `password` |
| `mail` | string | ✅ | Valid email format |
| `phone` | string | ✅ | Vietnamese format: `0[35789]XXXXXXXX` (10 digits) |
| `gender` | string | ✅ | One of: `MALE`, `FEMALE`, `OTHERS` |
| `dob` | string (ISO date) | ✅ | Past date, year > 1920, format: `YYYY-MM-DD` |
| `workplace` | string | ✅ | Any non-empty string |
| `role` | string | ❌ | Default: `USER`. One of: `ADMIN`, `LECTURER`, `STUDENT`, `RESEARCHER`, `USER` |

**Request Example:**

```json
{
  "username": "nguyenvana",
  "password": "Password@123",
  "confirmPassword": "Password@123",
  "mail": "nguyenvana@example.com",
  "phone": "0912345678",
  "gender": "MALE",
  "dob": "1995-06-15",
  "workplace": "FPT University",
  "role": "USER"
}
```

**Response `201 Created`:**

```json
{
  "message": "User registered successfully"
}
```

**Error `400 Bad Request` — Validation failed:**

```json
{
  "username": "Username must be between 3 and 50 characters",
  "mail": "Username is already taken",
  "password": "Password must be at least 9 characters, contain 1 uppercase, 1 number, 1 special character"
}
```

---

### 3.2 POST `/api/auth/login`

**Public — No token required.**

Authenticate and receive a JWT token.

**Request Body:**

| Field | Type | Required |
|-------|------|----------|
| `username` | string | ✅ |
| `password` | string | ✅ |

**Request Example:**

```json
{
  "username": "nguyenvana",
  "password": "Password@123"
}
```

**Response `200 OK`:**

```json
{
  "accessToken": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 1,
    "username": "nguyenvana",
    "mail": "nguyenvana@example.com",
    "phone": "0912345678",
    "gender": "MALE",
    "workplace": "FPT University",
    "role": "USER",
    "status": "ACTIVE",
    "dob": "1995-06-15",
    "createdAt": "2026-05-15T12:00:00",
    "lastLogin": "2026-05-15T14:30:00"
  }
}
```

**Error `400 Bad Request` — Validation:**

```json
{
  "message": "Username is required"
}
```

**Error `401 Unauthorized` — Wrong credentials:**

```json
{
  "message": "Invalid username or password"
}
```

**Error `401 Unauthorized` — Too many failed attempts:**

```json
{
  "message": "Too many failed attempts. Account is temporarily locked for 15 minutes."
}
```

> **Rate limiting:** 5 failed attempts → 15-minute lockout. Remaining attempts shown in response.

---

### 3.3 GET `/api/auth/me`

**Protected — Requires valid token.**

Get the currently authenticated user's profile.

**Request:**

```
GET /api/auth/me
Authorization: Bearer <accessToken>
```

**Response `200 OK`:**

```json
{
  "id": 1,
  "username": "nguyenvana",
  "mail": "nguyenvana@example.com",
  "phone": "0912345678",
  "gender": "MALE",
  "workplace": "FPT University",
  "role": "USER",
  "status": "ACTIVE",
  "dob": "1995-06-15",
  "createdAt": "2026-05-15T12:00:00",
  "lastLogin": "2026-05-15T14:30:00"
}
```

**Error `401 Unauthorized`:**

```json
{
  "message": "Full authentication is required to access this resource"
}
```

---

### 3.4 POST `/api/auth/change-password`

**Protected — Requires valid token.**

Change the current user's password.

**Request:**

```
POST /api/auth/change-password
Authorization: Bearer <accessToken>
```

**Request Body:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `oldPassword` | string | ✅ | Current password |
| `newPassword` | string | ✅ | Same rules as register password |
| `confirmPassword` | string | ✅ | Must match `newPassword` |

**Request Example:**

```json
{
  "oldPassword": "OldPass@123",
  "newPassword": "NewPass@456",
  "confirmPassword": "NewPass@456"
}
```

**Response `200 OK`:**

```json
{
  "message": "Password changed successfully"
}
```

**Error `400 Bad Request`:**

```json
{
  "oldPassword": "Incorrect old password"
}
```

---

### 3.5 POST `/api/auth/forgot-password`

**Public — No token required.**

Request a password reset. A new password is generated and sent to the user's email.

> The response always returns success (to prevent email enumeration). The email is only sent if the account exists.

**Request Body:**

| Field | Type | Required |
|-------|------|----------|
| `mail` | string | ✅ |

**Request Example:**

```json
{
  "mail": "nguyenvana@example.com"
}
```

**Response `200 OK`:**

```json
{
  "message": "If the email exists, a new password will be sent to it."
}
```

---

### 3.6 POST `/api/auth/logout`

**Public — No token required.**

Invalidate the current JWT token. Token must still be valid (not expired) to be logged out.

**Request:**

```
POST /api/auth/logout
Authorization: Bearer <accessToken>
```

**Response `200 OK`:**

```json
{
  "message": "Logged out successfully"
}
```

**Error `400 Bad Request` — Invalid/missing token:**

```json
{
  "message": "Invalid or missing token"
}
```

---

## 4. Admin User Management Endpoints

All endpoints in this section require **`ADMIN` role**.

```
Authorization: Bearer <accessToken>
```

---

### 4.1 GET `/api/admin/users`

Paginated list of all users with optional filters.

**Query Parameters:**

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | `1` | Page number (1-indexed) |
| `limit` | int | `10` | Items per page (max: `100`) |
| `role` | string | — | Filter by role |
| `status` | string | — | Filter by status |
| `search` | string | — | Search by username or email |

**Request Example:**

```
GET /api/admin/users?page=1&limit=20&role=USER&status=ACTIVE&search=nguyen
Authorization: Bearer <adminToken>
```

**Response `200 OK`:**

```json
{
  "content": [
    {
      "id": 1,
      "username": "nguyenvana",
      "mail": "nguyenvana@example.com",
      "phone": "0912345678",
      "gender": "MALE",
      "workplace": "FPT University",
      "role": "USER",
      "status": "ACTIVE",
      "dob": "1995-06-15",
      "createdAt": "2026-05-15T12:00:00",
      "lastLogin": "2026-05-15T14:30:00"
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "currentPage": 1
}
```

---

### 4.2 GET `/api/admin/users/{id}`

Get a single user by ID.

**Request:**

```
GET /api/admin/users/5
Authorization: Bearer <adminToken>
```

**Response `200 OK`:**

```json
{
  "id": 5,
  "username": "lecturera",
  "mail": "lecturera@example.com",
  "phone": "0934567890",
  "gender": "FEMALE",
  "workplace": "FPT University",
  "role": "LECTURER",
  "status": "ACTIVE",
  "dob": "1980-03-20",
  "createdAt": "2026-05-10T08:00:00",
  "lastLogin": null
}
```

**Error `404 Not Found`:**

```json
{
  "message": "User not found"
}
```

---

### 4.3 POST `/api/admin/users`

Create a new user (admin action).

**Request:**

```
POST /api/admin/users
Authorization: Bearer <adminToken>
```

**Request Body:**

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `username` | string | ✅ | 3–50 characters, unique |
| `password` | string | ✅ | Same validation as register |
| `mail` | string | ✅ | Valid email, unique |
| `role` | string | ✅ | Must be one of the Role enums |
| `status` | string | ❌ | Default: `ACTIVE` |
| `phone` | string | ❌ | Vietnamese phone format |
| `gender` | string | ❌ | One of: `MALE`, `FEMALE`, `OTHERS` |
| `dob` | string | ❌ | Format: `YYYY-MM-DD` |
| `workplace` | string | ❌ | — |

**Request Example:**

```json
{
  "username": "newuser",
  "password": "AdminPass@123",
  "mail": "newuser@example.com",
  "role": "LECTURER",
  "status": "ACTIVE",
  "phone": "0901234567",
  "gender": "MALE",
  "dob": "1990-01-01",
  "workplace": "FPT University"
}
```

**Response `201 Created`:**

```json
{
  "id": 10,
  "username": "newuser",
  "mail": "newuser@example.com",
  "phone": "0901234567",
  "gender": "MALE",
  "workplace": "FPT University",
  "role": "LECTURER",
  "status": "ACTIVE",
  "dob": "1990-01-01",
  "createdAt": "2026-05-15T15:00:00",
  "lastLogin": null
}
```

---

### 4.4 PUT `/api/admin/users/{id}`

Update an existing user's profile fields.

**Request:**

```
PUT /api/admin/users/10
Authorization: Bearer <adminToken>
```

**Request Body** — all fields optional:

```json
{
  "username": "updateduser",
  "mail": "updated@example.com",
  "phone": "0987654321",
  "gender": "FEMALE",
  "dob": "1985-07-20",
  "workplace": "HCMUT"
}
```

> Only the fields provided in the body will be updated. Other fields remain unchanged.

**Response `200 OK`:**

```json
{
  "id": 10,
  "username": "updateduser",
  "mail": "updated@example.com",
  "phone": "0987654321",
  "gender": "FEMALE",
  "workplace": "HCMUT",
  "role": "LECTURER",
  "status": "ACTIVE",
  "dob": "1985-07-20",
  "createdAt": "2026-05-15T15:00:00",
  "lastLogin": null
}
```

---

### 4.5 DELETE `/api/admin/users/{id}`

Delete a user.

**Request:**

```
DELETE /api/admin/users/10
Authorization: Bearer <adminToken>
```

**Response `200 OK`:**

```json
{
  "message": "User deleted successfully"
}
```

---

### 4.6 PATCH `/api/admin/users/{id}/status`

Update a user's account status.

**Request:**

```
PATCH /api/admin/users/10/status
Authorization: Bearer <adminToken>
```

**Request Body:**

```json
{
  "status": "SUSPENDED"
}
```

**Response `200 OK`:**

```json
{
  "id": 10,
  "username": "updateduser",
  "mail": "updated@example.com",
  "role": "LECTURER",
  "status": "SUSPENDED",
  ...
}
```

---

### 4.7 PATCH `/api/admin/users/{id}/role`

Update a user's role.

**Request:**

```
PATCH /api/admin/users/10/role
Authorization: Bearer <adminToken>
```

**Request Body:**

```json
{
  "role": "ADMIN"
}
```

**Response `200 OK`:**

```json
{
  "id": 10,
  "username": "updateduser",
  "role": "ADMIN",
  "status": "ACTIVE",
  ...
}
```

---

## 5. Error Handling

### 5.1 HTTP Status Codes

| Code | Meaning | When it occurs |
|------|---------|----------------|
| `200` | OK | Successful GET, PUT, PATCH, DELETE |
| `201` | Created | Successful POST (register, create user) |
| `400` | Bad Request | Validation failure, missing/invalid fields |
| `401` | Unauthorized | Missing token, invalid token, expired token |
| `403` | Forbidden | Valid token but insufficient permissions (non-admin accessing admin endpoints) |
| `404` | Not Found | User/resource not found |
| `429` | Too Many Requests | Rate limit exceeded (5 failed login attempts) |
| `500` | Internal Server Error | Unexpected server error |

### 5.2 Error Response Format

All error responses follow this structure:

```json
{
  "message": "Human-readable error description"
}
```

**Validation errors** return a map of field → error message:

```json
{
  "username": "Username is already taken",
  "password": "Password must be at least 9 characters, contain 1 uppercase, 1 number, 1 special character",
  "mail": "Invalid email format"
}
```

### 5.3 Recommended FE Error Handling

```typescript
async function apiCall(url, options = {}) {
  try {
    const res = await fetch(url, options);
    const data = await res.json();

    if (!res.ok) {
      switch (res.status) {
        case 401:
          // Token expired or invalid — redirect to login
          localStorage.removeItem('accessToken');
          window.location.href = '/login';
          throw new Error(data.message || 'Unauthorized');
        case 403:
          throw new Error(data.message || 'Access denied');
        case 400:
          // Validation errors — data is a map of field errors
          if (typeof data === 'object' && !data.message) {
            throw { validationErrors: data };
          }
          throw new Error(data.message);
        case 429:
          throw new Error(data.message || 'Too many requests. Please wait.');
        default:
          throw new Error(data.message || 'Server error');
      }
    }
    return data;
  } catch (err) {
    console.error('API Error:', err);
    throw err;
  }
}
```

---

## 6. Enums Reference

### 6.1 Role

| Value | Description |
|-------|-------------|
| `ADMIN` | Full access, user management |
| `LECTURER` | Academic staff |
| `STUDENT` | Student account |
| `RESEARCHER` | Research account |
| `USER` | Default general user |

### 6.2 Gender

| Value |
|-------|
| `MALE` |
| `FEMALE` |
| `OTHERS` |

### 6.3 UserStatus

| Value | Description |
|-------|-------------|
| `ACTIVE` | Normal account, can login |
| `INACTIVE` | Account deactivated, cannot login |
| `SUSPENDED` | Account temporarily suspended, cannot login |

---

## 7. Validation Rules

### 7.1 Password

```
Pattern: ^(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{9,}$
```
- Minimum **9 characters**
- At least **1 uppercase letter**
- At least **1 number**
- At least **1 special character**: `@ $ ! % * ? &`

**Examples:**
- ✅ `Trend@2024Pass`
- ✅ `MyPassw0rd!`
- ❌ `trend2024pass` (no uppercase, no special)
- ❌ `TrendPass123` (no special character)
- ❌ `Trend@Pass` (only 8 chars)

### 7.2 Phone Number (Vietnam)

```
Pattern: ^0[35789][0-9]{8}$
```
- Starts with `03`, `05`, `07`, `08`, or `09`
- Total **10 digits**

**Examples:**
- ✅ `0912345678`
- ✅ `0901234567`
- ✅ `0834567890`
- ❌ `0123456789` (starts with 01)
- ❌ `091234567` (only 9 digits)

### 7.3 Date of Birth

- Must be a **past date**
- Year must be **greater than 1920**
- Format: `YYYY-MM-DD`

---

## Quick Reference — Axios Interceptor Setup

```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: 'https://trendsearchor-be-production.up.railway.app',
  headers: { 'Content-Type': 'application/json' }
});

// Attach token to every request
api.interceptors.request.use(config => {
  const token = localStorage.getItem('accessToken');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Handle 401 globally
api.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Usage
const login = (data) => api.post('/api/auth/login', data);
const register = (data) => api.post('/api/auth/register', data);
const getMe = () => api.get('/api/auth/me');
const getUsers = (params) => api.get('/api/admin/users', { params });
const createUser = (data) => api.post('/api/admin/users', data);
const updateUser = (id, data) => api.put(`/api/admin/users/${id}`, data);
const deleteUser = (id) => api.delete(`/api/admin/users/${id}`);
const updateStatus = (id, status) => api.patch(`/api/admin/users/${id}/status`, { status });
const updateRole = (id, role) => api.patch(`/api/admin/users/${id}/role`, { role });
const logout = () => api.post('/api/auth/logout');
```
