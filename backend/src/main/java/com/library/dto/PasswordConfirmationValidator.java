package com.library.dto;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordConfirmationValidator.PasswordMatchValidator.class)
@Documented
public @interface PasswordConfirmationValidator {
    String message() default "密码和确认密码不一致";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class PasswordMatchValidator implements ConstraintValidator<PasswordConfirmationValidator, RegisterRequest> {
        @Override
        public boolean isValid(RegisterRequest request, ConstraintValidatorContext context) {
            if (request == null) {
                return true;
            }

            String password = request.getPassword();
            String confirmPassword = request.getConfirmPassword();

            if (password == null || confirmPassword == null) {
                return false;
            }

            return password.equals(confirmPassword);
        }
    }
}
