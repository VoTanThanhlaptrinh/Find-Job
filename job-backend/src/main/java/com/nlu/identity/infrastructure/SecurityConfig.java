package com.nlu.identity.infrastructure;

import java.util.List;

import com.nlu.identity.domain.vo.ApiConstants;
import com.nlu.identity.infrastructure.filter.CustomOAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.nlu.identity.infrastructure.filter.JwtFilter;
import com.nlu.identity.infrastructure.filter.RateLimitFilter;
import com.nlu.shared.infrastructure.filter.RequestLoggingFilter;
import com.nlu.identity.infrastructure.filter.VerifyRecoveryFilter;
import com.nlu.identity.application.impl.UserRepositoryDetailsService;

import lombok.AllArgsConstructor;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final RequestLoggingFilter requestLoggingFilter;
    private UserRepositoryDetailsService userDetailsService;
    private VerifyRecoveryFilter verifyRecoveryFilter;
    private CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    @Bean
    SecurityFilterChain restChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ApiConstants.PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, ApiConstants.PUBLIC_GET_ENDPOINTS).permitAll()

                        .requestMatchers(ApiConstants.HIRER_ENDPOINTS).hasAnyAuthority("ROLE_HIRER", "HIRER")
                        .requestMatchers(ApiConstants.USER_ENDPOINTS).hasAnyAuthority("ROLE_USER", "USER")
                        .requestMatchers(ApiConstants.ADMIN_ENDPOINTS).hasAnyAuthority("ROLE_ADMIN", "ADMIN")

                        // 3. Các endpoint Yêu cầu đăng nhập nói chung đưa xuống DƯỚI
                        .requestMatchers(ApiConstants.AUTHENTICATED_ENDPOINTS).authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(verifyRecoveryFilter, UsernamePasswordAuthenticationFilter.class)
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
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Correlation-ID"));
        config.setExposedHeaders(List.of("Set-Cookie", "X-Correlation-ID", "X-RateLimit-Limit", "X-RateLimit-Remaining", "X-RateLimit-Reset", "Retry-After"));

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


