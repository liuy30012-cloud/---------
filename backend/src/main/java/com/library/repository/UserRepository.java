package com.library.repository;

import com.library.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);

    boolean existsByEmail(String email);

    long countByStatus(Integer status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE User u SET u.loginCount = u.loginCount + 1, u.lastLoginTime = :lastLoginTime WHERE u.id = :userId")
    int incrementLoginStats(@Param("userId") Long userId, @Param("lastLoginTime") LocalDateTime lastLoginTime);
}
