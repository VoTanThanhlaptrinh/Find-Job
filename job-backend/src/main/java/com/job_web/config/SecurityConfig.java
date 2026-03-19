package com.job_web.config;

import java.util.List;

import com.job_web.security.CustomOAuth2SuccessHandler;
import org.springframework.beans.factory.aspectj.ConfigurableObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.job_web.security.JwtFilter;
import com.job_web.security.VerifyRecoveryFillter;
import com.job_web.service.security.impl.UserRepositoryDetailsService;

import lombok.AllArgsConstructor;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtAuthFilter;
    private UserRepositoryDetailsService userDetailsService;
    private VerifyRecoveryFillter verifyRecoveryFillter;
    private CustomOAuth2SuccessHandler customOAuth2SuccessHandler;

    @Bean
    SecurityFilterChain restChain(HttpSecurity http) throws Exception {
        http.cors(c -> c.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/activation-links/**",
                                "/api/auth/activate/**",
                                "/api/auth/refresh-token",
                                "/api/auth/logout",
                                "/api/auth/google/**",
                                "/api/auth/password/**",
                                "/api/auth/status",
                                "/api/home/init",
                                "/error",
                                "/oauth2/**", "/login/oauth2/**", "/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/jobs/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/jobs/filter").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/blogs/**").permitAll()
                        .requestMatchers("/api/hirer/**").hasAuthority("ROLE_HIRER")
                        .requestMatchers("/api/user/**").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/account/roles/hirer").hasAuthority("ROLE_HIRER")
                        .requestMatchers("/api/account/roles/user").hasAuthority("ROLE_USER")
                        .requestMatchers("/api/account/**", "/api/blogs/**", "/api/users/**").authenticated()
                        .anyRequest().permitAll()
                ).addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(verifyRecoveryFillter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(o -> o
                        .successHandler(customOAuth2SuccessHandler)
                )
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:4200")); // không dùng "*"
        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS", "PUT", "PATCH", "DELETE"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        config.setExposedHeaders(List.of("Set-Cookie"));

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


