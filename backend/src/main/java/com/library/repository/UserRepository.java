package com.library.repository;

import com.library.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);

    boolean existsByEmail(String email);

    long countByStatus(Integer status);

    long countByRole(String role);

    long countByRoleAndStatus(String role, Integer status);

    @Query("""
        SELECT u FROM User u
        WHERE (:keyword IS NULL
            OR LOWER(u.studentId) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:role IS NULL OR u.role = :role)
          AND (:status IS NULL OR u.status = :status)
        """)
    Page<User> searchForAdmin(@Param("keyword") String keyword,
                              @Param("role") String role,
                              @Param("status") Integer status,
                              Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.status = :status")
    List<User> findAllByRoleAndStatusForUpdate(@Param("role") String role, @Param("status") Integer status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.loginCount = u.loginCount + 1, u.lastLoginTime = :lastLoginTime WHERE u.id = :userId")
    int incrementLoginStats(@Param("userId") Long userId, @Param("lastLoginTime") LocalDateTime lastLoginTime);
}
