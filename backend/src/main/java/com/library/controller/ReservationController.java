package com.library.controller;

import com.library.dto.ApiResponse;
import com.library.dto.BorrowResponse;
import com.library.dto.ReservationRequest;
import com.library.dto.ReservationResponse;
import com.library.service.ReservationService;
import com.library.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationResponse>> reserveBook(@Valid @RequestBody ReservationRequest request, Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        ReservationResponse response = reservationService.reserveBook(userId, request);
        return ApiResponse.ok(response, response.getStatusHint());
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<Void>> cancelReservation(
        @PathVariable Long reservationId,
        @RequestParam(required = false) String reason,
        Authentication authentication
    ) {
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("预约记录 ID 无效。");
        }
        if (reason != null && reason.length() > 500) {
            throw new IllegalArgumentException("取消原因不能超过 500 个字符。");
        }

        Long userId = getUserIdFromAuth(authentication);
        reservationService.cancelReservation(reservationId, userId, reason);
        return ApiResponse.ok(null, "预约已取消。");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> getReservations(Authentication authentication) {
        Long userId = getUserIdFromAuth(authentication);
        List<ReservationResponse> reservations = reservationService.getUserReservations(userId);
        return ApiResponse.ok(reservations);
    }

    @PostMapping("/{reservationId}/pickup")
    public ResponseEntity<ApiResponse<BorrowResponse>> pickupReservation(@PathVariable Long reservationId, Authentication authentication) {
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("预约记录 ID 无效。");
        }

        Long userId = getUserIdFromAuth(authentication);
        BorrowResponse response = reservationService.pickupReservation(reservationId, userId);
        return ApiResponse.ok(response, "预约取书完成。");
    }

    @PostMapping("/{reservationId}/extend")
    public ResponseEntity<ApiResponse<ReservationResponse>> extendPickupDeadline(@PathVariable Long reservationId, Authentication authentication) {
        if (reservationId == null || reservationId <= 0) {
            throw new IllegalArgumentException("预约记录 ID 无效。");
        }

        Long userId = getUserIdFromAuth(authentication);
        ReservationResponse response = reservationService.extendPickupDeadline(reservationId, userId);
        return ApiResponse.ok(response, "取书时限已延长。");
    }

    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getCredentials() == null) {
            throw new IllegalArgumentException("缺少认证令牌。");
        }

        String token = authentication.getCredentials().toString();
        return jwtUtil.getUserIdFromToken(token);
    }
}
