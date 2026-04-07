package com.library.dto;

public class UserInfo {
    private Long id;
    private String studentId;
    private String username;
    private String email;
    private String phone;
    private String role;
    private String avatarUrl;

    public UserInfo() {}

    // 从User实体转换
    public static UserInfo fromUser(com.library.model.User user) {
        UserInfo info = new UserInfo();
        info.setId(user.getId());
        info.setStudentId(user.getStudentId());
        info.setUsername(user.getUsername());
        info.setEmail(user.getEmail());
        info.setPhone(user.getPhone());
        info.setRole(user.getRole());
        info.setAvatarUrl(user.getAvatarUrl());
        return info;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
