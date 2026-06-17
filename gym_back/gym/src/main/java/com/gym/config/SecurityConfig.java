package com.gym.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.gym.security.JwtAuthenticationFilter;
import com.gym.security.JwtEntryPoint;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtEntryPoint jwtEntryPoint;
    private final com.gym.security.JwtAccessDeniedHandler jwtAccessDeniedHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, JwtEntryPoint jwtEntryPoint,
                          com.gym.security.JwtAccessDeniedHandler jwtAccessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtEntryPoint = jwtEntryPoint;
        this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/auth/post-pago-login").permitAll()
                .requestMatchers("/api/usuario/pre-registro").permitAll()
                .requestMatchers("/api/usuario/retomar-pago").permitAll()
                .requestMatchers("/api/usuario/existe-dni/**").permitAll()
                .requestMatchers("/api/usuario/existe-email").permitAll()
                .requestMatchers("/api/usuario/existe-telefono").permitAll()
                .requestMatchers("/api/stripe/webhook").permitAll()
                .requestMatchers("/api/webhook/**").permitAll()
                .requestMatchers("/api/payment/create-checkout-session/**").permitAll()
                .requestMatchers("/api/payment/confirm").permitAll()
                .requestMatchers("/api/usuario/activar/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_PERSONAL")
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/clases/**").authenticated()
                .requestMatchers("/api/clases/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_PERSONAL")
                .requestMatchers("/api/payment/all").hasAnyAuthority("ROLE_ADMIN", "ROLE_PERSONAL")
                .requestMatchers("/api/personal/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_PERSONAL")
                .requestMatchers(HttpMethod.GET, "/api/usuario").hasAnyAuthority("ROLE_ADMIN", "ROLE_PERSONAL")
                .requestMatchers("/api/usuario/**").authenticated()
                .requestMatchers("/api/inscripciones/**").authenticated()
                .requestMatchers("/api/payment/last-payment").authenticated()
                .requestMatchers("/api/stripe/membresia-activa/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:4200",
            "https://*.vercel.app",
            "https://*.trycloudflare.com",
            "https://*.cloudflare.com"
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}