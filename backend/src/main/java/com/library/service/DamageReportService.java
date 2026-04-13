package com.library.service;

import com.library.dto.DamageReportResponse;
import com.library.dto.DamageReportStatistics;
import com.library.dto.UpdateDamageReportStatusPayload;
import com.library.model.Book;
import com.library.model.DamageReport;
import com.library.model.DamageReport.DamageStatus;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.DamageReportRepository;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DamageReportService {

    private final DamageReportRepository damageReportRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int MAX_PHOTOS = 3;
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    @Value("${app.upload.damage-photos-dir:uploads/damage-photos}")
    private String uploadDir;

    @Transactional
    public DamageReportResponse createReport(Long bookId, Long reporterId, String damageTypes,
                                              String description, List<MultipartFile> photos) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new IllegalArgumentException("书籍不存在"));

        User reporter = userRepository.findById(reporterId)
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (photos == null || photos.isEmpty()) {
            throw new IllegalArgumentException("至少上传一张破损照片");
        }
        if (photos.size() > MAX_PHOTOS) {
            throw new IllegalArgumentException("最多上传 " + MAX_PHOTOS + " 张照片");
        }

        for (MultipartFile photo : photos) {
            if (photo.getSize() > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("单张照片不能超过 5MB");
            }
            if (!ALLOWED_TYPES.contains(photo.getContentType())) {
                throw new IllegalArgumentException("仅支持 JPG、PNG、WebP 格式的照片");
            }
        }

        DamageReport report = new DamageReport();
        report.setBookId(bookId);
        report.setBookTitle(book.getTitle());
        report.setReporterId(reporterId);
        report.setReporterName(reporter.getUsername());
        report.setDamageTypes(damageTypes);
        report.setDescription(description);
        report.setStatus(DamageStatus.PENDING.name());

        report = damageReportRepository.save(report);

        List<String> photoUrls = new ArrayList<>();
        try {
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            for (MultipartFile photo : photos) {
                String ext = extractExtension(photo.getOriginalFilename());
                String filename = report.getId() + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
                Path filePath = dirPath.resolve(filename);
                Files.write(filePath, photo.getBytes());
                photoUrls.add("/damage-photos/" + filename);
            }
        } catch (IOException e) {
            log.error("Failed to save damage photos: {}", e.getMessage(), e);
            throw new RuntimeException("照片保存失败，请重试");
        }

        report.setPhotoUrls(String.join(",", photoUrls));
        report = damageReportRepository.save(report);

        // 通知所有管理员
        notifyAdminsNewReport(book.getTitle(), reporter);

        return toResponse(report);
    }

    public Page<DamageReportResponse> getMyReports(Long reporterId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
            Sort.by(Sort.Direction.DESC, "createdAt"));
        return damageReportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId, pageable)
            .map(this::toResponse);
    }

    public DamageReportResponse getReportDetail(Long reportId, Long userId, boolean isAdmin) {
        DamageReport report = damageReportRepository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("报告不存在"));

        if (!report.getReporterId().equals(userId) && !isAdmin) {
            throw new IllegalArgumentException("无权查看此报告");
        }

        return toResponse(report);
    }

    public Page<DamageReportResponse> getAllReports(String status, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
            Sort.by(Sort.Direction.DESC, "createdAt"));

        if (StringUtils.hasText(status)) {
            return damageReportRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                .map(this::toResponse);
        }
        return damageReportRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map(this::toResponse);
    }

    public DamageReportStatistics getStatistics() {
        DamageReportStatistics stats = new DamageReportStatistics();
        List<Object[]> counts = damageReportRepository.countGroupByStatus();
        long total = 0;

        for (Object[] row : counts) {
            String status = (String) row[0];
            long count = ((Number) row[1]).longValue();
            total += count;

            switch (status) {
                case "PENDING" -> stats.setPendingCount(count);
                case "IN_PROGRESS" -> stats.setInProgressCount(count);
                case "RESOLVED" -> stats.setResolvedCount(count);
                case "REJECTED" -> stats.setRejectedCount(count);
            }
        }
        stats.setTotalCount(total);
        return stats;
    }

    @Transactional
    public DamageReportResponse updateStatus(Long reportId, Long adminId, UpdateDamageReportStatusPayload payload) {
        DamageReport report = damageReportRepository.findById(reportId)
            .orElseThrow(() -> new IllegalArgumentException("报告不存在"));

        DamageStatus newStatus;
        try {
            newStatus = DamageStatus.valueOf(payload.getStatus());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的状态值: " + payload.getStatus());
        }

        DamageStatus oldStatus = DamageStatus.valueOf(report.getStatus());
        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new IllegalArgumentException("管理员不存在"));

        report.setStatus(newStatus.name());
        report.setAdminNotes(payload.getAdminNotes());
        report.setResolvedBy(adminId);
        report.setResolvedByName(admin.getUsername());
        report.setResolvedAt(LocalDateTime.now());

        // 自动更新书籍状态
        updateBookStatus(report.getBookId(), oldStatus, newStatus);

        report = damageReportRepository.save(report);

        // 通知报告人
        notifyReporterStatusChanged(report, admin.getUsername());

        return toResponse(report);
    }

    @Transactional
    public void deleteReport(Long reportId) {
        if (!damageReportRepository.existsById(reportId)) {
            throw new IllegalArgumentException("报告不存在");
        }
        damageReportRepository.deleteById(reportId);
    }

    private void updateBookStatus(Long bookId, DamageStatus oldStatus, DamageStatus newStatus) {
        Book book = bookRepository.findById(bookId).orElse(null);
        if (book == null) return;

        if (oldStatus == DamageStatus.PENDING && newStatus == DamageStatus.IN_PROGRESS) {
            book.setStatus("DAMAGED");
            bookRepository.save(book);
        } else if (newStatus == DamageStatus.RESOLVED) {
            book.setStatus("AVAILABLE");
            bookRepository.save(book);
        }
    }

    private void notifyAdminsNewReport(String bookTitle, User reporter) {
        List<User> admins = userRepository.findAll().stream()
            .filter(u -> "ADMIN".equals(u.getRole()))
            .toList();

        for (User admin : admins) {
            notificationService.sendDamageReportSubmittedNotification(admin, bookTitle, reporter.getUsername());
        }
    }

    private void notifyReporterStatusChanged(DamageReport report, String adminName) {
        User reporter = userRepository.findById(report.getReporterId()).orElse(null);
        if (reporter == null) return;

        DamageStatus status = DamageStatus.valueOf(report.getStatus());
        switch (status) {
            case IN_PROGRESS -> notificationService.sendDamageReportInProgressNotification(reporter, report.getBookTitle(), adminName);
            case RESOLVED -> notificationService.sendDamageReportResolvedNotification(reporter, report.getBookTitle());
            case REJECTED -> notificationService.sendDamageReportRejectedNotification(reporter, report.getBookTitle());
            default -> {}
        }
    }

    private DamageReportResponse toResponse(DamageReport report) {
        DamageReportResponse response = new DamageReportResponse();
        response.setId(report.getId());
        response.setBookId(report.getBookId());
        response.setBookTitle(report.getBookTitle());
        response.setReporterId(report.getReporterId());
        response.setReporterName(report.getReporterName());
        response.setDamageTypes(StringUtils.hasText(report.getDamageTypes())
            ? Arrays.asList(report.getDamageTypes().split(",")) : List.of());
        response.setDescription(report.getDescription());
        response.setPhotoUrls(StringUtils.hasText(report.getPhotoUrls())
            ? Arrays.asList(report.getPhotoUrls().split(",")) : List.of());
        response.setStatus(report.getStatus());
        response.setAdminNotes(report.getAdminNotes());
        response.setResolvedByName(report.getResolvedByName());
        response.setResolvedAt(report.getResolvedAt());
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }

    private String extractExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
            if (List.of("jpg", "jpeg", "png", "webp").contains(ext)) {
                return ext;
            }
        }
        return "jpg";
    }
}
