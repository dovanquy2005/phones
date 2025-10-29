package com.fonestore.staff_api.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(reg -> reg

                // 1) Staff Users: phân quyền chi tiết theo method
                .requestMatchers(HttpMethod.GET,    "/api/staff/users", "/api/staff/users/**")
                    .hasAnyAuthority("staff","manager")
                .requestMatchers(HttpMethod.POST,   "/api/staff/users", "/api/staff/users/**")
                    .hasAuthority("manager")
                .requestMatchers(HttpMethod.PUT,    "/api/staff/users/**")
                    .hasAuthority("manager")
                .requestMatchers(HttpMethod.PATCH,  "/api/staff/users/**")
                    .hasAuthority("manager")
                .requestMatchers(HttpMethod.DELETE, "/api/staff/users/**")
                    .hasAuthority("manager")
                
                    // VOUCHERS
                .requestMatchers(HttpMethod.GET,    "/api/staff/vouchers", "/api/staff/vouchers/**")
                    .hasAnyAuthority("staff","manager")
                .requestMatchers(HttpMethod.POST,   "/api/staff/vouchers/**")
                    .hasAuthority("manager")
                .requestMatchers(HttpMethod.PATCH,  "/api/staff/vouchers/**")
                    .hasAuthority("manager")
                .requestMatchers(HttpMethod.DELETE, "/api/staff/vouchers/**")
                    .hasAuthority("manager")


                // ===== PUBLIC =====
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/", "/index.html", "/favicon.ico",
                        "/user-frontend/**", "/staff-frontend/**",
                        "/assets/**", "/images/**", "/css/**", "/js/**"
                ).permitAll()

                // Auth endpoints
                .requestMatchers("/api/auth/**").permitAll()

                // Public APIs (read-only)
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/products/**",
                        "/api/assets/**",
                        "/api/brands/**"
                ).permitAll()

                // Preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // ===== USER =====
                .requestMatchers("/api/user/**").hasAuthority("user")

                // ===== STAFF =====
                .requestMatchers("/api/staff/**").hasAnyAuthority("staff", "manager")

                // STAFF / MANAGER quản lý sản phẩm
                .requestMatchers(HttpMethod.POST,   "/api/products/**").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyAuthority("staff", "manager")

                // ===== ORDERS (Staff/Manager) =====
                .requestMatchers(HttpMethod.GET,    "/api/orders/**").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.POST,   "/api/orders/**").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.PUT,    "/api/orders/**").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasAnyAuthority("staff", "manager")
                // ✅ NEW: Cho phép đổi trạng thái bằng PATCH
                .requestMatchers(HttpMethod.PATCH,  "/api/orders/*/status").hasAnyAuthority("staff", "manager")

                // ===== PAYMENTS (Staff/Manager) =====
                .requestMatchers(HttpMethod.GET,   "/api/orders/*/payment").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.POST,  "/api/orders/*/payment").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.PATCH, "/api/orders/*/payment-status").hasAnyAuthority("staff", "manager")

                
            
                // ===== CUSTOMERS =====
                .requestMatchers("/api/customers").hasAnyAuthority("staff","manager")
                .requestMatchers("/api/customers/**").hasAnyAuthority("staff","manager")
                .requestMatchers("/api/customers/*/orders").hasAnyAuthority("staff","manager")


                // ===== MANAGER =====
                .requestMatchers("/api/admin/**").hasAuthority("manager")

                // Các API còn lại → cần đăng nhập
                .anyRequest().authenticated()
            )

            // Trả về 401 khi chưa đăng nhập
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
            )

            // Thêm JWT Filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
            "http://localhost:9090", "http://127.0.0.1:9090",
            "http://localhost:5173", "http://127.0.0.1:5173",
            "http://localhost:5500", "http://127.0.0.1:5500"
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setExposedHeaders(List.of("Location"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
