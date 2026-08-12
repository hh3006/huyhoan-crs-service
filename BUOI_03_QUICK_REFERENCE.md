# BUỔI 3 - Quick Reference Guide

## Course-Service (Port 8082)

### Modified Files

1. **CourseRepository.java** - Added pagination
   ```java
   Page<Course> findByTenMonHocContainingIgnoreCase(String keyword, Pageable pageable);
   ```

2. **CourseService.java** - Added 3 new methods
   ```java
   public Page<CourseDTO> search(String keyword, Pageable pageable)  // A.3
   @Transactional public CourseDTO reserveSeat(Long courseId)        // B.2
   @Transactional public CourseDTO releaseSeat(Long courseId)        // B.2
   ```

3. **CourseController.java** - Changed endpoint
   ```
   Before: @GetMapping public List<CourseDTO> getAll()
   After:  @GetMapping public Page<CourseDTO> search(String keyword, Pageable pageable)
   ```

4. **GlobalExceptionHandler.java** - Added 409 handler
   ```java
   @ExceptionHandler(IllegalStateException.class)
   public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex)
   ```

### New Files

5. **InternalCourseController.java** - New controller for internal APIs
   ```java
   @RequestMapping("/internal/courses")
   - PATCH /{id}/reserve-seat
   - PATCH /{id}/release-seat
   ```

---

## Registration-Service (Port 8083) - NEW

### Project Structure
```
registration-service/
├── pom.xml                                    # Dependencies
├── src/main/resources/
│   └── application.properties                 # Config (port 8083, DB: registration_db)
├── src/main/java/vn/edu/crs/registrationservice/
│   ├── RegistrationServiceApplication.java    # Main app
│   ├── config/
│   │   └── RestTemplateConfig.java            # RestTemplate with PATCH support
│   ├── entity/
│   │   └── Registration.java                  # JPA entity
│   ├── dto/
│   │   └── RegistrationRequestDTO.java        # Input validation
│   ├── repository/
│   │   └── RegistrationRepository.java        # Data access
│   ├── client/
│   │   └── CourseClient.java                  # Inter-service HTTP calls
│   ├── service/
│   │   └── RegistrationService.java           # Business logic
│   ├── controller/
│   │   └── RegistrationController.java        # REST endpoints
│   └── exception/
│       └── GlobalExceptionHandler.java        # Error handling
└── src/test/
    └── RegistrationServiceApplicationTests.java
```

### Key Endpoints

#### Public APIs
- `POST /registrations` - Register for a course
- `DELETE /registrations/{id}` - Cancel registration

#### Internal APIs (for inter-service)
- `PATCH /internal/courses/{id}/reserve-seat` (called by registration-service)
- `PATCH /internal/courses/{id}/release-seat` (called by registration-service)

---

## Database Schemas

### course_db (existing)
```sql
CREATE TABLE course (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ten_mon_hoc VARCHAR(255) NOT NULL,
    so_tin_chi INT NOT NULL,
    so_cho_toi_da INT NOT NULL,
    so_cho_con_lai INT NOT NULL
);
```

### registration_db (new)
```sql
CREATE DATABASE registration_db;
CREATE TABLE registration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    ngay_dang_ky DATETIME NOT NULL
);
```

---

## Configuration Files

### application.properties (course-service)
```properties
server.port=8082
spring.datasource.url=jdbc:mysql://localhost:3306/course_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh
```

### application.properties (registration-service)
```properties
server.port=8083
spring.datasource.url=jdbc:mysql://localhost:3306/registration_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ho_Chi_Minh
course-service.base-url=http://localhost:8082
```

---

## Example API Calls (Postman)

### Course Search with Pagination
```
GET http://localhost:8082/courses?keyword=java&page=0&size=10&sort=tenMonHoc,asc
```

### Reserve a Seat (Internal - called by registration-service)
```
PATCH http://localhost:8082/internal/courses/1/reserve-seat
```

### Register for Course
```
POST http://localhost:8083/registrations
Content-Type: application/json

{
  "studentId": 1,
  "courseId": 1
}
```

### Cancel Registration
```
DELETE http://localhost:8083/registrations/1
```

---

## Testing Flow

1. ✅ Start course-service (port 8082)
2. ✅ Start registration-service (port 8083)
3. ✅ Test course-service endpoints (search, reserve, release)
4. ✅ Test registration-service endpoints (register, cancel)
5. ✅ Verify both databases update correctly
6. ✅ Test error scenarios (full course, duplicate, connection error)
7. ✅ Test connection failure handling (stop course-service)

---

## Key Annotations Used

### @Transactional
- **Where**: CourseService.reserveSeat(), releaseSeat()
- **Why**: Ensures atomic read-modify-write operations, prevents race conditions
- **Scope**: Single database only

### @RequestParam(required = false)
- **Where**: CourseController.search()
- **Why**: Makes keyword parameter optional

### @Value
- **Where**: CourseClient
- **Why**: Injects configuration value from application.properties

### @Component vs @Service
- **CourseClient**: @Component (utility component for HTTP calls)
- **RegistrationService**: @Service (business logic)

---

## Distributed Transaction Limitation

**Problem**: If step 1 succeeds but step 2 fails:
```
1. courseClient.reserveSeat() ✅ (course-service DB modified)
2. registrationRepository.save() ❌ (registration-service DB failed)
→ Data inconsistency
```

**Current Approach**: Accept this limitation as known issue

**Production Solutions** (not implemented):
- Saga Pattern: Compensating transactions
- Outbox Pattern: Event log with retry mechanism
- 2-Phase Commit: Complex and not recommended for microservices

---

## Common Commands

### Build Services
```bash
# Course-service
cd course-service
mvn clean install

# Registration-service
cd registration-service
mvn clean install
```

### Run Services (IntelliJ or Command Line)
```bash
# Terminal 1 - Course-service
mvn spring-boot:run

# Terminal 2 - Registration-service
mvn spring-boot:run
```

### Git Operations
```bash
git add course-service/
git commit -m "feat(course-service): search/pagination + internal reserve/release seat API"

git add registration-service/
git commit -m "init(registration-service): registration with cross-service call"

git push
```

---

## Debugging Tips

### 409 Conflict Unexpected?
- Check course has seats available (soChoConLai > 0)
- Verify @Transactional is present on reserveSeat
- Check GlobalExceptionHandler has IllegalStateException handler

### Connection Error When Services Are Running?
- Verify port numbers are correct (8082 vs 8083)
- Check course-service.base-url in registration-service properties
- Ensure both services are actually running: `netstat -ano | findstr :8082`

### Registration Not Saved?
- Check courseClient.reserveSeat() succeeds before save
- If reserve succeeds but save fails, check registration-service logs
- This is the known distributed transaction issue

### HTTP 400 on Pagination?
- Ensure CourseController uses `Pageable pageable` parameter
- Spring Data Web will parse page, size, sort parameters automatically

---

## Performance Considerations

### Pagination Impact
- **Before**: `GET /courses` returns entire list (1000+ records = slow)
- **After**: `GET /courses?page=0&size=20` returns only 20 records (fast)

### Query Optimization
- `findByTenMonHocContainingIgnoreCase` uses SQL LIKE %keyword%
- Not indexed by default; add index on tenMonHoc for production

### Inter-Service Communication
- RestTemplate is synchronous (blocks until course-service responds)
- Alternative: WebClient for asynchronous calls (advanced)

---

## Security Notes

### Internal API Protection
- Current: Any caller can access /internal/courses
- Session 4: Add API Gateway with route filtering
- Production: Implement service-to-service authentication (mTLS, service mesh)

### Database Security
- allowPublicKeyRetrieval=true is acceptable for development only
- Production: Use proper SSL certificates

---

## Next Session Preview (Buổi 4)

- Add API Gateway (Spring Cloud Gateway)
- Route /courses → course-service:8082
- Route /registrations → registration-service:8083
- Implement JWT authentication
- Add role-based access control (RBAC)

