package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.BorrowApprovalDecisionRequest;
import com.library.dto.BorrowRequest;
import com.library.dto.BorrowResponse;
import com.library.service.BorrowService;
import com.library.util.JwtUtil;
import com.library.util.PageableHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;
    private final JwtUtil jwtUtil;

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<BorrowResponse>> applyBorrow(@Valid @RequestBody BorrowRequest request, Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        BorrowResponse response = borrowService.applyBorrow(userId, request);
        return ApiResponse.ok(response, response.getStatusHint());
    }

    @PostMapping("/{recordId}/return")
    public ResponseEntity<ApiResponse<BorrowResponse>> returnBook(@PathVariable Long recordId, Authentication authentication) {
        validateRecordId(recordId);
        Long userId = getUserIdFromAuth(authentication);
        BorrowResponse response = borrowService.returnBook(recordId, userId);
        return ApiResponse.ok(response, "归还成功。");
    }

    @PostMapping("/{recordId}/renew")
    public ResponseEntity<ApiResponse<BorrowResponse>> renewBorrow(@PathVariable Long recordId, Authentication authentication) {
        validateRecordId(recordId);
        Long userId = getUserIdFromAuth(authentication);
        BorrowResponse response = borrowService.renewBorrow(recordId, userId);
        return ApiResponse.ok(response, "续借成功。");
    }

    @PostMapping("/{recordId}/pickup")
    public ResponseEntity<ApiResponse<BorrowResponse>> confirmPickup(@PathVariable Long recordId, Authentication authentication) {
        validateRecordId(recordId);
        Long userId = getUserIdFromAuth(authentication);
        BorrowResponse response = borrowService.confirmPickup(recordId, userId);
        return ApiResponse.ok(response, "取书成功。");
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<BorrowResponse>>> getBorrowHistory(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "15") int size,
        @RequestParam(defaultValue = "createdAt,desc") String[] sort
    ) {
        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageableHelper.createPageable(page, size, 15, sort);
        Page<BorrowResponse> history = borrowService.getUserBorrowHistory(userId, pageable);
        return ApiResponse.okWithPagination(
            history.getContent(),
            (int) history.getTotalElements(),
            history.getNumber(),
            history.getSize(),
            history.getTotalPages()
        );
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<BorrowResponse>>> getCurrentBorrows(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "createdAt,desc") String[] sort
    ) {
        Long userId = getUserIdFromAuth(authentication);
        Pageable pageable = PageableHelper.createPageable(page, size, 10, sort);
        Page<BorrowResponse> borrows = borrowService.getUserCurrentBorrows(userId, pageable);
        return ApiResponse.okWithPagination(
            borrows.getContent(),
            (int) borrows.getTotalElements(),
            borrows.getNumber(),
            borrows.getSize(),
            borrows.getTotalPages()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/pending")
    public ResponseEntity<ApiResponse<List<BorrowResponse>>> getPendingBorrows(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "createdAt,asc") String[] sort
    ) {
        Pageable pageable = PageableHelper.createPageable(page, size, 20, sort);
        Page<BorrowResponse> pendingBorrows = borrowService.getPendingBorrows(pageable);
        return ApiResponse.okWithPagination(
            pendingBorrows.getContent(),
            (int) pendingBorrows.getTotalElements(),
            pendingBorrows.getNumber(),
            pendingBorrows.getSize(),
            pendingBorrows.getTotalPages()
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/{recordId}/decision")
    public ResponseEntity<ApiResponse<BorrowResponse>> reviewBorrow(
        @PathVariable Long recordId,
        @Valid @RequestBody BorrowApprovalDecisionRequest request,
        Authentication authentication
    ) {
        validateRecordId(recordId);
        String approver = authentication == null ? "LIBRARIAN" : authentication.getName();
        BorrowResponse response = borrowService.approveBorrow(
            recordId,
            approver,
            request.isApproved(),
            request.getRejectReason()
        );

        return ApiResponse.ok(response, response.getStatusHint());
    }

    private void validateRecordId(Long recordId) {
        if (recordId == null || recordId <= 0) {
            throw new IllegalArgumentException("借阅记录 ID 无效。");
        }
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException("缺少认证令牌。");
        }

        String token = authentication.getCredentials().toString();
        return jwtUtil.getUserIdFromToken(token);
    }
}
