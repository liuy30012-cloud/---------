package com.library.service;

import com.library.constant.AdminUserManagementMessageKeys;
import com.library.dto.AdminUserStatisticsDTO;
import com.library.dto.UserDTO;
import com.library.exception.ResourceNotFoundException;
import com.library.model.User;
import com.library.repository.UserRepository;
import com.library.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserManagementService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final int ACTIVE_STATUS = 1;
    private static final int DISABLED_STATUS = 0;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Set<String> ALLOWED_ROLES = Set.of("STUDENT", "TEACHER", ADMIN_ROLE);

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public Page<UserDTO> getUsers(String keyword, String role, Integer status, int page, int size) {
        String normalizedRole = normalizeRole(role, true);
        Integer normalizedStatus = normalizeStatus(status, true);
        PageRequest pageable = PageRequest.of(
            Math.max(page, 0),
            Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return userRepository.searchForAdmin(normalizeKeyword(keyword), normalizedRole, normalizedStatus, pageable)
            .map(UserDTO::fromUser);
    }

    @Transactional(readOnly = true)
    public AdminUserStatisticsDTO getStatistics() {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(ACTIVE_STATUS);
        long disabledUsers = userRepository.countByStatus(DISABLED_STATUS);
        long activeAdmins = userRepository.countByRoleAndStatus(ADMIN_ROLE, ACTIVE_STATUS);
        return new AdminUserStatisticsDTO(totalUsers, activeUsers, disabledUsers, activeAdmins);
    }

    @Transactional
    public UserDTO updateUserRole(Long targetUserId, Long operatorUserId, String newRole) {
        String normalizedRole = normalizeRole(newRole, false);
        User targetUser = findExistingUser(targetUserId);

        if (targetUser.getId().equals(operatorUserId) && !normalizedRole.equals(targetUser.getRole())) {
            throw new IllegalArgumentException(AdminUserManagementMessageKeys.SELF_ROLE_CHANGE_FORBIDDEN);
        }

        if (normalizedRole.equals(targetUser.getRole())) {
            return UserDTO.fromUser(targetUser);
        }

        assertActiveAdminWillRemain(targetUser, normalizedRole, targetUser.getStatus());

        String previousRole = targetUser.getRole();
        targetUser.setRole(normalizedRole);
        User savedUser = userRepository.save(targetUser);
        jwtUtil.invalidateAllUserTokens(savedUser.getId());

        log.info(
            "Admin {} changed user {} role from {} to {}",
            operatorUserId,
            savedUser.getId(),
            previousRole,
            normalizedRole
        );

        return UserDTO.fromUser(savedUser);
    }

    @Transactional
    public UserDTO updateUserStatus(Long targetUserId, Long operatorUserId, Integer newStatus) {
        Integer normalizedStatus = normalizeStatus(newStatus, false);
        User targetUser = findExistingUser(targetUserId);

        if (targetUser.getId().equals(operatorUserId) && !normalizedStatus.equals(targetUser.getStatus())) {
            throw new IllegalArgumentException(AdminUserManagementMessageKeys.SELF_STATUS_CHANGE_FORBIDDEN);
        }

        if (normalizedStatus.equals(targetUser.getStatus())) {
            return UserDTO.fromUser(targetUser);
        }

        assertActiveAdminWillRemain(targetUser, targetUser.getRole(), normalizedStatus);

        Integer previousStatus = targetUser.getStatus();
        targetUser.setStatus(normalizedStatus);
        User savedUser = userRepository.save(targetUser);

        if (normalizedStatus == DISABLED_STATUS) {
            jwtUtil.invalidateAllUserTokens(savedUser.getId());
        }

        log.info(
            "Admin {} changed user {} status from {} to {}",
            operatorUserId,
            savedUser.getId(),
            previousStatus,
            normalizedStatus
        );

        return UserDTO.fromUser(savedUser);
    }

    private User findExistingUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(AdminUserManagementMessageKeys.USER_NOT_FOUND));
    }

    private void assertActiveAdminWillRemain(User targetUser, String targetRole, Integer targetStatus) {
        boolean isCurrentlyActiveAdmin = ADMIN_ROLE.equals(targetUser.getRole()) && ACTIVE_STATUS == targetUser.getStatus();
        boolean willRemainActiveAdmin = ADMIN_ROLE.equals(targetRole) && ACTIVE_STATUS == targetStatus;

        if (!isCurrentlyActiveAdmin || willRemainActiveAdmin) {
            return;
        }

        long lockedActiveAdminCount = userRepository.findAllByRoleAndStatusForUpdate(ADMIN_ROLE, ACTIVE_STATUS).size();
        if (lockedActiveAdminCount <= 1) {
            throw new IllegalArgumentException(AdminUserManagementMessageKeys.ACTIVE_ADMIN_REQUIRED);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String normalized = keyword.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeRole(String role, boolean allowNull) {
        if (role == null) {
            if (allowNull) {
                return null;
            }
            throw new IllegalArgumentException(AdminUserManagementMessageKeys.ROLE_REQUIRED);
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (allowNull && normalized.isEmpty()) {
            return null;
        }
        if (!ALLOWED_ROLES.contains(normalized)) {
            throw new IllegalArgumentException(AdminUserManagementMessageKeys.ROLE_INVALID);
        }
        return normalized;
    }

    private Integer normalizeStatus(Integer status, boolean allowNull) {
        if (status == null) {
            if (allowNull) {
                return null;
            }
            throw new IllegalArgumentException(AdminUserManagementMessageKeys.STATUS_REQUIRED);
        }
        if (status != ACTIVE_STATUS && status != DISABLED_STATUS) {
            throw new IllegalArgumentException(AdminUserManagementMessageKeys.STATUS_INVALID);
        }
        return status;
    }
}
