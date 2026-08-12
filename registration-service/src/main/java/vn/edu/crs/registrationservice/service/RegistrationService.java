package vn.edu.crs.registrationservice.service;

import vn.edu.crs.registrationservice.client.CourseClient;
import vn.edu.crs.registrationservice.dto.RegistrationRequestDTO;
import vn.edu.crs.registrationservice.entity.Registration;
import vn.edu.crs.registrationservice.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final String DA_DANG_KY = "DA_DANG_KY";
    private static final String DA_HUY = "DA_HUY";

    private final RegistrationRepository registrationRepository;
    private final CourseClient courseClient;

    public Registration register(RegistrationRequestDTO dto) {
        // Kiểm tra xem sinh viên đã đăng ký môn học này chưa
        if (registrationRepository.existsByStudentIdAndCourseIdAndTrangThai(
                dto.getStudentId(), dto.getCourseId(), DA_DANG_KY)) {
            throw new IllegalStateException("Sinh vien da dang ky mon hoc nay roi");
        }

        // Bước 1: gọi sang course-service để trừ chỗ TRƯỚC.
        // Nếu bước này ném exception, hàm sẽ dừng lại ngay, KHÔNG lưu Registration.
        courseClient.reserveSeat(dto.getCourseId());

        // Bước 2: chỉ lưu Registration SAU KHI course-service xác nhận thành công.
        Registration registration = new Registration();
        registration.setStudentId(dto.getStudentId());
        registration.setCourseId(dto.getCourseId());
        registration.setTrangThai(DA_DANG_KY);
        registration.setNgayDangKy(LocalDateTime.now());

        return registrationRepository.save(registration);

        // LƯU Ý: nếu dòng save() này thất bại (ví dụ mất kết nối DB
        // đúng lúc nay), chỗ đã bị trừ ở course-service nhưng không có
        // bản ghi Registration nào được tạo.
        // Đây là giới hạn đã biết của kiến trúc đơn giản hoá này.
    }

    public void cancel(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new NoSuchElementException("Khong tim thay dang ky id = " + registrationId));

        if (DA_HUY.equals(registration.getTrangThai())) {
            throw new IllegalStateException("Dang ky nay da duoc huy truoc do");
        }

        // Gọi sang course-service để hoàn trả chỗ TRƯỚC khi đổi trạng thái
        courseClient.releaseSeat(registration.getCourseId());

        registration.setTrangThai(DA_HUY);
        registrationRepository.save(registration);
    }
}
