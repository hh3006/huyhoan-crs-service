# Session 3 (Buổi 3) - Pagination/Search & Registration Service Implementation Guide

## Summary of Changes

### Part A: course-service Updates

✅ **A.1-A.2: CourseRepository** - Added pagination method
- `Page<Course> findByTenMonHocContainingIgnoreCase(String keyword, Pageable pageable)`

✅ **A.3: CourseService** - Added search method with pagination
- `public Page<CourseDTO> search(String keyword, Pageable pageable)`
- Supports keyword search and handles null/blank keyword

✅ **A.4: CourseController** - Replaced GET /courses endpoint
- Changed from `List<CourseDTO> getAll()` to `Page<CourseDTO> search(String keyword, Pageable pageable)`
- Automatically supports: `?keyword=`, `?page=`, `?size=`, `?sort=`

✅ **B.2: CourseService** - Added reserve/release seat methods
- `@Transactional public CourseDTO reserveSeat(Long courseId)` - Decreases available seats
- `@Transactional public CourseDTO releaseSeat(Long courseId)` - Increases available seats
- Both include checks for seat availability

✅ **B.3: GlobalExceptionHandler** - Added IllegalStateException handler
- Returns HTTP 409 Conflict when course is full

✅ **B.4: InternalCourseController** - Created internal API controller
- `PATCH /internal/courses/{id}/reserve-seat`
- `PATCH /internal/courses/{id}/release-seat`

### Part C: registration-service Creation

✅ **C.1-C.2: Project Setup**
- Created new Spring Boot project with dependencies (Spring Web, Data JPA, MySQL, Lombok, Validation)
- Configured server port: 8083
- Configured MySQL connection to registration_db

✅ **C.3: Registration Entity**
- Fields: id, studentId, courseId, trangThai (DA_DANG_KY/DA_HUY), ngayDangKy
- Note: courseId is stored as number only (no foreign key across databases)

✅ **C.4: RestTemplateConfig**
- Configured RestTemplate with JdkClientHttpRequestFactory to support PATCH requests

✅ **C.5: RegistrationRepository & DTO**
- Repository methods: findByStudentId, existsByStudentIdAndCourseIdAndTrangThai
- DTO: RegistrationRequestDTO with validation

✅ **C.6: CourseClient**
- Encapsulates HTTP calls to course-service
- Methods: reserveSeat, releaseSeat
- Exception handling for connection errors, not found, and conflict scenarios

✅ **C.7: RegistrationService**
- `register()`: Calls course-service to reserve seat BEFORE saving Registration
- `cancel()`: Calls course-service to release seat BEFORE changing status to DA_HUY
- Validates no duplicate active registrations

✅ **C.8: RegistrationController**
- `POST /registrations`: Register for a course (returns 201 Created)
- `DELETE /registrations/{id}`: Cancel registration

✅ **C.9: GlobalExceptionHandler**
- Consistent error format with course-service: `{"message": "..."}`

---

## Database Setup

### Create registration_db
```sql
CREATE DATABASE registration_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## Testing Instructions (with Postman)

### Prerequisites
1. Both services are running (course-service on 8082, registration-service on 8083)
2. Both databases exist and are accessible
3. course_db has sample course data
4. MySQL Workbench open with both databases for visual verification

### Test Cases (in order)

#### Test 1: Search with Pagination
```
GET http://localhost:8082/courses?keyword=laptrinh&page=0&size=5&sort=tenMonHoc,asc
```
✅ Expected: Returns paginated results with search criteria

---

#### Test 2: Internal API - Reserve Seat (Success)
```
PATCH http://localhost:8082/internal/courses/1/reserve-seat
```
✅ Expected: 200 OK, soChoConLai decreases by 1

---

#### Test 3: Internal API - Reserve Seat (Conflict)
```
PATCH http://localhost:8082/internal/courses/1/reserve-seat
(Repeat until soChoConLai = 0)
```
✅ Expected: 409 Conflict with message "Mon hoc da het cho, khong the dang ky"

---

#### Test 4: Internal API - Release Seat
```
PATCH http://localhost:8082/internal/courses/1/release-seat
```
✅ Expected: 200 OK, soChoConLai increases by 1 (not exceeding soChoToiDa)

---

#### Test 5: Register for Course (Cross-Service Call)
```
POST http://localhost:8083/registrations
Content-Type: application/json

{
  "studentId": 1,
  "courseId": 1
}
```
✅ Expected: 201 Created
- course_db.course.soChoConLai decreases by 1
- registration_db.registration has new record with trangThai = "DA_DANG_KY"

---

#### Test 6: Register - Duplicate Check
```
POST http://localhost:8083/registrations
(Same body as Test 5)
```
✅ Expected: 409 Conflict with message "Sinh vien da dang ky mon hoc nay roi"

---

#### Test 7: Cancel Registration
```
DELETE http://localhost:8083/registrations/1
```
✅ Expected: 200 OK
- course_db.course.soChoConLai increases by 1
- registration_db.registration.trangThai changes to "DA_HUY"

---

#### Test 8: Cancel - Already Cancelled
```
DELETE http://localhost:8083/registrations/1
(Repeat Test 7)
```
✅ Expected: 409 Conflict with message "Dang ky nay da duoc huy truoc do"

---

#### Test 9: Register - Course Doesn't Exist
```
POST http://localhost:8083/registrations

{
  "studentId": 1,
  "courseId": 9999
}
```
✅ Expected: 409 Conflict with message "Mon hoc khong ton tai"

---

#### Test 10: Register - Course Full
```
POST http://localhost:8083/registrations

{
  "studentId": 2,
  "courseId": 1  (where soChoConLai = 0)
}
```
✅ Expected: 409 Conflict with message "Mon hoc da het cho"

---

#### Test 11: Connection Error Handling
1. Stop course-service (Ctrl+C in IntelliJ)
2. Execute:
```
POST http://localhost:8083/registrations

{
  "studentId": 3,
  "courseId": 1
}
```
✅ Expected: 409 Conflict with message "Khong the ket noi toi course-service, vui long thu lai sau"
⚠️ Should NOT hang indefinitely

---

## Git Commits

```bash
cd crs-microservices  # or parent directory containing both services

# Commit course-service changes
git add course-service/
git commit -m "feat(course-service): search/pagination + internal reserve/release seat API"

# Commit registration-service
git add registration-service/
git commit -m "init(registration-service): registration with cross-service call"

git push
```

---

## Key Concepts Demonstrated

### Pagination (Pageable)
- Spring Data JPA automatically handles pagination
- Query params: `page` (0-indexed), `size` (items per page), `sort` (field,direction)

### Internal APIs
- Prefixed with `/internal/` to distinguish from public APIs
- Only for inter-service communication
- Better security boundaries

### @Transactional
- Ensures read-modify-write operations are atomic
- Prevents race conditions when multiple requests modify same resource
- Note: Applies only within single database

### Distributed Transactions
- This simplified implementation has a known limitation:
- If `courseClient.reserveSeat()` succeeds but `registrationRepository.save()` fails, data becomes inconsistent
- Production solutions: Saga Pattern or Outbox Pattern (not implemented in this course)

### RestTemplate for Inter-Service Calls
- JdkClientHttpRequestFactory supports PATCH (default doesn't)
- Exception mapping: HttpClientErrorException.Conflict → 409, .NotFound → 404, etc.

---

## Checklist - Final Deliverables

- [ ] GET /courses supports keyword, page, size, sort parameters
- [ ] PATCH /internal/courses/{id}/reserve-seat works with @Transactional
- [ ] PATCH /internal/courses/{id}/release-seat works correctly
- [ ] Returns 409 when attempting to reserve full course
- [ ] registration-service runs independently on port 8083
- [ ] registration_db database is separate from course_db
- [ ] POST /registrations calls course-service before persisting
- [ ] DELETE /registrations/{id} calls course-service before marking DA_HUY
- [ ] Both services have GlobalExceptionHandler with consistent error format
- [ ] Can explain distributed transaction concept and known limitations
- [ ] Tested connection failure handling (service doesn't hang)
- [ ] Git has 2 separate commits for each service

---

## Common Errors & Solutions

| Error | Cause | Solution |
|-------|-------|----------|
| 400 Bad Request on GET /courses?page=0&size=5 | Controller still returns List instead of Page | Check A.4 - update method signature |
| Can reserve negative seats | Missing @Transactional or check | See B.2 - add @Transactional and check soChoConLai <= 0 |
| Connection refused from registration-service | Wrong base-url in properties | Verify C.2: course-service.base-url=http://localhost:8082 |
| 500 on DELETE when already cancelled | Missing status check before release | See C.7 - check DA_HUY before calling releaseSeat |
| Hangs when course-service unavailable | Poor error handling in CourseClient | See C.6 - catch ResourceAccessException |

---

## Architecture Diagram

```
Frontend/Postman
    ↓
    ├─→ Gateway (will be added in Session 4)
    ↓
    ├──────────────────────────────┬─────────────────────────────┐
    ↓                              ↓                             ↓
course-service               registration-service          (other services)
   (8082)                         (8083)
    ├─ GET /courses (public)       ├─ POST /registrations        
    ├─ GET /courses/{id}           ├─ DELETE /registrations/{id}
    ├─ POST /courses               └─→ CourseClient
    ├─ PUT /courses/{id}               └─→ PATCH /internal/courses/{id}/reserve-seat
    ├─ DELETE /courses/{id}            └─→ PATCH /internal/courses/{id}/release-seat
    └─ PATCH /internal/courses/{id}/reserve-seat
       PATCH /internal/courses/{id}/release-seat
           ↓
        course_db          registration_db
        (MySQL)            (MySQL)
```

---

## Next Steps (Session 4)

- Add API Gateway (Spring Cloud Gateway)
- Implement security with JWT tokens
- Add rate limiting and circuit breaker patterns
- Implement notification service for confirmations

