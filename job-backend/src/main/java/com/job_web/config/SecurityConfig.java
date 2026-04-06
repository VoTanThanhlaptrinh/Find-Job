package com.job_web.config;

import java.util.List;

import com.job_web.constant.ApiConstants;
import com.job_web.security.CustomOAuth2SuccessHandler;
import org.springframework.beans.factory.aspectj.ConfigurableObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.job_web.security.JwtFilter;
import com.job_web.security.RateLimitFilter;
import com.job_web.security.VerifyRecoveryFillter;
import com.job_web.service.security.impl.UserRepositoryDetailsService;

import lombok.AllArgsConstructor;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private UserRepositoryDetailsService userDetailsService;
    private VerifyRecoveryFillter verifyRecoveryFillter;
    private CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    @Bean
    SecurityFilterChain restChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // 1. Các endpoint Public để trên cùng
                        .requestMatchers(ApiConstants.PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, ApiConstants.PUBLIC_GET_ENDPOINTS).permitAll()

                        // 2. Các endpoint Yêu cầu Role cụ thể đưa lên TRƯỚC
                        .requestMatchers(ApiConstants.HIRER_ENDPOINTS).hasAnyAuthority("ROLE_HIRER", "HIRER")
                        .requestMatchers(ApiConstants.USER_ENDPOINTS).hasAnyAuthority("ROLE_USER", "USER")

                        // 3. Các endpoint Yêu cầu đăng nhập nói chung đưa xuống DƯỚI
                        .requestMatchers(ApiConstants.AUTHENTICATED_ENDPOINTS).authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(verifyRecoveryFillter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(o -> o
                        .successHandler(customOAuth2SuccessHandler)
                ).exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")))
                .authenticationProvider(authenticationProvider());
        return http.build();
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("""
                http://localhost:4200""", """
                https://findjob-xi.vercel.app"""));
        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        config.setExposedHeaders(List.of("Set-Cookie", "X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset", "Retry-After"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setPasswordEncoder(encode());
        authenticationProvider.setUserDetailsService(userDetailsService);
        return authenticationProvider;
    }


    @Bean
    BCryptPasswordEncoder encode() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    HttpFirewall defaultHttpFirewall() {
        return new DefaultHttpFirewall();
    }
}


