package com.job_web.config;

import java.util.List;

import com.job_web.security.CustomOAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.job_web.security.JwtFilter;
import com.job_web.security.VerifyRecoveryFillter;
import com.job_web.service.impl.UserRepositoryDetailsService;

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
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		// TODO Auto-generated method stub
		return http.cors(c -> c.configurationSource(corsConfigurationSource()))
				.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(requests -> {
					requests.antMatchers("/api/account/**"
							,"/error"
							,"/api/home/init"
							,"/api/job/pub/**"
							,"/api/blog/pub/**"
							,"/auth/**"
							,"/oauth2"
							).permitAll();
					requests.anyRequest().authenticated();
				}).oauth2Login(httpSecurityOAuth2LoginConfigurer ->{
					httpSecurityOAuth2LoginConfigurer.successHandler(customOAuth2SuccessHandler);
				})
				.headers(headers -> headers.frameOptions().sameOrigin())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authenticationProvider(authenticationProvider())
				.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(verifyRecoveryFillter, UsernamePasswordAuthenticationFilter.class).build();
	}

	@Bean
	UrlBasedCorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();

		config.setAllowedOrigins(List.of("http://localhost:4200")); // không dùng "*"
		config.setAllowCredentials(true);
		config.setAllowedMethods(List.of("GET","POST","OPTIONS"));
		config.setAllowedHeaders(List.of("Content-Type","Authorization"));
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
	AuthenticationManager authenticationManager() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(encode());
		return new ProviderManager(authProvider);
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
