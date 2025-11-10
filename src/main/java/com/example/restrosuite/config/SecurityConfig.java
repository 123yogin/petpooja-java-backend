package com.example.restrosuite.config;

import com.example.restrosuite.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/customer/**").permitAll() // Allow public access to customer endpoints
                .requestMatchers("/customer/**").permitAll() // Allow public access to customer frontend pages
                .requestMatchers("/ws/**").permitAll() // Allow WebSocket connections
                .requestMatchers("/api/menu").permitAll() // Allow all to view menu
                .requestMatchers("/api/menu/**").hasAnyAuthority("ADMIN") // Only ADMIN can modify
                .requestMatchers("/api/tables").permitAll() // Allow all to view tables
                .requestMatchers("/api/tables/**").hasAnyAuthority("ADMIN", "CASHIER") // ADMIN/CASHIER can modify
                .requestMatchers("/api/orders/create").hasAnyAuthority("CASHIER", "ADMIN")
                .requestMatchers("/api/orders").hasAnyAuthority("CASHIER", "ADMIN", "KITCHEN", "MANAGER") // All can view orders
                .requestMatchers("/api/orders/*/status").hasAnyAuthority("KITCHEN", "ADMIN", "CASHIER") // Allow CASHIER to update status
                .requestMatchers("/api/analytics/**").hasAnyAuthority("ADMIN", "MANAGER")
                .requestMatchers("/api/billing/**").hasAnyAuthority("CASHIER", "ADMIN", "MANAGER") // Allow MANAGER to view bills
                .requestMatchers("/api/inventory/**").hasAnyAuthority("ADMIN") // Inventory endpoints
                .requestMatchers("/api/suppliers/**").hasAnyAuthority("ADMIN") // Supplier management (ADMIN only)
                .requestMatchers("/api/purchase-orders/**").hasAnyAuthority("ADMIN") // Purchase order management (ADMIN only)
                .requestMatchers("/api/customers/**").hasAnyAuthority("ADMIN", "CASHIER", "MANAGER") // Customer management
                .requestMatchers("/api/tasks/**").hasAnyAuthority("ADMIN", "MANAGER", "CASHIER", "KITCHEN") // All roles can access tasks
                .requestMatchers("/api/employees/**").hasAnyAuthority("ADMIN", "MANAGER") // Employee management (ADMIN, MANAGER)
                .requestMatchers("/api/attendance/**").hasAnyAuthority("ADMIN", "MANAGER", "CASHIER", "KITCHEN") // All roles can mark attendance
                .requestMatchers("/api/leaves/**").hasAnyAuthority("ADMIN", "MANAGER", "CASHIER", "KITCHEN") // All roles can access leaves
                .requestMatchers("/api/payroll/**").hasAnyAuthority("ADMIN", "MANAGER") // Payroll management (ADMIN, MANAGER only)
                .requestMatchers("/api/payments/**").hasAnyAuthority("ADMIN", "CASHIER", "MANAGER") // Payment management
                .requestMatchers("/api/accounts-receivable/**").hasAnyAuthority("ADMIN", "CASHIER", "MANAGER") // Accounts receivable
                .requestMatchers("/api/outlets/**").hasAnyAuthority("ADMIN", "MANAGER") // Outlet management (ADMIN, MANAGER only)
                .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow localhost for development and common production domains
        // Use setAllowedOriginPatterns for wildcard support
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:*",
            "https://*.vercel.app",
            "https://*.netlify.app"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}

