package com.library.repository;

import com.library.model.PurchaseRequestVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface PurchaseRequestVoteRepository extends JpaRepository<PurchaseRequestVote, Long> {

    boolean existsByPurchaseRequestIdAndUserId(Long purchaseRequestId, Long userId);

    List<PurchaseRequestVote> findByUserIdAndPurchaseRequestIdIn(Long userId, Collection<Long> purchaseRequestIds);
}
