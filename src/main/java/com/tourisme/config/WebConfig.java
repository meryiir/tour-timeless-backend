package com.tourisme.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        // Convert to file URL format - Spring handles Windows paths correctly
        String fileUrl = uploadPath.toUri().toString();
        if (!fileUrl.endsWith("/")) {
            fileUrl += "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(fileUrl);
    }
}
