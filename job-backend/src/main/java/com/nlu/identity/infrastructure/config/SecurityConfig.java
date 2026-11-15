package com.nlu.identity.infrastructure.config;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

import com.nlu.identity.domain.vo.ApiConstants;
import com.nlu.identity.infrastructure.filter.CustomOAuth2SuccessHandler;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.nlu.identity.infrastructure.filter.JwtFilter;
import com.nlu.identity.infrastructure.filter.RateLimitFilter;
import com.nlu.shared.infrastructure.cache.HotCacheConstants;
import com.nlu.shared.infrastructure.cache.ProtectedHotGetCacheFilter;
import com.nlu.shared.infrastructure.cache.PublicHotGetCacheFilter;
import com.nlu.shared.infrastructure.filter.RequestLoggingFilter;
import com.nlu.shared.infrastructure.idempotency.IdempotencyFilter;
import com.nlu.identity.infrastructure.filter.VerifyRecoveryFilter;
import com.nlu.identity.application.impl.UserRepositoryDetailsService;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;
    private final RequestLoggingFilter requestLoggingFilter;
    private final IdempotencyFilter idempotencyFilter;
    private final PublicHotGetCacheFilter publicHotGetCacheFilter;
    private final ProtectedHotGetCacheFilter protectedHotGetCacheFilter;
    private final UserRepositoryDetailsService userDetailsService;
    private final VerifyRecoveryFilter verifyRecoveryFilter;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.cors.allowed-origins:}")
    private String corsAllowedOrigins = "";

    @Value("${app.cors.allowed-origin-patterns:}")
    private String corsAllowedOriginPatterns = "";

    @Bean
    SecurityFilterChain restChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(ApiConstants.PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/api/blogs/me").authenticated()
                        .requestMatchers(HttpMethod.GET, ApiConstants.PUBLIC_GET_ENDPOINTS).permitAll()

                        .requestMatchers(ApiConstants.HIRER_ENDPOINTS).hasAnyAuthority("ROLE_HIRER", "HIRER")
                        .requestMatchers(ApiConstants.USER_ENDPOINTS).hasAnyAuthority("ROLE_USER", "USER")
                        .requestMatchers(ApiConstants.ADMIN_ENDPOINTS).hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                        // 3. Các endpoint Yêu cầu đăng nhập nói chung đưa xuống DƯỚI
                        .requestMatchers(ApiConstants.AUTHENTICATED_ENDPOINTS).authenticated()

                        .anyRequest().denyAll())
                .addFilterBefore(requestLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(publicHotGetCacheFilter, RateLimitFilter.class)
                .addFilterAfter(jwtAuthFilter, PublicHotGetCacheFilter.class)
                .addFilterAfter(idempotencyFilter, JwtFilter.class)
                .addFilterBefore(verifyRecoveryFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(protectedHotGetCacheFilter, AuthorizationFilter.class)
                .oauth2Login(o -> o
                        .successHandler(customOAuth2SuccessHandler))
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")))
                .authenticationProvider(authenticationProvider());
        return http.build();
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> allowedOrigins = Stream.concat(
                        parseOrigins(frontendUrl).stream(),
                        parseOrigins(corsAllowedOrigins).stream())
                .distinct()
                .toList();

        List<String> allowedOriginPatterns = parseOrigins(corsAllowedOriginPatterns);

        if (!allowedOrigins.isEmpty()) {
            config.setAllowedOrigins(allowedOrigins);
        } else if (allowedOriginPatterns.isEmpty() && frontendUrl != null) {
            config.setAllowedOrigins(List.of(frontendUrl));
        }
        if (!allowedOriginPatterns.isEmpty()) {
            config.setAllowedOriginPatterns(allowedOriginPatterns);
        }

        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Set-Cookie", "X-Correlation-ID", "X-RateLimit-Limit", "X-RateLimit-Remaining",
                "X-RateLimit-Reset", "Retry-After", "X-Idempotent-Replayed", HotCacheConstants.HEADER_CACHE_STATUS));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    FilterRegistrationBean<CorsFilter> corsFilterRegistration(UrlBasedCorsConfigurationSource corsConfigurationSource) {
        CorsFilter corsFilter = new CorsFilter(corsConfigurationSource) {
            @Override
            protected boolean shouldNotFilterErrorDispatch() {
                return false;
            }

            @Override
            protected boolean shouldNotFilterAsyncDispatch() {
                return false;
            }
        };
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(corsFilter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR, DispatcherType.ASYNC));
        return registration;
    }

    private List<String> parseOrigins(String rawOrigins) {
        if (rawOrigins == null || rawOrigins.isBlank()) {
            return List.of();
        }
        return Arrays.stream(rawOrigins.split(","))
                .map(String::trim)
                .map(origin -> origin.replaceAll("/+$", ""))
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    @Bean
    AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();
        authenticationProvider.setPasswordEncoder(encode());
        authenticationProvider.setUserDetailsService(userDetailsService);
        return authenticationProvider;
    }

    @Bean
    PasswordEncoder encode() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    HttpFirewall defaultHttpFirewall() {
        return new DefaultHttpFirewall();
    }
}
