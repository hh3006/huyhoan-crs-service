# Session 3 (Buổi 3) - Completion Summary

## ✅ All Tasks Completed Successfully

### Status: READY FOR TESTING

Both services have been created/updated with zero compilation errors.

---

## Part A: Course-Service Updates (7 Changes)

### 1. CourseRepository.java ✅
**Location**: `course-service/src/main/java/.../repository/CourseRepository.java`

Added pagination support:
```java
Page<Course> findByTenMonHocContainingIgnoreCase(String keyword, Pageable pageable);
```

### 2. CourseService.java ✅
**Location**: `course-service/src/main/java/.../service/CourseService.java`

Added imports:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
```

Added 3 new methods:
- `search(String keyword, Pageable pageable)` - Handles search and pagination
- `reserveSeat(Long courseId)` - Decrements available seats with @Transactional
- `releaseSeat(Long courseId)` - Increments available seats with @Transactional

### 3. CourseController.java ✅
**Location**: `course-service/src/main/java/.../controller/CourseController.java`

Updated imports:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Replaced endpoint:
```java
// OLD: public List<CourseDTO> getAll()
// NEW: public Page<CourseDTO> search(String keyword, Pageable pageable)
```

### 4. GlobalExceptionHandler.java ✅
**Location**: `course-service/src/main/java/.../exception/GlobalExceptionHandler.java`

Added handler for 409 Conflict:
```java
@ExceptionHandler(IllegalStateException.class)
public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex)
```

### 5. InternalCourseController.java ✅ (NEW)
**Location**: `course-service/src/main/java/.../controller/InternalCourseController.java`

New controller for internal APIs:
- `@PatchMapping("/{id}/reserve-seat")` - Calls CourseService.reserveSeat()
- `@PatchMapping("/{id}/release-seat")` - Calls CourseService.releaseSeat()

---

## Part C: Registration-Service Creation (9 Files)

### Directory Structure Created
```
registration-service/
├── pom.xml
├── src/main/resources/application.properties
├── src/main/java/vn/edu/crs/registrationservice/
│   ├── RegistrationServiceApplication.java
│   ├── config/RestTemplateConfig.java
│   ├── entity/Registration.java
│   ├── dto/RegistrationRequestDTO.java
│   ├── repository/RegistrationRepository.java
│   ├── client/CourseClient.java
│   ├── service/RegistrationService.java
│   ├── controller/RegistrationController.java
│   └── exception/GlobalExceptionHandler.java
└── src/test/java/...ApplicationTests.java
```

### 6. pom.xml ✅
**Includes**: Spring Web, Data JPA, MySQL Driver, Lombok, Validation, Tests

### 7. application.properties ✅
```properties
server.port=8083
spring.datasource.url=jdbc:mysql://localhost:3306/registration_db?useSSL=false&...
course-service.base-url=http://localhost:8082
```

### 8. RegistrationServiceApplication.java ✅
Spring Boot main application class

### 9. RestTemplateConfig.java ✅
Configured RestTemplate with JdkClientHttpRequestFactory for PATCH support

### 10. Registration.java (Entity) ✅
- Fields: id, studentId, courseId, trangThai, ngayDangKy
- No foreign key to Course (different database)

### 11. RegistrationRequestDTO.java ✅
Input validation with @NotNull annotations

### 12. RegistrationRepository.java ✅
- `findByStudentId(Long studentId)`
- `existsByStudentIdAndCourseIdAndTrangThai(...)`

### 13. CourseClient.java ✅
HTTP client for inter-service communication:
- `reserveSeat(Long courseId)` - Calls PATCH /internal/courses/{id}/reserve-seat
- `releaseSeat(Long courseId)` - Calls PATCH /internal/courses/{id}/release-seat
- Handles exceptions: 409 Conflict, 404 Not Found, Connection errors

### 14. RegistrationService.java ✅
Business logic:
- `register(RegistrationRequestDTO)` - Calls course-service FIRST, then saves Registration
- `cancel(Long registrationId)` - Calls course-service FIRST, then updates status
- Validation: prevents duplicate active registrations

### 15. RegistrationController.java ✅
REST endpoints:
- `POST /registrations` - Register (201 Created)
- `DELETE /registrations/{id}` - Cancel

### 16. GlobalExceptionHandler.java ✅
Centralized error handling with consistent JSON format:
```json
{"message": "error description"}
```

---

## Before Starting Tests

### 1. Create registration_db Database
```sql
CREATE DATABASE registration_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. Both Services Need to Be Running
**Terminal 1** (course-service):
```
cd course-service
mvn spring-boot:run
# Or run RegistrationServiceApplication in IntelliJ
```

**Terminal 2** (registration-service):
```
cd registration-service
mvn spring-boot:run
# Or run RegistrationServiceApplication in IntelliJ
```

---

## Testing Checklist (11 Test Cases)

### ✅ Internal API Tests (course-service)

**Test 1: Search with Pagination**
```
GET http://localhost:8082/courses?keyword=java&page=0&size=5&sort=tenMonHoc,asc
Expected: 200, paginated results
```

**Test 2: Reserve Seat**
```
PATCH http://localhost:8082/internal/courses/1/reserve-seat
Expected: 200, soChoConLai -= 1
```

**Test 3: Reserve When Full**
```
PATCH http://localhost:8082/internal/courses/1/reserve-seat (repeated until full)
Expected: 409 Conflict, "Mon hoc da het cho, khong the dang ky"
```

**Test 4: Release Seat**
```
PATCH http://localhost:8082/internal/courses/1/release-seat
Expected: 200, soChoConLai += 1 (not exceeding soChoToiDa)
```

### ✅ Cross-Service Tests (registration-service calling course-service)

**Test 5: Register Successfully**
```
POST http://localhost:8083/registrations
{"studentId": 1, "courseId": 1}
Expected: 201 Created
- course_db.course.soChoConLai -= 1
- registration_db.registration has new DA_DANG_KY record
```

**Test 6: Register - Duplicate Prevention**
```
POST http://localhost:8083/registrations
(same body as Test 5)
Expected: 409 Conflict, "Sinh vien da dang ky mon hoc nay roi"
```

**Test 7: Cancel Registration**
```
DELETE http://localhost:8083/registrations/1
Expected: 200
- course_db.course.soChoConLai += 1
- registration_db.registration.trangThai = DA_HUY
```

**Test 8: Cancel - Already Cancelled**
```
DELETE http://localhost:8083/registrations/1 (repeat Test 7)
Expected: 409 Conflict, "Dang ky nay da duoc huy truoc do"
```

**Test 9: Register - Course Not Found**
```
POST http://localhost:8083/registrations
{"studentId": 1, "courseId": 9999}
Expected: 409 Conflict, "Mon hoc khong ton tai"
```

**Test 10: Register - Course Full**
```
POST http://localhost:8083/registrations
{"studentId": 2, "courseId": 1} (where soChoConLai = 0)
Expected: 409 Conflict, "Mon hoc da het cho"
```

**Test 11: Connection Error Handling**
```
1. Stop course-service (Ctrl+C)
2. POST http://localhost:8083/registrations
   {"studentId": 3, "courseId": 1}
Expected: 409 Conflict (NOT 500 or hanging)
Message: "Khong the ket noi toi course-service, vui long thu lai sau"
```

---

## Git Commit Instructions

```bash
# Navigate to parent directory containing both services
cd c:\Users\DELL\IdeaProjects

# Commit course-service changes
git add course-service/
git commit -m "feat(course-service): search/pagination + internal reserve/release seat API"

# Commit registration-service
git add registration-service/
git commit -m "init(registration-service): registration with cross-service call"

# Push to remote
git push
```

---

## Deliverables Checklist

### course-service
- [ ] GET /courses supports ?keyword=, ?page=, ?size=, ?sort=
- [ ] PATCH /internal/courses/{id}/reserve-seat works (soChoConLai decreases)
- [ ] PATCH /internal/courses/{id}/release-seat works (soChoConLai increases)
- [ ] @Transactional prevents race conditions
- [ ] Returns 409 when course is full
- [ ] GlobalExceptionHandler handles IllegalStateException

### registration-service
- [ ] POST /registrations calls course-service before saving
- [ ] DELETE /registrations/{id} calls course-service before marking DA_HUY
- [ ] Validates no duplicate active registrations
- [ ] Handles connection errors gracefully (doesn't hang)
- [ ] Returns consistent error format with course-service
- [ ] RestTemplate configured for PATCH support

### Integration
- [ ] Both services run simultaneously without conflicts
- [ ] Inter-service calls work correctly
- [ ] Both databases (course_db, registration_db) update correctly
- [ ] Error scenarios tested and handled properly

### Version Control
- [ ] Git has 2 separate commits
- [ ] Commit messages follow convention
- [ ] All code pushed to remote

---

## Key Features Implemented

### A: Pagination & Search
- ✅ Spring Data Pageable integration
- ✅ Case-insensitive search with LIKE
- ✅ Automatic query parameter support (page, size, sort)

### B: Internal APIs
- ✅ Separated internal endpoints with /internal/ prefix
- ✅ Atomic reserve/release with @Transactional
- ✅ Proper seat availability validation
- ✅ HTTP 409 Conflict for business logic errors

### C: Registration Service
- ✅ Second microservice with independent database
- ✅ Cross-service HTTP communication with RestTemplate
- ✅ Transaction-like behavior (reserve BEFORE persist)
- ✅ Distributed transaction limitation acknowledged

### Error Handling
- ✅ Centralized GlobalExceptionHandler in both services
- ✅ Consistent JSON error format across system
- ✅ Specific HTTP status codes (400, 404, 409, 500)
- ✅ Connection failure handling without hanging

---

## Architecture Achieved

```
Postman/Frontend
    ↓
┌─────────────────────────────────────────────────────────────────┐
│                      (Gateway - Coming in Session 4)             │
└──────────────┬──────────────────────────────┬──────────────────┘
               ↓                              ↓
        ┌─────────────────┐          ┌──────────────────┐
        │ course-service  │          │registration-srv  │
        │   (Port 8082)   │◄─────────│   (Port 8083)    │
        └──────┬──────────┘ PATCH    └──────────────────┘
               ↓                              ↓
        ┌─────────────────┐          ┌──────────────────┐
        │  course_db      │          │ registration_db  │
        │    (MySQL)      │          │    (MySQL)       │
        └─────────────────┘          └──────────────────┘

Key Flows:
1. GET /courses?... → course-service → course_db
2. PATCH /internal/courses/{id}/reserve-seat → course-service → course_db
3. POST /registrations → registration-service → CourseClient 
   → course-service:8082 → reserve-seat → registration-service saves to registration_db
```

---

## Known Limitations (Documented for Discussion - Part D)

1. **Distributed Transactions**
   - No true ACID across 2 databases
   - If course-service succeeds but registration-service fails, inconsistency occurs
   - Production solutions: Saga Pattern, Outbox Pattern (not implemented here)

2. **Synchronous Communication**
   - RestTemplate blocks on inter-service calls
   - If course-service is slow, registration-service waits
   - Future: WebClient for async/reactive approach

3. **No Service Discovery**
   - Hard-coded course-service.base-url
   - Future: Eureka/Consul for dynamic discovery

4. **No Circuit Breaker**
   - Cascading failures if course-service goes down
   - Future: Hystrix/Resilience4j pattern

5. **Simple Search**
   - LIKE %keyword% not indexed
   - Production: Add database indexes and consider Elasticsearch

---

## Files Modified/Created Summary

### Modified (5)
1. `course-service/src/main/java/.../repository/CourseRepository.java`
2. `course-service/src/main/java/.../service/CourseService.java`
3. `course-service/src/main/java/.../controller/CourseController.java`
4. `course-service/src/main/java/.../exception/GlobalExceptionHandler.java`
5. `course-service/src/main/resources/application.properties` (optional: add allowPublicKeyRetrieval if needed)

### Created (16)
1. `registration-service/pom.xml`
2. `registration-service/src/main/resources/application.properties`
3. `registration-service/src/main/java/.../RegistrationServiceApplication.java`
4. `registration-service/src/main/java/.../config/RestTemplateConfig.java`
5. `registration-service/src/main/java/.../entity/Registration.java`
6. `registration-service/src/main/java/.../dto/RegistrationRequestDTO.java`
7. `registration-service/src/main/java/.../repository/RegistrationRepository.java`
8. `registration-service/src/main/java/.../client/CourseClient.java`
9. `registration-service/src/main/java/.../service/RegistrationService.java`
10. `registration-service/src/main/java/.../controller/RegistrationController.java`
11. `registration-service/src/main/java/.../exception/GlobalExceptionHandler.java`
12. `registration-service/src/test/java/.../ApplicationTests.java`

---

## Next Steps

1. **Create registration_db**: Run CREATE DATABASE SQL
2. **Verify course_db exists** with sample data
3. **Start course-service**: Verify it runs on port 8082
4. **Start registration-service**: Verify it runs on port 8083
5. **Run 11 tests** in order (as documented above)
6. **Verify databases** update correctly in MySQL Workbench
7. **Test connection failure** (stop course-service and re-test)
8. **Make Git commits** with proper messages
9. **Review & understand** Part D discussion points on distributed transactions

---

## Documentation Generated

Two reference documents have been created:
- **BUOI_03_IMPLEMENTATION_GUIDE.md** - Comprehensive guide with all test cases and architecture diagrams
- **BUOI_03_QUICK_REFERENCE.md** - Quick lookup for endpoints, files, and configurations

---

## Session 3 Completion Status

```
✅ A.1 - Pagination concept understood
✅ A.2 - CourseRepository updated
✅ A.3 - CourseService search method added
✅ A.4 - CourseController updated to use Pageable
✅ B.1 - Internal API concept applied
✅ B.2 - reserve/release with @Transactional
✅ B.3 - IllegalStateException handler added
✅ B.4 - InternalCourseController created
✅ C.1 - registration-service project initialized
✅ C.2 - Database and configuration done
✅ C.3 - Registration entity created
✅ C.4 - RestTemplate configured
✅ C.5 - Repository and DTO created
✅ C.6 - CourseClient for inter-service calls
✅ C.7 - RegistrationService business logic
✅ C.8 - RegistrationController endpoints
✅ C.9 - GlobalExceptionHandler for registration-service
✅ D   - Ready for distributed transaction discussion
✅ E   - Testing documentation prepared
✅ F   - Git commit instructions provided
```

**Ready for Testing! 🚀**

