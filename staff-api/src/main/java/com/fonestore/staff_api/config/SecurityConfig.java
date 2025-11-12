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
                
              
                // ===== STAFF USERS =====
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

                // ===== VOUCHERS =====
                .requestMatchers(HttpMethod.GET,    "/api/staff/vouchers", "/api/staff/vouchers/**")
                    .hasAnyAuthority("staff","manager")
                .requestMatchers(HttpMethod.POST,   "/api/staff/vouchers/**")
                    .hasAuthority("manager")
                .requestMatchers(HttpMethod.PATCH,  "/api/staff/vouchers/**")
                    .hasAuthority("manager")
                .requestMatchers(HttpMethod.DELETE, "/api/staff/vouchers/**")
                    .hasAuthority("manager")

                // ===== PUBLIC STATIC =====
                .requestMatchers("/uploads/**","/images/**","/api/public/**").permitAll()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/", "/index.html", "/favicon.ico",
                        "/user-frontend/**", "/staff-frontend/**",
                        "/assets/**", "/images/**", "/css/**", "/js/**"
                ).permitAll()

                // ===== AUTH / PUBLIC READ =====
                .requestMatchers("/api/auth/**").permitAll()
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

                // ===== STAFF (bao gồm MANAGER) =====
                .requestMatchers("/api/staff/**").hasAnyAuthority("staff", "manager")

                // Quản lý sản phẩm (staff/manager)
                .requestMatchers(HttpMethod.POST,   "/api/products/**").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyAuthority("staff", "manager")

                // ===== ORDERS (staff/manager) =====
                .requestMatchers(HttpMethod.GET,    "/api/orders/**").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.POST,   "/api/orders/**").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.PUT,    "/api/orders/**").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.PATCH,  "/api/orders/*/status").hasAnyAuthority("staff", "manager")

                // ===== PAYMENTS (staff/manager) =====
                .requestMatchers(HttpMethod.GET,   "/api/orders/*/payment").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.POST,  "/api/orders/*/payment").hasAnyAuthority("staff", "manager")
                .requestMatchers(HttpMethod.PATCH, "/api/orders/*/payment-status").hasAnyAuthority("staff", "manager")

                // ===== CUSTOMERS (staff/manager) =====
                .requestMatchers("/api/customers").hasAnyAuthority("staff","manager")
                .requestMatchers("/api/customers/**").hasAnyAuthority("staff","manager")
                .requestMatchers("/api/customers/*/orders").hasAnyAuthority("staff","manager")

                // ===== REPORTS (đúng path, có "s") — chỉ MANAGER =====
                // Cho phép manager (hoặc tạm cả staff nếu bạn muốn test)
                .requestMatchers("/api/reports/**", "/api/report/**").hasAnyAuthority("manager","staff")


                // ===== MANAGER =====
                .requestMatchers("/api/admin/**").hasAuthority("manager")
                

                // Các request còn lại → cần đăng nhập
                .anyRequest().authenticated()
            )

            // 401 khi chưa đăng nhập, 403 khi thiếu quyền
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                .accessDeniedHandler((req, res, e) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
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
        // expose để FE tải file Excel (Content-Disposition)
        cfg.setExposedHeaders(List.of("Location", "Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
