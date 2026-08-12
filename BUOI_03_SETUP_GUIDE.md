# Session 3 - Step-by-Step Setup & Execution Guide

## Phase 1: Pre-Testing Preparation

### Step 1.1: Create registration_db Database
```sql
-- Open MySQL/HeidiSQL and execute:
CREATE DATABASE registration_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Verify:
SHOW DATABASES; -- Should show registration_db
```

### Step 1.2: Verify course_db Exists
```sql
-- Check course table has data:
SELECT * FROM course_db.course;
-- Should have at least 1 record with soChoConLai > 0
```

### Step 1.3: Check Both Services Compile
```bash
# Terminal 1 - Check course-service
cd course-service
mvn clean install
# Expected: BUILD SUCCESS

# Terminal 2 - Check registration-service
cd registration-service
mvn clean install
# Expected: BUILD SUCCESS
```

---

## Phase 2: Start Services

### Step 2.1: Start course-service (Port 8082)

**Option A: Via Maven**
```bash
cd course-service
mvn spring-boot:run
```

**Option B: IntelliJ IDEA**
1. Open `course-service` project
2. Right-click `CourseServiceApplication.java`
3. Select "Run 'CourseServiceApplication.main()'"
4. Expected output: "Started CourseServiceApplication in X seconds"

**Verification:**
```bash
# In another terminal, test:
curl http://localhost:8082/courses?page=0&size=10
# Should return: Page object with content, totalElements, etc.
```

### Step 2.2: Start registration-service (Port 8083)

**Option A: Via Maven**
```bash
cd registration-service
mvn spring-boot:run
```

**Option B: IntelliJ IDEA**
1. Open `registration-service` project (second window)
2. Right-click `RegistrationServiceApplication.java`
3. Select "Run 'RegistrationServiceApplication.main()'"
4. Expected output: "Started RegistrationServiceApplication in X seconds"

**Verification:**
```bash
# In another terminal, test:
curl -X POST http://localhost:8083/registrations \
  -H "Content-Type: application/json" \
  -d '{"studentId":1,"courseId":1}'
# Should return: 201 Created or error
```

### Step 2.3: Verify Both Services Running
```bash
# Terminal - Check port 8082
netstat -ano | findstr :8082
# Expected: LISTENING

# Terminal - Check port 8083
netstat -ano | findstr :8083
# Expected: LISTENING
```

---

## Phase 3: Postman Testing

### Setup Postman Environment
1. Open Postman
2. Create new Collection: "CRS Microservices"
3. Create new Environment: "Local Dev"
4. Set variables:
   ```
   base_url_course = http://localhost:8082
   base_url_registration = http://localhost:8083
   ```

### Test Group 1: Course Search & Pagination

**Test 1.1: Search all courses**
```
GET {{base_url_course}}/courses
```
Expected: 200 OK, empty results or all courses

---

**Test 1.2: Pagination - First page**
```
GET {{base_url_course}}/courses?page=0&size=5
```
Expected: 200 OK, 5 courses in content array

---

**Test 1.3: Pagination - Sort ascending**
```
GET {{base_url_course}}/courses?page=0&size=5&sort=tenMonHoc,asc
```
Expected: 200 OK, courses sorted by name ascending

---

**Test 1.4: Search with keyword**
```
GET {{base_url_course}}/courses?keyword=java&page=0&size=10
```
Expected: 200 OK, only courses with "java" in name (case-insensitive)

---

### Test Group 2: Internal API - Reserve/Release Seats

**Prerequisite**: Note a course ID with soChoConLai > 0 (e.g., ID=1)

---

**Test 2.1: Check initial seat count**
```
GET {{base_url_course}}/courses/1
```
Response:
```json
{
  "id": 1,
  "tenMonHoc": "Lap trinh Java",
  "soTinChi": 3,
  "soChoToiDa": 30,
  "soChoConLai": 30
}
```
Note down soChoConLai value (e.g., 30)

---

**Test 2.2: Reserve first seat**
```
PATCH {{base_url_course}}/internal/courses/1/reserve-seat
```
Expected: 200 OK
Response: soChoConLai = 29 (decreased by 1)

---

**Test 2.3: Reserve multiple seats (loop 28 times)**
```
PATCH {{base_url_course}}/internal/courses/1/reserve-seat
```
Repeat until soChoConLai = 0

---

**Test 2.4: Try to reserve when full (soChoConLai = 0)**
```
PATCH {{base_url_course}}/internal/courses/1/reserve-seat
```
Expected: 409 Conflict
Response:
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Mon hoc da het cho, khong the dang ky"
}
```

---

**Test 2.5: Release one seat**
```
PATCH {{base_url_course}}/internal/courses/1/release-seat
```
Expected: 200 OK
Response: soChoConLai = 1 (increased by 1)

---

**Test 2.6: Check seat count back to original**
```
GET {{base_url_course}}/courses/1
```
Expected: soChoConLai = 1 or whatever is current after releases

---

### Test Group 3: Registration Service - Cross-Service Calls

**Prerequisite**: course ID=2 with soChoConLai > 0

---

**Test 3.1: Open MySQL Workbench**
- Window 1: Connect to course_db
- Window 2: Connect to registration_db
- Set both to auto-refresh
- Query for Test Group 3:
  ```sql
  -- Window 1
  SELECT id, so_cho_con_lai FROM course WHERE id = 2;
  
  -- Window 2
  SELECT * FROM registration WHERE student_id = 1 AND course_id = 2;
  ```

---

**Test 3.2: Register for course (success)**
```
POST {{base_url_registration}}/registrations
Content-Type: application/json

{
  "studentId": 1,
  "courseId": 2
}
```
Expected: 201 Created
Response:
```json
{
  "id": 1,
  "studentId": 1,
  "courseId": 2,
  "trangThai": "DA_DANG_KY",
  "ngayDangKy": "2024-08-12T..."
}
```

**Verify in MySQL**:
- Window 1: course_db.course.soChoConLai decreased by 1
- Window 2: registration_db.registration has new row with id=1

---

**Test 3.3: Try to register same course twice (duplicate check)**
```
POST {{base_url_registration}}/registrations

{
  "studentId": 1,
  "courseId": 2
}
```
Expected: 409 Conflict
Response:
```json
{
  "message": "Sinh vien da dang ky mon hoc nay roi"
}
```

---

**Test 3.4: Register different student for same course**
```
POST {{base_url_registration}}/registrations

{
  "studentId": 2,
  "courseId": 2
}
```
Expected: 201 Created
**Verify**: course_db.course.soChoConLai decreased again

---

**Test 3.5: Cancel registration**
```
DELETE {{base_url_registration}}/registrations/1
```
Expected: 200 OK (no body)

**Verify in MySQL**:
- Window 1: course_db.course.soChoConLai increased back by 1
- Window 2: registration_db.registration[id=1].trangThai = "DA_HUY"

---

**Test 3.6: Try to cancel same registration twice**
```
DELETE {{base_url_registration}}/registrations/1
```
Expected: 409 Conflict
Response:
```json
{
  "message": "Dang ky nay da duoc huy truoc do"
}
```

---

### Test Group 4: Error Scenarios

**Test 4.1: Register for non-existent course**
```
POST {{base_url_registration}}/registrations

{
  "studentId": 1,
  "courseId": 99999
}
```
Expected: 409 Conflict
Response:
```json
{
  "message": "Mon hoc khong ton tai"
}
```

---

**Test 4.2: Register for course with no seats**
```
-- First, exhaust all seats of course ID=3:
PATCH {{base_url_course}}/internal/courses/3/reserve-seat (repeat until full)

-- Then try to register:
POST {{base_url_registration}}/registrations

{
  "studentId": 1,
  "courseId": 3
}
```
Expected: 409 Conflict
Response:
```json
{
  "message": "Mon hoc da het cho, khong the dang ky"
}
```

---

**Test 4.3: Missing required field**
```
POST {{base_url_registration}}/registrations
Content-Type: application/json

{
  "studentId": 1
  // Missing courseId
}
```
Expected: 400 Bad Request
Response:
```json
{
  "courseId": "courseId khong duoc de trong"
}
```

---

### Test Group 5: Connection Resilience

**Test 5.1: Service failure handling**

Step 1: Stop course-service
```
Ctrl+C in the terminal where course-service is running
Wait 2-3 seconds
```

Step 2: Try to register
```
POST {{base_url_registration}}/registrations

{
  "studentId": 1,
  "courseId": 2
}
```

Expected: 409 Conflict (NOT hanging or 500 error)
Response:
```json
{
  "message": "Khong the ket noi toi course-service, vui long thu lai sau"
}
```

Expected behavior: Response comes within 5 seconds max

---

Step 3: Restart course-service
```
Run the service again (Maven or IntelliJ)
```

Step 4: Verify service recovered
```
GET {{base_url_course}}/courses
```
Should work again

---

## Phase 4: Git Commits

### Step 4.1: Verify Changes

```bash
cd c:\Users\DELL\IdeaProjects

# Check status
git status
# Should show modified course-service files and new registration-service
```

### Step 4.2: Commit course-service

```bash
git add course-service/
git commit -m "feat(course-service): search/pagination + internal reserve/release seat API"
git log -1 --oneline
# Verify: feat(course-service): search/pagination + internal reserve/release seat API
```

### Step 4.3: Commit registration-service

```bash
git add registration-service/
git commit -m "init(registration-service): registration with cross-service call"
git log -1 --oneline
# Verify: init(registration-service): registration with cross-service call
```

### Step 4.4: Push to Remote

```bash
git push origin main
# (or master, depending on your default branch)

# Verify on GitHub/GitLab web interface
```

---

## Phase 5: Review & Discussion (Part D)

After all tests pass, discuss:

### Question: What happens if...?

**Scenario**: courseClient.reserveSeat() succeeds (seat decremented), but registrationRepository.save() fails

**Response**: 
- ✅ Seat is permanently removed from course-service database
- ❌ Registration record is NOT created in registration-service database
- 📊 Result: Data inconsistency (orphaned seat deduction)

**Why This Happens**:
- Two separate databases
- No distributed transaction across them
- Registration service depends on success of course-service

**Production Solutions** (not implemented):
1. **Saga Pattern**: Compensating transactions (automatic rollback)
2. **Outbox Pattern**: Event log with retry mechanism
3. **2-Phase Commit**: Complex, not recommended for microservices

**Our Approach**: Accept this limitation as known risk for learning purposes

---

## Troubleshooting Guide

### Issue: 400 Bad Request on pagination query
```
Symptom: GET /courses?page=0&size=5 returns 400
Cause: CourseController still returns List instead of Page
Fix: Verify CourseController has:
  public Page<CourseDTO> search(String keyword, Pageable pageable)
```

### Issue: 409 Can reserve negative seats
```
Symptom: soChoConLai goes negative
Cause: Missing @Transactional or check
Fix: Verify CourseService.reserveSeat has:
  1. @Transactional annotation
  2. if (course.getSoChoConLai() <= 0) throw IllegalStateException
```

### Issue: Connection refused from registration-service
```
Symptom: 500 error, "Connection refused"
Cause: Wrong base-url in application.properties
Fix: In registration-service/application.properties:
  course-service.base-url=http://localhost:8082
  (NOT 8083 or wrong hostname)
```

### Issue: Service hangs on connection failure
```
Symptom: Request never returns when course-service is down
Cause: CourseClient doesn't catch connection exceptions
Fix: Verify CourseClient catches:
  org.springframework.web.client.ResourceAccessException
  (indicates network/connection issues)
```

### Issue: Can cancel same registration twice
```
Symptom: DELETE /registrations/1 works twice
Cause: Missing status check before release
Fix: In RegistrationService.cancel:
  if (DA_HUY.equals(registration.getTrangThai()))
    throw new IllegalStateException("Dang ky nay da duoc huy truoc do");
```

---

## Success Criteria Checklist

### Functional Requirements
- [ ] GET /courses?page=0&size=5&sort=tenMonHoc,asc returns paginated results
- [ ] PATCH /internal/courses/{id}/reserve-seat decreases soChoConLai
- [ ] PATCH /internal/courses/{id}/release-seat increases soChoConLai
- [ ] Returns 409 when attempting to reserve full course
- [ ] Returns 409 when attempting to register already-registered student
- [ ] Returns 409 when attempting to cancel already-cancelled registration
- [ ] POST /registrations creates registration only after course-service confirms
- [ ] DELETE /registrations/{id} releases seat before marking DA_HUY

### Non-Functional Requirements
- [ ] Both services run without compilation errors
- [ ] Response times < 1 second (normal operations)
- [ ] Service doesn't hang when other service unavailable
- [ ] Consistent error format across both services
- [ ] Both databases update atomically per service

### Git Requirements
- [ ] 2 commits exist in history
- [ ] Commit messages follow convention
- [ ] All changes pushed to remote
- [ ] Can view commits on GitHub/GitLab

---

## Performance Benchmarks (Optional)

### Test Execution Times
- Search query: < 500ms
- Reserve seat: < 300ms
- Register (cross-service): < 800ms
- Cancel: < 400ms

### Load Testing (Optional, for curiosity)
```bash
# Using Apache Bench
ab -n 100 -c 10 "http://localhost:8082/courses"

# Using Wrk
wrk -t4 -c100 -d10s http://localhost:8082/courses
```

---

## Completed Architecture

```
┌────────────────────────────────────────────────────────────┐
│                      Your Microservices                    │
└─────────────┬────────────────────────────────┬─────────────┘
              │                                │
              ▼                                ▼
        ┌──────────────┐              ┌──────────────────┐
        │ CourseService│              │RegistrationSrv  │
        │  (Port 8082) │◀─────────────│  (Port 8083)     │
        └──────┬───────┘   PATCH      └────────┬─────────┘
               │ HTTP Call                      │ HTTP Call
               ▼                                ▼
        ┌──────────────┐              ┌──────────────────┐
        │  course_db   │              │registration_db   │
        │   (MySQL)    │              │    (MySQL)       │
        └──────────────┘              └──────────────────┘

Flow Examples:
1. Search: Browser → GET /courses?... → CourseService → course_db
2. Register: Browser → POST /registrations → RegistrationService
   → CourseClient → CourseService → course_db + registration_db
3. Dual Update: Both databases modified in single API call (though not atomic)
```

---

## End of Setup Guide ✅

**Ready to test? Start at Phase 3 with your Postman requests!**

