package com.library.config;

import com.library.service.UserAccountService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoUserInitializer {

    @Bean
    public ApplicationRunner demoUserApplicationRunner(UserAccountService userAccountService) {
        return args -> userAccountService.ensureDefaultUsersExist();
    }
}
