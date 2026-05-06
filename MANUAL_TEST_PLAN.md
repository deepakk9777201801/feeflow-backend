# FeeFlow Backend – Manual Test Plan

> **Base URL:** `http://localhost:8080`  
> **Auth:** All protected endpoints require `Authorization: Bearer <token>` header unless stated otherwise.  
> **Content-Type:** `application/json` unless stated otherwise.

---

## Table of Contents

1. [Auth – Register](#1-auth--register)
2. [Auth – Login](#2-auth--login)
3. [Auth – Get Current User (me)](#3-auth--get-current-user-me)
4. [Auth – Logout](#4-auth--logout)
5. [Auth – Forgot Password](#5-auth--forgot-password)
6. [Auth – Reset Password](#6-auth--reset-password)
7. [Institute – Create](#7-institute--create)
8. [Institute – Get My Institutes](#8-institute--get-my-institutes)
9. [Institute – Get By ID](#9-institute--get-by-id)
10. [Institute – Update](#10-institute--update)
11. [Batch – Create](#11-batch--create)
12. [Batch – Get By Institute](#12-batch--get-by-institute)
13. [Batch – Update](#13-batch--update)
14. [Batch – Delete](#14-batch--delete)
15. [Student – Create](#15-student--create)
16. [Student – Get All (with filters)](#16-student--get-all-with-filters)
17. [Student – Get By ID](#17-student--get-by-id)
18. [Student – Bulk Import (JSON)](#18-student--bulk-import-json)
19. [Student – Bulk Import (File)](#19-student--bulk-import-file)
20. [Cross-Cutting: Security & Authorization](#20-cross-cutting-security--authorization)

---

## 1. Auth – Register

**Endpoint:** `POST /api/v1/auth/register`  
**Auth Required:** No

### Positive Scenarios

| # | Test Case | Request Body | Expected Status | Expected Response |
|---|-----------|-------------|-----------------|-------------------|
| P1 | Register with all valid fields (no institute) | `{"name":"Alice","email":"alice@test.com","phone":"9876543210","password":"secret123"}` | 201 | `success: true`, JWT token returned, role defaults to `STUDENT` |
| P2 | Register with valid role `INSTITUTE_ADMIN` | `{"name":"Bob","email":"bob@test.com","phone":"9876543211","password":"pass123","role":"INSTITUTE_ADMIN"}` | 201 | `success: true`, roles includes `INSTITUTE_ADMIN` |
| P3 | Register with valid `instituteId` (existing institute) | `{"name":"Carol","email":"carol@test.com","phone":"9876543212","password":"pass123","instituteId":1}` | 201 | User linked to the institute |
| P4 | Register with role in lowercase (`student`) | `{"name":"Dave","email":"dave@test.com","phone":"9876543213","password":"pass123","role":"student"}` | 201 | Role normalised, `STUDENT` assigned |
| P5 | Register with role in mixed case (`Institute_Admin`) | `{"name":"Eve","email":"eve@test.com","phone":"9876543214","password":"pass123","role":"Institute_Admin"}` | 201 | Role normalised to `INSTITUTE_ADMIN` |
| P6 | Register with phone at minimum length (10 digits) | `{"name":"Frank","email":"frank@test.com","phone":"9876543210","password":"pass123"}` | 201 | Created successfully |
| P7 | Register with phone at maximum length (15 digits) | `{"name":"Grace","email":"grace@test.com","phone":"987654321098765","password":"pass123"}` | 201 | Created successfully |

### Negative Scenarios

| # | Test Case | Request Body | Expected Status | Expected Error |
|---|-----------|-------------|-----------------|----------------|
| N1 | Duplicate email | Same email as P1 | 409 / 400 | `UserAlreadyExistsException` / "Email is already registered" |
| N2 | Missing `name` field | `{"email":"x@test.com","phone":"9876543210","password":"pass"}` | 400 | `VALIDATION_FAILED`, name required |
| N3 | Missing `email` field | `{"name":"X","phone":"9876543210","password":"pass"}` | 400 | `VALIDATION_FAILED`, email required |
| N4 | Invalid email format | `{"name":"X","email":"not-an-email","phone":"9876543210","password":"pass"}` | 400 | `VALIDATION_FAILED`, invalid email format |
| N5 | Missing `phone` field | `{"name":"X","email":"x@test.com","password":"pass"}` | 400 | `VALIDATION_FAILED`, phone required |
| N6 | Phone with fewer than 10 digits | `{"name":"X","email":"x@test.com","phone":"12345","password":"pass"}` | 400 | `VALIDATION_FAILED`, phone must be 10-15 digits |
| N7 | Phone with more than 15 digits | `{"name":"X","email":"x@test.com","phone":"1234567890123456","password":"pass"}` | 400 | `VALIDATION_FAILED`, phone must be 10-15 digits |
| N8 | Phone with non-numeric characters | `{"name":"X","email":"x@test.com","phone":"98765abc10","password":"pass"}` | 400 | `VALIDATION_FAILED`, phone must be 10-15 digits |
| N9 | Missing `password` field | `{"name":"X","email":"x@test.com","phone":"9876543210"}` | 400 | `VALIDATION_FAILED`, password required |
| N10 | Role string exceeding 20 characters | `{"name":"X","email":"x@test.com","phone":"9876543210","password":"pass","role":"AVERYLONGROLENAME_EXCEEDING"}` | 400 | `VALIDATION_FAILED`, role must not exceed 20 chars |
| N11 | Non-existent `instituteId` | `{"name":"X","email":"x@test.com","phone":"9876543210","password":"pass","instituteId":99999}` | 404 | "Institute not found" |
| N12 | Empty request body | `{}` | 400 | `VALIDATION_FAILED`, multiple field errors |
| N13 | Blank `name` (whitespace only) | `{"name":"  ","email":"x@test.com","phone":"9876543210","password":"pass"}` | 400 | `VALIDATION_FAILED`, name is required |

---

## 2. Auth – Login

**Endpoint:** `POST /api/v1/auth/login`  
**Auth Required:** No

### Positive Scenarios

| # | Test Case | Request Body | Expected Status | Expected Response |
|---|-----------|-------------|-----------------|-------------------|
| P1 | Valid credentials | `{"email":"alice@test.com","password":"secret123"}` | 200 | JWT token, user object |
| P2 | Email in different case (if case-insensitive) | `{"email":"ALICE@TEST.COM","password":"secret123"}` | 200 or 401 | Depends on implementation |

### Negative Scenarios

| # | Test Case | Request Body | Expected Status | Expected Error |
|---|-----------|-------------|-----------------|----------------|
| N1 | Wrong password | `{"email":"alice@test.com","password":"wrongpass"}` | 401 | Unauthorized / bad credentials |
| N2 | Non-existent email | `{"email":"nobody@test.com","password":"pass"}` | 401 | Unauthorized |
| N3 | Missing email | `{"password":"pass"}` | 400 | `VALIDATION_FAILED`, email required |
| N4 | Missing password | `{"email":"alice@test.com"}` | 400 | `VALIDATION_FAILED`, password required |
| N5 | Invalid email format | `{"email":"invalid","password":"pass"}` | 400 | `VALIDATION_FAILED`, invalid email |
| N6 | Empty body | `{}` | 400 | `VALIDATION_FAILED` |
| N7 | Blank email string | `{"email":"","password":"pass"}` | 400 | `VALIDATION_FAILED`, email required |

---

## 3. Auth – Get Current User (me)

**Endpoint:** `GET /api/v1/auth/me`  
**Auth Required:** Yes

### Positive Scenarios

| # | Test Case | Expected Status | Expected Response |
|---|-----------|-----------------|-------------------|
| P1 | Valid JWT token | 200 | User object (id, name, email, phone, roles) |

### Negative Scenarios

| # | Test Case | Expected Status | Expected Error |
|---|-----------|-----------------|----------------|
| N1 | No Authorization header | 401 | Unauthorized |
| N2 | Expired JWT token | 401 | Unauthorized / token expired |
| N3 | Malformed token (`Bearer abc123xyz`) | 401 | Unauthorized |
| N4 | Token with tampered payload | 401 | Unauthorized |

---

## 4. Auth – Logout

**Endpoint:** `POST /api/v1/auth/logout`  
**Auth Required:** Yes

### Positive Scenarios

| # | Test Case | Expected Status | Expected Response |
|---|-----------|-----------------|-------------------|
| P1 | Valid token, logout | 200 | "Logged out successfully" |

### Negative Scenarios

| # | Test Case | Expected Status | Expected Error |
|---|-----------|-----------------|----------------|
| N1 | No Authorization header | 401 | Unauthorized |
| N2 | Expired token | 401 | Unauthorized |

> **Note:** Since JWT is stateless, token is invalidated client-side only. Verify the security context is cleared server-side.

---

## 5. Auth – Forgot Password

**Endpoint:** `POST /api/v1/auth/forgot-password`  
**Auth Required:** No

### Positive Scenarios

| # | Test Case | Request Body | Expected Status | Expected Response |
|---|-----------|-------------|-----------------|-------------------|
| P1 | Valid registered email | `{"email":"alice@test.com"}` | 200 | "OTP sent to email" |

### Negative Scenarios

| # | Test Case | Request Body | Expected Status | Expected Error |
|---|-----------|-------------|-----------------|----------------|
| N1 | Non-existent email | `{"email":"ghost@test.com"}` | 500 / 404 | "User not found" |
| N2 | Invalid email format | `{"email":"notanemail"}` | 400 | `VALIDATION_FAILED`, invalid email |
| N3 | Missing email field | `{}` | 400 | `VALIDATION_FAILED`, email required |
| N4 | Blank email | `{"email":""}` | 400 | `VALIDATION_FAILED`, email required |

---

## 6. Auth – Reset Password

**Endpoint:** `POST /api/v1/auth/reset-password`  
**Auth Required:** No

### Positive Scenarios

| # | Test Case | Request Body | Expected Status | Expected Response |
|---|-----------|-------------|-----------------|-------------------|
| P1 | Valid email, correct OTP, matching passwords | `{"email":"alice@test.com","otp":"123456","password":"newpass1","confirmPassword":"newpass1"}` | 200 | "Password reset successful" |

### Negative Scenarios

| # | Test Case | Request Body | Expected Status | Expected Error |
|---|-----------|-------------|-----------------|----------------|
| N1 | Wrong OTP | `{"email":"alice@test.com","otp":"000000","password":"newpass1","confirmPassword":"newpass1"}` | 500 / 400 | "Invalid OTP" |
| N2 | Expired OTP (after 10 min) | Valid OTP but sent after expiry | 500 / 400 | "OTP expired" |
| N3 | Password mismatch | `{"email":"alice@test.com","otp":"123456","password":"newpass1","confirmPassword":"different"}` | 500 / 400 | "Password mismatch" |
| N4 | Password less than 6 characters | `{"email":"alice@test.com","otp":"123456","password":"abc","confirmPassword":"abc"}` | 400 | `VALIDATION_FAILED`, password min 6 chars |
| N5 | Missing email | `{"otp":"123456","password":"newpass1","confirmPassword":"newpass1"}` | 400 | `VALIDATION_FAILED` |
| N6 | Missing OTP | `{"email":"alice@test.com","password":"newpass1","confirmPassword":"newpass1"}` | 400 | `VALIDATION_FAILED` |
| N7 | Missing password | `{"email":"alice@test.com","otp":"123456","confirmPassword":"newpass1"}` | 400 | `VALIDATION_FAILED` |
| N8 | Missing confirmPassword | `{"email":"alice@test.com","otp":"123456","password":"newpass1"}` | 400 | `VALIDATION_FAILED` |
| N9 | Non-existent email | `{"email":"ghost@test.com","otp":"123456","password":"newpass1","confirmPassword":"newpass1"}` | 500 / 404 | "User not found" |
| N10 | No OTP requested before reset | Valid user, no OTP set in DB | 500 / 400 | "Invalid OTP" |

---

## 7. Institute – Create

**Endpoint:** `POST /api/v1/institutes`  
**Auth Required:** Yes (any authenticated user)

### Positive Scenarios

| # | Test Case | Request Body | Expected Status | Expected Response |
|---|-----------|-------------|-----------------|-------------------|
| P1 | All valid fields | `{"name":"ABC Academy","city":"Bhubaneswar","state":"Odisha","phone":"9876543210","email":"abc@institute.com"}` | 201 | Institute object, caller assigned `INSTITUTE_ADMIN` |
| P2 | Different valid institute by same user | New unique name/email | 201 | New institute created |

### Negative Scenarios

| # | Test Case | Request Body | Expected Status | Expected Error |
|---|-----------|-------------|-----------------|----------------|
| N1 | Missing `name` | `{"city":"Bhubaneswar","state":"Odisha","phone":"9876543210","email":"a@b.com"}` | 400 | `VALIDATION_FAILED`, name required |
| N2 | Missing `city` | `{"name":"X","state":"Odisha","phone":"9876543210","email":"a@b.com"}` | 400 | `VALIDATION_FAILED`, city required |
| N3 | Missing `state` | `{"name":"X","city":"Bhubaneswar","phone":"9876543210","email":"a@b.com"}` | 400 | `VALIDATION_FAILED`, state required |
| N4 | Missing `phone` | `{"name":"X","city":"Bhubaneswar","state":"Odisha","email":"a@b.com"}` | 400 | `VALIDATION_FAILED`, phone required |
| N5 | Invalid phone format | `{"name":"X","city":"B","state":"O","phone":"12345","email":"a@b.com"}` | 400 | `VALIDATION_FAILED`, phone must be 10-15 digits |
| N6 | Missing `email` | `{"name":"X","city":"B","state":"O","phone":"9876543210"}` | 400 | `VALIDATION_FAILED`, email required |
| N7 | Invalid email format | `{"name":"X","city":"B","state":"O","phone":"9876543210","email":"notanemail"}` | 400 | `VALIDATION_FAILED`, invalid email |
| N8 | Unauthenticated request | No token | 401 | Unauthorized |
| N9 | Empty body | `{}` | 400 | `VALIDATION_FAILED`, multiple errors |

---

## 8. Institute – Get My Institutes

**Endpoint:** `GET /api/v1/institutes/my`  
**Auth Required:** Yes

### Positive Scenarios

| # | Test Case | Expected Status | Expected Response |
|---|-----------|-----------------|-------------------|
| P1 | User with one institute | 200 | Array with 1 institute |
| P2 | User with multiple institutes | 200 | Array with all institutes |
| P3 | User with no institutes | 200 | Empty array `[]` |

### Negative Scenarios

| # | Test Case | Expected Status | Expected Error |
|---|-----------|-----------------|----------------|
| N1 | No Authorization header | 401 | Unauthorized |
| N2 | Expired token | 401 | Unauthorized |

---

## 9. Institute – Get By ID

**Endpoint:** `GET /api/v1/institutes/{id}`  
**Auth Required:** Yes (must be a member of the institute)

### Positive Scenarios

| # | Test Case | Expected Status | Expected Response |
|---|-----------|-----------------|-------------------|
| P1 | Valid ID, user is a member | 200 | Institute object |

### Negative Scenarios

| # | Test Case | Expected Status | Expected Error |
|---|-----------|-----------------|----------------|
| N1 | Non-existent institute ID | 404 | "Institute not found" |
| N2 | User is NOT a member of that institute | 403 | `ACCESS_DENIED` |
| N3 | Unauthenticated request | 401 | Unauthorized |
| N4 | Non-numeric ID in path (`/api/v1/institutes/abc`) | 400 | Bad Request / type mismatch |

---

## 10. Institute – Update

**Endpoint:** `PUT /api/v1/institutes/{id}`  
**Auth Required:** Yes (must be `INSTITUTE_ADMIN` of that institute)

### Positive Scenarios

| # | Test Case | Request Body | Expected Status | Expected Response |
|---|-----------|-------------|-----------------|-------------------|
| P1 | Update name only | `{"name":"New Name"}` | 200 | Updated institute object |
| P2 | Update all optional fields | `{"name":"X","city":"Delhi","state":"Delhi","phone":"9999999999","email":"new@inst.com"}` | 200 | Updated institute |
| P3 | Update phone to valid 15-digit number | `{"phone":"999999999999999"}` | 200 | Updated |

### Negative Scenarios

| # | Test Case | Request Body | Expected Status | Expected Error |
|---|-----------|-------------|-----------------|----------------|
| N1 | Non-admin member tries to update | Valid body, non-admin user | 403 | `ACCESS_DENIED` |
| N2 | Non-member tries to update | Valid body, unrelated user | 403 | `ACCESS_DENIED` |
| N3 | Invalid email in update | `{"email":"bademail"}` | 400 | `VALIDATION_FAILED`, invalid email |
| N4 | Invalid phone in update | `{"phone":"123"}` | 400 | `VALIDATION_FAILED`, phone pattern |
| N5 | Non-existent institute ID | Valid admin token, wrong ID | 404 | Not found |
| N6 | Unauthenticated request | No token | 401 | Unauthorized |

---

## 11. Batch – Create

**Endpoint:** `POST /api/v1/batches`  
**Auth Required:** Yes (`INSTITUTE_ADMIN` of the given `instituteId`)

### Positive Scenarios

| # | Test Case | Request Body | Expected Status | Expected Response |
|---|-----------|-------------|-----------------|-------------------|
| P1 | All valid fields | `{"instituteId":1,"name":"Batch A","courseName":"JEE","startDate":"2025-01-01","endDate":"2025-12-31"}` | 201 | Batch object, `status=ACTIVE` |
| P2 | Start date equals end date | Same start & end | 201 | Created successfully |

### Negative Scenarios

| # | Test Case | Request Body | Expected Status | Expected Error |
|---|-----------|-------------|-----------------|----------------|
| N1 | Missing `instituteId` | `{"name":"B","courseName":"C","startDate":"2025-01-01","endDate":"2025-12-31"}` | 400 | `VALIDATION_FAILED`, institute ID required |
| N2 | Missing `name` | `{"instituteId":1,"courseName":"C","startDate":"2025-01-01","endDate":"2025-12-31"}` | 400 | `VALIDATION_FAILED`, batch name required |
| N3 | Missing `courseName` | `{"instituteId":1,"name":"B","startDate":"2025-01-01","endDate":"2025-12-31"}` | 400 | `VALIDATION_FAILED`, course name required |
| N4 | Missing `startDate` | `{"instituteId":1,"name":"B","courseName":"C","endDate":"2025-12-31"}` | 400 | `VALIDATION_FAILED`, start date required |
| N5 | Missing `endDate` | `{"instituteId":1,"name":"B","courseName":"C","startDate":"2025-01-01"}` | 400 | `VALIDATION_FAILED`, end date required |
| N6 | End date before start date | `{"instituteId":1,"name":"B","courseName":"C","startDate":"2025-12-31","endDate":"2025-01-01"}` | 400 | `VALIDATION_FAILED`, end date must be on or after start date |
| N7 | Non-admin member creates batch | Valid body, non-admin user | 403 | `ACCESS_DENIED` |
| N8 | Non-member creates batch | Valid body, unrelated user | 403 | `ACCESS_DENIED` |
| N9 | Non-existent `instituteId` | `{"instituteId":99999,...}` | 403 / 404 | Access denied or not found |
| N10 | Invalid date format | `{"startDate":"31-01-2025","endDate":"31-12-2025",...}` | 400 | Parsing error |
| N11 | Unauthenticated request | No token | 401 | Unauthorized |

---

## 12. Batch – Get By Institute

**Endpoint:** `GET /api/v1/batches?instituteId={id}`  
**Auth Required:** Yes (member of the institute)

### Positive Scenarios

| # | Test Case | Expected Status | Expected Response |
|---|-----------|-----------------|-------------------|
| P1 | Valid `instituteId`, user is a member | 200 | List of active batches |
| P2 | Institute exists but has no batches | 200 | Empty array `[]` |

### Negative Scenarios

| # | Test Case | Expected Status | Expected Error |
|---|-----------|-----------------|----------------|
| N1 | Missing `instituteId` param | 400 | Missing required request parameter |
| N2 | Non-member requests batches | 403 | `ACCESS_DENIED` |
| N3 | Non-existent `instituteId` | 403 / 404 | Access denied or not found |
| N4 | Unauthenticated request | 401 | Unauthorized |
| N5 | Non-numeric `instituteId` (`?instituteId=abc`) | 400 | Type mismatch error |

---

## 13. Batch – Update

**Endpoint:** `PUT /api/v1/batches/{id}`  
**Auth Required:** Yes (`INSTITUTE_ADMIN` of the batch's institute)

### Positive Scenarios

| # | Test Case | Request Body | Expected Status | Expected Response |
|---|-----------|-------------|-----------------|-------------------|
| P1 | Update batch name | `{"name":"New Batch Name"}` | 200 | Updated batch |
| P2 | Update all fields | `{"name":"X","courseName":"NEET","startDate":"2025-02-01","endDate":"2025-11-30","status":"ACTIVE"}` | 200 | Updated batch |
| P3 | Update status to `INACTIVE` | `{"status":"INACTIVE"}` | 200 | Status updated |

### Negative Scenarios

| # | Test Case | Request Body | Expected Status | Expected Error |
|---|-----------|-------------|-----------------|----------------|
| N1 | Non-admin member updates | Valid body | 403 | `ACCESS_DENIED` |
| N2 | Non-member updates | Valid body | 403 | `ACCESS_DENIED` |
| N3 | Non-existent batch ID | Valid body | 404 | Not found |
| N4 | Unauthenticated request | No token | 401 | Unauthorized |
| N5 | Non-numeric path ID | `/api/v1/batches/abc` | 400 | Type mismatch |

---

## 14. Batch – Delete

**Endpoint:** `DELETE /api/v1/batches/{id}`  
**Auth Required:** Yes (`INSTITUTE_ADMIN` of the batch's institute)

### Positive Scenarios

| # | Test Case | Expected Status | Expected Response |
|---|-----------|-----------------|-------------------|
| P1 | Admin deletes existing batch | 200 | "Batch deleted successfully", batch `status=DELETED` |
| P2 | Verify deleted batch no longer appears in `GET /api/v1/batches` | 200 (list call) | Deleted batch absent |

### Negative Scenarios

| # | Test Case | Expected Status | Expected Error |
|---|-----------|-----------------|----------------|
| N1 | Non-admin member deletes | 403 | `ACCESS_DENIED` |
| N2 | Non-member deletes | 403 | `ACCESS_DENIED` |
| N3 | Non-existent batch ID | 404 | Not found |
| N4 | Unauthenticated request | 401 | Unauthorized |
| N5 | Delete already-deleted batch | 404 / 400 | Not found or bad state |

---

## 15. Student – Create

**Endpoint:** `POST /api/v1/students`  
**Auth Required:** Yes (`INSTITUTE_ADMIN` of the given `instituteId`)

### Positive Scenarios

| # | Test Case | Request Body | Expected Status | Expected Response |
|---|-----------|-------------|-----------------|-------------------|
| P1 | All valid required fields | `{"instituteId":1,"batchId":1,"name":"John Doe","parentName":"Jane Doe","primaryPhone":"9876543210","enrollmentDate":"2025-01-15"}` | 201 | Student object, `status=ACTIVE` |
| P2 | With optional `whatsappPhone` | Add `"whatsappPhone":"9876543210"` | 201 | Student created with WhatsApp |
| P3 | WhatsApp phone is different from primary | Both phones different but valid | 201 | Created successfully |

### Negative Scenarios

| # | Test Case | Request Body | Expected Status | Expected Error |
|---|-----------|-------------|-----------------|----------------|
| N1 | Missing `instituteId` | No `instituteId` field | 400 | `VALIDATION_FAILED`, institute ID required |
| N2 | Missing `batchId` | No `batchId` field | 400 | `VALIDATION_FAILED`, batch ID required |
| N3 | Missing `name` | No `name` field | 400 | `VALIDATION_FAILED`, student name required |
| N4 | Missing `parentName` | No `parentName` field | 400 | `VALIDATION_FAILED`, parent name required |
| N5 | Missing `primaryPhone` | No `primaryPhone` field | 400 | `VALIDATION_FAILED`, primary phone required |
| N6 | Invalid `primaryPhone` (too short) | `"primaryPhone":"12345"` | 400 | `VALIDATION_FAILED`, phone must be 10-15 digits |
| N7 | Invalid `whatsappPhone` format | `"whatsappPhone":"abc"` | 400 | `VALIDATION_FAILED`, WhatsApp phone pattern |
| N8 | Missing `enrollmentDate` | No `enrollmentDate` field | 400 | `VALIDATION_FAILED`, enrollment date required |
| N9 | Invalid date format for `enrollmentDate` | `"enrollmentDate":"15-01-2025"` | 400 | Parsing/Bad request error |
| N10 | Non-admin member creates | Valid body, non-admin | 403 | `ACCESS_DENIED` |
| N11 | Non-existent `instituteId` | `"instituteId":99999` | 403 / 404 | Access denied or not found |
| N12 | Non-existent `batchId` | `"batchId":99999` | 404 | Not found |
| N13 | Unauthenticated request | No token | 401 | Unauthorized |

---

## 16. Student – Get All (with filters)

**Endpoint:** `GET /api/v1/students?instituteId={id}[&batchId={id}]`  
**Auth Required:** Yes (member of the institute)

### Positive Scenarios

| # | Test Case | Query Params | Expected Status | Expected Response |
|---|-----------|-------------|-----------------|-------------------|
| P1 | All students in institute | `?instituteId=1` | 200 | Full list of students |
| P2 | Students filtered by batch | `?instituteId=1&batchId=1` | 200 | Students of that batch only |
| P3 | No students in institute | `?instituteId=X` (empty) | 200 | Empty array |
| P4 | No students in batch filter | `?instituteId=1&batchId=999` | 200 | Empty array |

### Negative Scenarios

| # | Test Case | Expected Status | Expected Error |
|---|-----------|-----------------|----------------|
| N1 | Missing `instituteId` param | 400 | Required request parameter missing |
| N2 | Non-member requesting students | 403 | `ACCESS_DENIED` |
| N3 | Non-existent `instituteId` | 403 / 404 | Access denied or not found |
| N4 | Unauthenticated request | 401 | Unauthorized |
| N5 | Non-numeric `instituteId` | 400 | Type mismatch |

---

## 17. Student – Get By ID

**Endpoint:** `GET /api/v1/students/{id}`  
**Auth Required:** Yes (member of student's institute)

### Positive Scenarios

| # | Test Case | Expected Status | Expected Response |
|---|-----------|-----------------|-------------------|
| P1 | Valid student ID, user is a member | 200 | Student object |

### Negative Scenarios

| # | Test Case | Expected Status | Expected Error |
|---|-----------|-----------------|----------------|
| N1 | Non-existent student ID | 404 | Not found |
| N2 | User not a member of student's institute | 403 | `ACCESS_DENIED` |
| N3 | Unauthenticated request | 401 | Unauthorized |
| N4 | Non-numeric path ID (`/students/abc`) | 400 | Type mismatch |

---

## 18. Student – Bulk Import (JSON)

**Endpoint:** `POST /api/v1/students/bulk-import`  
**Auth Required:** Yes (`INSTITUTE_ADMIN` of the given `instituteId`)

### Positive Scenarios

| # | Test Case | Request Body | Expected Status | Expected Response |
|---|-----------|-------------|-----------------|-------------------|
| P1 | Valid list with 1 student | `{"instituteId":1,"batchId":1,"students":[{"name":"S1","parentName":"P1","primaryPhone":"9876543210","enrollmentDate":"2025-01-01"}]}` | 201 | Import summary, successCount=1, failureCount=0 |
| P2 | Valid list with multiple students (e.g., 5) | Multiple valid entries | 201 | All imported |
| P3 | Mix of valid and invalid students (partial import) | Some rows with bad phone | 201 | Partial import: successCount + failureCount reported |
| P4 | Student with optional `whatsappPhone` | Include `whatsappPhone` in one entry | 201 | Imported successfully |

### Negative Scenarios

| # | Test Case | Request Body | Expected Status | Expected Error |
|---|-----------|-------------|-----------------|----------------|
| N1 | Empty students list | `{"instituteId":1,"batchId":1,"students":[]}` | 400 | `VALIDATION_FAILED`, student list must not be empty |
| N2 | Missing `instituteId` | No `instituteId` | 400 | `VALIDATION_FAILED` |
| N3 | Missing `batchId` | No `batchId` | 400 | `VALIDATION_FAILED` |
| N4 | Student entry missing `name` | `{"students":[{"parentName":"P","primaryPhone":"9876543210","enrollmentDate":"2025-01-01"}]}` | 400 | `VALIDATION_FAILED` |
| N5 | Student entry with invalid phone | `{"primaryPhone":"123"}` in student | 400 | `VALIDATION_FAILED` |
| N6 | Student entry missing `enrollmentDate` | No `enrollmentDate` | 400 | `VALIDATION_FAILED` |
| N7 | Non-admin member | Valid body, non-admin user | 403 | `ACCESS_DENIED` |
| N8 | Non-existent `instituteId` | `"instituteId":99999` | 403 / 404 | Access denied or not found |
| N9 | Unauthenticated request | No token | 401 | Unauthorized |
| N10 | `null` inside students array | `{"students":[null]}` | 400 | Bad request / null processing |

---

## 19. Student – Bulk Import (File)

**Endpoint:** `POST /api/v1/students/bulk-import/file`  
**Auth Required:** Yes (`INSTITUTE_ADMIN` of the given `instituteId`)  
**Content-Type:** `multipart/form-data`  
**Expected File Columns:** `name`, `parent_name`, `primary_phone`, `whatsapp_phone`, `enrollment_date` (format: `yyyy-MM-dd`)

### Positive Scenarios

| # | Test Case | Input | Expected Status | Expected Response |
|---|-----------|-------|-----------------|-------------------|
| P1 | Valid CSV file with correct columns | 3-row CSV, all valid | 201 | Import summary, successCount=3 |
| P2 | Valid XLSX file with correct columns | Excel with 5 rows | 201 | All imported |
| P3 | CSV with optional `whatsapp_phone` blank | Leave column empty | 201 | Imported without WhatsApp |
| P4 | Large file (100+ students) | CSV with 100 rows | 201 | All/partial import with summary |

### Negative Scenarios

| # | Test Case | Input | Expected Status | Expected Error |
|---|-----------|-------|-----------------|----------------|
| N1 | Missing `instituteId` param | No `?instituteId=` | 400 | Missing required param |
| N2 | Missing `batchId` param | No `?batchId=` | 400 | Missing required param |
| N3 | Unsupported file type (.pdf, .txt) | PDF file | 400 | Unsupported file format |
| N4 | File with missing required column (`name`) | CSV without `name` column | 400 / 201 | Error or partial fail |
| N5 | File with wrong date format | `enrollment_date` as `15-01-2025` | 400 / 201 | Parse error or row failure |
| N6 | Empty file (no rows, only header) | CSV with header only | 201 / 400 | 0 imported |
| N7 | File with invalid phone numbers | `primary_phone` = `"abc"` | 201 | Row fails, failure summary |
| N8 | No file part attached | Request without file | 400 | Missing file part |
| N9 | Non-admin user | Valid file, non-admin | 403 | `ACCESS_DENIED` |
| N10 | Unauthenticated request | No token | 401 | Unauthorized |
| N11 | Non-existent `batchId` param | `?batchId=99999` | 404 | Batch not found |

---

## 20. Cross-Cutting: Security & Authorization

These tests apply across all protected endpoints and validate the security layer independently of business logic.

| # | Test Case | Endpoint Example | Expected Status | Expected Error |
|---|-----------|-----------------|-----------------|----------------|
| S1 | Request with no `Authorization` header | Any protected endpoint | 401 | Unauthorized |
| S2 | Request with `Authorization: Bearer ` (empty token) | Any protected endpoint | 401 | Unauthorized |
| S3 | Request with expired JWT | Any protected endpoint | 401 | Token expired / Unauthorized |
| S4 | Request with JWT signed with wrong secret | Any protected endpoint | 401 | Invalid signature |
| S5 | Request with `Authorization: Basic abc` (wrong scheme) | Any protected endpoint | 401 | Unauthorized |
| S6 | `STUDENT` role tries to create an institute batch | `POST /api/v1/batches` | 403 | `ACCESS_DENIED` |
| S7 | `STUDENT` role tries to create a student | `POST /api/v1/students` | 403 | `ACCESS_DENIED` |
| S8 | `INSTITUTE_ADMIN` of Institute A tries to update Institute B | `PUT /api/v1/institutes/B_ID` | 403 | `ACCESS_DENIED` |
| S9 | `INSTITUTE_ADMIN` of Institute A tries to delete Batch in Institute B | `DELETE /api/v1/batches/B_BATCH_ID` | 403 | `ACCESS_DENIED` |
| S10 | Non-member user tries to list students of an institute | `GET /api/v1/students?instituteId=X` | 403 | `ACCESS_DENIED` |
| S11 | Non-member tries to get institute details | `GET /api/v1/institutes/{id}` | 403 | `ACCESS_DENIED` |
| S12 | JWT token reused after logout | `GET /api/v1/auth/me` post-logout | 200 (stateless) | Token is still valid server-side; verify client removes it |

---

## Appendix: Test Data Setup

### Prerequisite Order
1. **Register** a user (becomes admin)
2. **Create an Institute** → note `instituteId`
3. **Create a Batch** under that institute → note `batchId`
4. **Create Students** under that batch

### Roles Used in Testing
| Role | Description |
|------|-------------|
| `STUDENT` | Default role, read-only for their own data |
| `INSTITUTE_ADMIN` | Full CRUD on institute, batches, and students |

### Sample Tokens Needed
- Token A: `INSTITUTE_ADMIN` of Institute 1
- Token B: `INSTITUTE_ADMIN` of Institute 2 (different institute)
- Token C: `STUDENT` (member of Institute 1)
- Token D: Unrelated user (no institute membership)

---

*Generated for FeeFlow Backend — April 2026*
