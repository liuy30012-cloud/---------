package com.library.repository;

import com.library.model.DamageReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DamageReportRepository extends JpaRepository<DamageReport, Long> {

    Page<DamageReport> findByReporterIdOrderByCreatedAtDesc(Long reporterId, Pageable pageable);

    Page<DamageReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<DamageReport> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatus(String status);

    long countByReporterId(Long reporterId);

    @Query("SELECT d.status, COUNT(d) FROM DamageReport d GROUP BY d.status")
    java.util.List<Object[]> countGroupByStatus();
}
