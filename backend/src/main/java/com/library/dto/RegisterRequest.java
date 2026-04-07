package com.library.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordConfirmationValidator
public class RegisterRequest {
    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^[0-9]{7,10}$", message = "学号必须为 7 到 10 位数字")
    private String studentId;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 20, message = "用户名长度必须在 2 到 20 个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度必须在 8 到 20 个字符之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    public RegisterRequest() {}

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
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
}
