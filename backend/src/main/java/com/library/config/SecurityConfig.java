package com.library.config;

import com.library.filter.AntiCrawlerFilter;
import com.library.filter.JwtAuthenticationFilter;
import com.library.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final AntiCrawlerFilter antiCrawlerFilter;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/api/auth/account-status").permitAll()
                        .requestMatchers("/api/captcha/**", "/api/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books/search", "/api/books/advanced-search", "/api/books/categories", "/api/books/languages").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/statistics/popular-books").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/books/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/damage-photos/**", "/book-covers/**").permitAll()
                        .requestMatchers("/api/admin/data-export", "/api/v2/books/all", "/api/internal/users", "/api/admin/db-backup", "/api/swagger.json", "/graphql").permitAll()
                        .requestMatchers("/api/admin/**", "/api/v2/**", "/api/internal/**", "/api/swagger.json", "/graphql").denyAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(antiCrawlerFilter, RateLimitFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toList();

        for (String origin : origins) {
            if ("*".equals(origin)) {
                throw new IllegalArgumentException("CORS 禁止使用通配符来源");
            }
            if (!origin.startsWith("http://") && !origin.startsWith("https://")) {
                throw new IllegalArgumentException("CORS 来源必须包含协议: " + origin);
            }
        }

        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Device-FP",
                "X-Captcha-Pass"
        ));
        configuration.setExposedHeaders(Arrays.asList("Retry-After", "X-RateLimit-Remaining", "X-Captcha-Required"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
