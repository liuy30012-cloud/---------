package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.CreatePurchaseRequestPayload;
import com.library.dto.PurchaseRequestBoardDTO;
import com.library.dto.PurchaseRequestCreateResponseDTO;
import com.library.dto.PurchaseRequestItemDTO;
import com.library.dto.PurchaseRequestVoteResponseDTO;
import com.library.dto.UpdatePurchaseRequestStatusPayload;
import com.library.exception.ResourceNotFoundException;
import com.library.service.PurchaseRequestService;
import com.library.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase-requests")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<PurchaseRequestBoardDTO>> getBoard(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        return ResponseEntity.ok(ApiResponse.success(
            purchaseRequestService.getBoard(userId),
            "采购心愿池加载成功"
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PurchaseRequestCreateResponseDTO>> createRequest(
        @Valid @RequestBody CreatePurchaseRequestPayload payload,
        Authentication authentication
    ) {
        Long userId = getUserIdFromAuth(authentication);
        PurchaseRequestCreateResponseDTO response = purchaseRequestService.createRequest(userId, payload);
        String message = Boolean.TRUE.equals(response.getCreated())
            ? "采购请求已提交"
            : switch (response.getConflictType()) {
                case "EXISTING_BOOK" -> "馆藏中已存在该图书，请直接查看馆藏详情";
                case "DUPLICATE_REQUEST" -> "该书已在采购心愿池中，请前往列表投票";
                default -> "请求已处理";
            };
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<ApiResponse<PurchaseRequestVoteResponseDTO>> vote(
        @PathVariable Long id,
        Authentication authentication
    ) {
        Long userId = getUserIdFromAuth(authentication);
        PurchaseRequestVoteResponseDTO response = purchaseRequestService.vote(userId, id);
        String message = Boolean.TRUE.equals(response.getAlreadyVoted())
            ? "你已经支持过这条采购请求"
            : "投票成功";
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<PurchaseRequestItemDTO>> updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdatePurchaseRequestStatusPayload payload,
        Authentication authentication
    ) {
        Long userId = getUserIdFromAuth(authentication);
        PurchaseRequestItemDTO response = purchaseRequestService.updateStatus(id, payload, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "采购进度已更新"));
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException("缺少认证令牌");
        }

        try {
            return jwtUtil.getUserIdFromToken(authentication.getCredentials().toString());
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("认证信息无效");
        }
    }
}
