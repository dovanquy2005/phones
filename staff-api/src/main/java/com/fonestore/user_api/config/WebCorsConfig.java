package com.fonestore.user_api.config;

import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebCorsConfig {
  @Bean
  public CorsFilter corsFilter(@Value("${app.cors.allowed-origins:*}") String origins) {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowCredentials(true);
    cfg.setAllowedOriginPatterns(Arrays.asList(origins.split(",")));
    cfg.addAllowedHeader("*");
    cfg.addAllowedMethod("*");
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return new CorsFilter(source);
  }
}
