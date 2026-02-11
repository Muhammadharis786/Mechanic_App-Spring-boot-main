package com.haris.MechanicApp.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ImageServe implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Iska matlab hai ke 'http://localhost:8080/uploads/...' se aane wali request
        // 'file:/path/to/your/project/upload/...' folder se serve hogi.
        registry.addResourceHandler("/uploads/**" )
                .addResourceLocations("file:upload/");
    }
}
