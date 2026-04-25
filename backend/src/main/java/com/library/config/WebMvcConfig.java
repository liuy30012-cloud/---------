package com.library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.damage-photos-dir:uploads/damage-photos}")
    private String damagePhotosDir;

    @Value("${app.upload.book-covers-dir:uploads/book-covers}")
    private String bookCoversDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(damagePhotosDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/damage-photos/**")
                .addResourceLocations(absolutePath);

        String bookCoversAbsolutePath = Paths.get(bookCoversDir).toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/book-covers/**")
                .addResourceLocations(bookCoversAbsolutePath);
    }
}
