package org.ouracademy.srtomy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Component
public class CorsConfiguration {

    @Value("${cors_origin}")
    private String corsOrigin;

    public void configure(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(corsOrigin)
                .allowedMethods("*");
    }

}
