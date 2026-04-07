package com.library.testsupport;

import com.library.config.JpaAuditingConfig;
import com.library.config.SecurityStateStoreConfig;
import com.library.config.TimeConfig;
import com.library.util.JwtUtil;
import com.library.config.DemoUserInitializer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
    SecurityAutoConfiguration.class,
    SecurityFilterAutoConfiguration.class,
    UserDetailsServiceAutoConfiguration.class,
    ManagementWebSecurityAutoConfiguration.class
})
@EntityScan(basePackages = "com.library.model")
@EnableJpaRepositories(basePackages = "com.library.repository")
@ComponentScan(basePackages = {
    "com.library.service",
    "com.library.service.borrow"
})
@Import({
    TimeConfig.class,
    SecurityStateStoreConfig.class,
    JpaAuditingConfig.class,
    DemoUserInitializer.class,
    JwtUtil.class
})
public class BorrowTestApplication {
}
