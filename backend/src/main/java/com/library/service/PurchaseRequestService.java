package com.library.service;

import com.library.dto.CreatePurchaseRequestPayload;
import com.library.dto.PurchaseRequestBoardDTO;
import com.library.dto.PurchaseRequestCreateResponseDTO;
import com.library.dto.PurchaseRequestItemDTO;
import com.library.dto.PurchaseRequestVoteResponseDTO;
import com.library.dto.UpdatePurchaseRequestStatusPayload;
import com.library.exception.ResourceNotFoundException;
import com.library.model.Book;
import com.library.model.PurchaseRequest;
import com.library.model.PurchaseRequestStatus;
import com.library.model.PurchaseRequestVote;
import com.library.model.User;
import com.library.repository.BookRepository;
import com.library.repository.PurchaseRequestRepository;
import com.library.repository.PurchaseRequestVoteRepository;
import com.library.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseRequestService {

    private static final String CONFLICT_NONE = "NONE";
    private static final String CONFLICT_EXISTING_BOOK = "EXISTING_BOOK";
    private static final String CONFLICT_DUPLICATE_REQUEST = "DUPLICATE_REQUEST";
    private static final Pattern ISBN_SANITIZER = Pattern.compile("[^0-9Xx]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestVoteRepository purchaseRequestVoteRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Value("${purchase-requests.auto-priority-threshold:3}")
    private int autoPriorityThreshold;

    @Value("${purchase-requests.auto-planned-threshold:8}")
    private int autoPlannedThreshold;

    @Transactional(readOnly = true)
    public PurchaseRequestBoardDTO getBoard(Long currentUserId) {
        List<PurchaseRequest> requests = purchaseRequestRepository.findAll();
        requests.sort(requestComparator());

        Map<Long, String> proposerNames = resolveUserNames(
            requests.stream().map(PurchaseRequest::getProposerUserId).collect(Collectors.toSet())
        );
        Set<Long> votedRequestIds = resolveVotedRequestIds(currentUserId, requests.stream()
            .map(PurchaseRequest::getId)
            .toList());

        List<PurchaseRequestItemDTO> items = requests.stream()
            .map(request -> toItemDTO(request, proposerNames.get(request.getProposerUserId()), votedRequestIds.contains(request.getId())))
            .toList();

        EnumMap<PurchaseRequestStatus, Integer> counts = new EnumMap<>(PurchaseRequestStatus.class);
        for (PurchaseRequestStatus status : PurchaseRequestStatus.values()) {
            counts.put(status, 0);
        }

        int totalSupportCount = 0;
        for (PurchaseRequest request : requests) {
            counts.put(request.getStatus(), counts.get(request.getStatus()) + 1);
            totalSupportCount += defaultSupportCount(request);
        }

        return new PurchaseRequestBoardDTO(
            requests.size(),
            counts.get(PurchaseRequestStatus.PENDING_REVIEW),
            counts.get(PurchaseRequestStatus.PRIORITY_POOL),
            counts.get(PurchaseRequestStatus.PLANNED),
            counts.get(PurchaseRequestStatus.PURCHASING),
            counts.get(PurchaseRequestStatus.ARRIVED),
            counts.get(PurchaseRequestStatus.REJECTED),
            totalSupportCount,
            items
        );
    }

    @Transactional
    public PurchaseRequestCreateResponseDTO createRequest(Long currentUserId, CreatePurchaseRequestPayload payload) {
        String title = requireTrimmed(payload.getTitle(), "书名不能为空");
        String author = requireTrimmed(payload.getAuthor(), "作者不能为空");
        String isbn = trimToNull(payload.getIsbn());
        String reason = trimToNull(payload.getReason());

        Optional<Book> existingBook = findExistingBook(title, author, isbn);
        if (existingBook.isPresent()) {
            return new PurchaseRequestCreateResponseDTO(false, CONFLICT_EXISTING_BOOK, null, existingBook.get().getId(), null);
        }

        String dedupeKey = buildDedupeKey(title, author, isbn);
        Optional<PurchaseRequest> duplicateRequest = purchaseRequestRepository.findByDedupeKey(dedupeKey);
        if (duplicateRequest.isPresent()) {
            PurchaseRequest existingRequest = duplicateRequest.get();
            boolean voted = purchaseRequestVoteRepository.existsByPurchaseRequestIdAndUserId(existingRequest.getId(), currentUserId);
            String proposerName = resolveUserNames(Set.of(existingRequest.getProposerUserId())).get(existingRequest.getProposerUserId());
            return new PurchaseRequestCreateResponseDTO(
                false,
                CONFLICT_DUPLICATE_REQUEST,
                existingRequest.getId(),
                null,
                toItemDTO(existingRequest, proposerName, voted)
            );
        }

        PurchaseRequest request = new PurchaseRequest();
        request.setTitle(title);
        request.setAuthor(author);
        request.setIsbn(isbn);
        request.setReason(reason);
        request.setProposerUserId(currentUserId);
        request.setSupportCount(1);
        request.setStatus(PurchaseRequestStatus.PENDING_REVIEW);
        request.setStatusNote(null);
        request.setDedupeKey(dedupeKey);
        request.setStatusManagedManually(false);
        applyAutoProgression(request);

        try {
            PurchaseRequest savedRequest = purchaseRequestRepository.saveAndFlush(request);

            PurchaseRequestVote vote = new PurchaseRequestVote();
            vote.setPurchaseRequestId(savedRequest.getId());
            vote.setUserId(currentUserId);
            purchaseRequestVoteRepository.saveAndFlush(vote);

            String proposerName = resolveUserNames(Set.of(currentUserId)).get(currentUserId);
            return new PurchaseRequestCreateResponseDTO(
                true,
                CONFLICT_NONE,
                null,
                null,
                toItemDTO(savedRequest, proposerName, true)
            );
        } catch (DataIntegrityViolationException ex) {
            PurchaseRequest existingRequest = purchaseRequestRepository.findByDedupeKey(dedupeKey)
                .orElseThrow(() -> ex);
            boolean voted = purchaseRequestVoteRepository.existsByPurchaseRequestIdAndUserId(existingRequest.getId(), currentUserId);
            String proposerName = resolveUserNames(Set.of(existingRequest.getProposerUserId())).get(existingRequest.getProposerUserId());
            return new PurchaseRequestCreateResponseDTO(
                false,
                CONFLICT_DUPLICATE_REQUEST,
                existingRequest.getId(),
                null,
                toItemDTO(existingRequest, proposerName, voted)
            );
        }
    }

    @Transactional
    public PurchaseRequestVoteResponseDTO vote(Long currentUserId, Long requestId) {
        PurchaseRequest request = purchaseRequestRepository.findByIdForUpdate(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("采购请求不存在"));

        if (!isVotingOpen(request.getStatus())) {
            throw new IllegalArgumentException("当前状态不支持继续投票");
        }

        if (purchaseRequestVoteRepository.existsByPurchaseRequestIdAndUserId(requestId, currentUserId)) {
            String proposerName = resolveUserNames(Set.of(request.getProposerUserId())).get(request.getProposerUserId());
            return new PurchaseRequestVoteResponseDTO(true, toItemDTO(request, proposerName, true));
        }

        PurchaseRequestVote vote = new PurchaseRequestVote();
        vote.setPurchaseRequestId(requestId);
        vote.setUserId(currentUserId);

        try {
            purchaseRequestVoteRepository.saveAndFlush(vote);
        } catch (DataIntegrityViolationException ex) {
            String proposerName = resolveUserNames(Set.of(request.getProposerUserId())).get(request.getProposerUserId());
            return new PurchaseRequestVoteResponseDTO(true, toItemDTO(request, proposerName, true));
        }

        request.setSupportCount(defaultSupportCount(request) + 1);
        applyAutoProgression(request);
        PurchaseRequest savedRequest = purchaseRequestRepository.save(request);
        String proposerName = resolveUserNames(Set.of(savedRequest.getProposerUserId())).get(savedRequest.getProposerUserId());
        return new PurchaseRequestVoteResponseDTO(false, toItemDTO(savedRequest, proposerName, true));
    }

    @Transactional
    public PurchaseRequestItemDTO updateStatus(Long requestId, UpdatePurchaseRequestStatusPayload payload, Long currentUserId) {
        PurchaseRequest request = purchaseRequestRepository.findByIdForUpdate(requestId)
            .orElseThrow(() -> new ResourceNotFoundException("采购请求不存在"));

        request.setStatus(payload.getStatus());
        request.setStatusNote(trimToNull(payload.getStatusNote()));
        request.setStatusManagedManually(isManualLockStatus(payload.getStatus()));

        PurchaseRequest savedRequest = purchaseRequestRepository.save(request);
        boolean voted = purchaseRequestVoteRepository.existsByPurchaseRequestIdAndUserId(savedRequest.getId(), currentUserId);
        String proposerName = resolveUserNames(Set.of(savedRequest.getProposerUserId())).get(savedRequest.getProposerUserId());
        return toItemDTO(savedRequest, proposerName, voted);
    }

    private Optional<Book> findExistingBook(String title, String author, String isbn) {
        String normalizedIsbn = normalizeIsbn(isbn);
        if (StringUtils.hasText(normalizedIsbn)) {
            Optional<Book> bookByIsbn = bookRepository.findFirstByNormalizedIsbn(normalizedIsbn);
            if (bookByIsbn.isPresent()) {
                return bookByIsbn;
            }
        }
        return bookRepository.findFirstByNormalizedTitleAndAuthor(title, author);
    }

    private PurchaseRequestItemDTO toItemDTO(PurchaseRequest request, String proposerName, boolean votedByCurrentUser) {
        return new PurchaseRequestItemDTO(
            request.getId(),
            request.getTitle(),
            request.getAuthor(),
            request.getIsbn(),
            request.getReason(),
            request.getProposerUserId(),
            proposerName,
            defaultSupportCount(request),
            request.getStatus().name(),
            getStatusLabel(request.getStatus()),
            request.getStatusNote(),
            getProgressPercent(request.getStatus()),
            getProgressLabel(request.getStatus()),
            votedByCurrentUser,
            isVotingOpen(request.getStatus()) && !votedByCurrentUser,
            request.getCreatedAt(),
            request.getUpdatedAt()
        );
    }

    private Map<Long, String> resolveUserNames(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> userNameMap = new HashMap<>();
        for (User user : userRepository.findAllById(new HashSet<>(userIds))) {
            userNameMap.put(user.getId(), user.getUsername());
        }
        return userNameMap;
    }

    private Set<Long> resolveVotedRequestIds(Long currentUserId, List<Long> requestIds) {
        if (currentUserId == null || requestIds.isEmpty()) {
            return Set.of();
        }
        return purchaseRequestVoteRepository.findByUserIdAndPurchaseRequestIdIn(currentUserId, requestIds).stream()
            .map(PurchaseRequestVote::getPurchaseRequestId)
            .collect(Collectors.toSet());
    }

    private void applyAutoProgression(PurchaseRequest request) {
        if (Boolean.TRUE.equals(request.getStatusManagedManually())) {
            return;
        }

        int supportCount = defaultSupportCount(request);
        if (supportCount >= autoPlannedThreshold) {
            request.setStatus(PurchaseRequestStatus.PLANNED);
            return;
        }
        if (supportCount >= autoPriorityThreshold) {
            request.setStatus(PurchaseRequestStatus.PRIORITY_POOL);
            return;
        }
        request.setStatus(PurchaseRequestStatus.PENDING_REVIEW);
    }

    private Comparator<PurchaseRequest> requestComparator() {
        return Comparator
            .comparingInt((PurchaseRequest request) -> getSortRank(request.getStatus()))
            .thenComparing((PurchaseRequest request) -> defaultSupportCount(request), Comparator.reverseOrder())
            .thenComparing(PurchaseRequest::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private int getSortRank(PurchaseRequestStatus status) {
        return switch (status) {
            case PRIORITY_POOL -> 0;
            case PLANNED -> 1;
            case PURCHASING -> 2;
            case PENDING_REVIEW -> 3;
            case ARRIVED -> 4;
            case REJECTED -> 5;
        };
    }

    private boolean isVotingOpen(PurchaseRequestStatus status) {
        return status == PurchaseRequestStatus.PENDING_REVIEW
            || status == PurchaseRequestStatus.PRIORITY_POOL
            || status == PurchaseRequestStatus.PLANNED;
    }

    private boolean isManualLockStatus(PurchaseRequestStatus status) {
        return status == PurchaseRequestStatus.PLANNED
            || status == PurchaseRequestStatus.PURCHASING
            || status == PurchaseRequestStatus.ARRIVED
            || status == PurchaseRequestStatus.REJECTED;
    }

    private int getProgressPercent(PurchaseRequestStatus status) {
        return switch (status) {
            case PENDING_REVIEW -> 20;
            case PRIORITY_POOL -> 45;
            case PLANNED -> 65;
            case PURCHASING -> 85;
            case ARRIVED, REJECTED -> 100;
        };
    }

    private String getProgressLabel(PurchaseRequestStatus status) {
        return switch (status) {
            case PENDING_REVIEW -> "等待更多读者支持";
            case PRIORITY_POOL -> "已进入热门优先池";
            case PLANNED -> "已纳入采购计划";
            case PURCHASING -> "馆方正在采购处理中";
            case ARRIVED -> "图书已到馆";
            case REJECTED -> "本次采购未通过";
        };
    }

    private String getStatusLabel(PurchaseRequestStatus status) {
        return switch (status) {
            case PENDING_REVIEW -> "待评估";
            case PRIORITY_POOL -> "热门优先";
            case PLANNED -> "已纳入计划";
            case PURCHASING -> "采购中";
            case ARRIVED -> "已到馆";
            case REJECTED -> "已拒绝";
        };
    }

    private int defaultSupportCount(PurchaseRequest request) {
        return request.getSupportCount() == null ? 0 : request.getSupportCount();
    }

    private String buildDedupeKey(String title, String author, String isbn) {
        String normalizedIsbn = normalizeIsbn(isbn);
        if (StringUtils.hasText(normalizedIsbn)) {
            return "ISBN:" + normalizedIsbn;
        }
        return "TITLE_AUTHOR:" + normalizeText(title) + "|" + normalizeText(author);
    }

    private String normalizeIsbn(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = ISBN_SANITIZER.matcher(value).replaceAll("").toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeText(String value) {
        String trimmed = requireTrimmed(value, "字段不能为空");
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFKC);
        normalized = MULTI_SPACE.matcher(normalized).replaceAll(" ");
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String requireTrimmed(String value, String errorMessage) {
        String trimmed = trimToNull(value);
        if (!StringUtils.hasText(trimmed)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
