// com.fonestore.user_api.config.StaticResourceConfig (hoặc ở staff_api cũng được)
package com.fonestore.user_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/assets/**")
        .addResourceLocations(
            "classpath:/static/assets/",                // nếu sau này có
            "classpath:/public/assets/",                // nếu sau này có
            "classpath:/static/user-frontend/assets/",  // nếu có
            "classpath:/static/staff-frontend/assets/"  // ✅ ảnh bạn đang để ở đây
        );
  }
}
