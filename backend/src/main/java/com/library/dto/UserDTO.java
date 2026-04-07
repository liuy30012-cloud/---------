package com.library.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户数据传输对象 - 不包含敏感信息
 * 用于API响应，避免直接返回User实体导致敏感信息泄露
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String studentId;
    private String username;
    private String email;  // 可选：如果需要显示邮箱
    private String role;
    private String avatarUrl;
    private Integer status;
    private Integer loginCount;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdAt;

    // 不包含敏感字段：
    // - password (密码)
    // - phone (手机号 - 如果不需要显示)
    // - updatedAt (内部字段)

    /**
     * 从User实体转换为UserDTO
     */
    public static UserDTO fromUser(com.library.model.User user) {
        if (user == null) {
            return null;
        }
        return new UserDTO(
            user.getId(),
            user.getStudentId(),
            user.getUsername(),
            maskEmail(user.getEmail()),  // 脱敏处理
            user.getRole(),
            user.getAvatarUrl(),
            user.getStatus(),
            user.getLoginCount(),
            user.getLastLoginTime(),
            user.getCreatedAt()
        );
    }

    /**
     * 邮箱脱敏处理
     * 例如: example@gmail.com -> e****e@gmail.com
     */
    private static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return email;  // 太短，不脱敏
        }
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "***" + domain;
        }

        return localPart.charAt(0) + "****" + localPart.charAt(localPart.length() - 1) + domain;
    }

    /**
     * 创建简化版DTO（仅包含基本信息）
     */
    public static UserDTO createSimple(com.library.model.User user) {
        if (user == null) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setStudentId(user.getStudentId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setAvatarUrl(user.getAvatarUrl());
        return dto;
    }
}
