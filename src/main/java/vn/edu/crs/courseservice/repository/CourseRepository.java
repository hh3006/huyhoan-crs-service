package vn.edu.crs.courseservice.repository;

import vn.edu.crs.courseservice.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {

    boolean existsByTenMonHocIgnoreCase(String tenMonHoc);

    // Buổi 3: Spring Data JPA tự sinh câu lệnh SQL LIKE %keyword% không phân biệt hoa/thường
    Page<Course> findByTenMonHocContainingIgnoreCase(String keyword, Pageable pageable);

}