package web_application.support;

import com.job_web.constant.ApiConstants;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ApiConstants.PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, ApiConstants.PUBLIC_GET_ENDPOINTS).permitAll()
                        .requestMatchers(ApiConstants.HIRER_ENDPOINTS).hasAnyAuthority("ROLE_HIRER", "HIRER")
                        .requestMatchers(ApiConstants.USER_ENDPOINTS).hasAnyAuthority("ROLE_USER", "USER")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        new AntPathRequestMatcher("/api/**")
                ));

        return http.build();
    }
}
