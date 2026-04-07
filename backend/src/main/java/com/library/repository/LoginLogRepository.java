package com.library.repository;

import com.library.model.LoginLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    List<LoginLog> findByStudentIdOrderByLoginTimeDescIdDesc(String studentId, Pageable pageable);
}
