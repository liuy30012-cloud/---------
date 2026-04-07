package com.library.repository;

import com.library.model.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    Optional<PurchaseRequest> findByDedupeKey(String dedupeKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PurchaseRequest p WHERE p.id = :id")
    Optional<PurchaseRequest> findByIdForUpdate(@Param("id") Long id);
}
