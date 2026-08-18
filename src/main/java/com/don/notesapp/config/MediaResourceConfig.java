package com.don.notesapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {
    // Media is now served directly from Cloudinary
    // No local resource handling needed
}

