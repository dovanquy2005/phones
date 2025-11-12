package com.fonestore.user_api.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class UserSecurityConfig {

    private final UserJwtAuthFilter userJwtAuthFilter;

    public UserSecurityConfig(UserJwtAuthFilter userJwtAuthFilter) {
        this.userJwtAuthFilter = userJwtAuthFilter;
    }

    /**
     * SecurityFilterChain dành cho user_api.
     * ĐẶT securityMatcher("/api/**") để chain này chỉ áp dụng cho /api/**
     * và tránh va chạm với staff_api's SecurityFilterChain.
     */
    @Bean("userSecurityFilterChain")
    @Order(1)
    public SecurityFilterChain userSecurityFilterChain(HttpSecurity http) throws Exception {

        // IMPORTANT: only match /api/** so this chain won't be "anyRequest"
        http.securityMatcher("/api/**");

        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(reg -> {
            // explicit allow for merge first (most specific) - dev/test only
            reg.requestMatchers(HttpMethod.POST, "/api/cart/merge").permitAll();
            reg.requestMatchers(HttpMethod.POST, "/api/cart/post").permitAll();
            // static + frontend assets under /api/** (if any) or general public GETs
            reg.requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll();
            reg.requestMatchers(HttpMethod.GET,
                    "/", "/index.html", "/favicon.ico",
                    "/user-frontend/**", "/staff-frontend/**",
                    "/assets/**", "/images/**", "/css/**", "/js/**"
            ).permitAll();

            // public endpoints
            reg.requestMatchers("/api/auth/**").permitAll();
            reg.requestMatchers("/api/public/**").permitAll();
            reg.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

            // public read endpoints
            reg.requestMatchers(HttpMethod.GET,
                    "/api/products/**",
                    "/api/brands/**",
                    "/api/assets/**"
            ).permitAll();

            // protected user endpoints
            reg.requestMatchers("/api/cart", "/api/cart/items/**", "/api/user/**").hasAuthority("user");

            // everything else under /api/** requires authentication
            reg.anyRequest().authenticated();
        });

        // return clean 401 / 403
        http.exceptionHandling(ex -> ex
            .authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
            .accessDeniedHandler((req, res, e) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
        );

        // register the user-specific JWT filter
        http.addFilterBefore(userJwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Provide a CORS config only if none exists already (avoid bean collision with staff_api).
     */
    @Bean
    @ConditionalOnMissingBean(CorsConfigurationSource.class)
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
            "http://localhost:9090", "http://127.0.0.1:9090",
            "http://localhost:5173", "http://127.0.0.1:5173",
            "http://localhost:5500", "http://127.0.0.1:5500"
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cfg.setAllowCredentials(true);
        cfg.setExposedHeaders(List.of("Location", "Content-Disposition", "Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
