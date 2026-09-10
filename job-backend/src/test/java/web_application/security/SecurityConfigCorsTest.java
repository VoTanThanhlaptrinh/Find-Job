package web_application.security;

import com.nlu.identity.application.impl.UserRepositoryDetailsService;
import com.nlu.identity.infrastructure.config.SecurityConfig;
import com.nlu.identity.infrastructure.filter.CustomOAuth2SuccessHandler;
import com.nlu.identity.infrastructure.filter.JwtFilter;
import com.nlu.identity.infrastructure.filter.RateLimitFilter;
import com.nlu.identity.infrastructure.filter.VerifyRecoveryFilter;
import com.nlu.shared.infrastructure.filter.RequestLoggingFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigCorsTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(
                mock(JwtFilter.class),
                mock(RateLimitFilter.class),
                mock(RequestLoggingFilter.class),
                mock(UserRepositoryDetailsService.class),
                mock(VerifyRecoveryFilter.class),
                mock(CustomOAuth2SuccessHandler.class)
        );
    }

    @Test
    void testCorsConfigurationWithSingleOrigin() {
        ReflectionTestUtils.setField(securityConfig, "frontendUrl", "http://localhost:4200");

        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) ReflectionTestUtils.invokeMethod(securityConfig, "corsConfigurationSource");
        assertThat(source).isNotNull();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/jobs");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:4200");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }

    @Test
    void testCorsConfigurationWithMultipleOriginsAndTrailingSlashes() {
        ReflectionTestUtils.setField(securityConfig, "frontendUrl", "http://localhost:4200/, https://job-frontend-gray.vercel.app/");

        UrlBasedCorsConfigurationSource source = (UrlBasedCorsConfigurationSource) ReflectionTestUtils.invokeMethod(securityConfig, "corsConfigurationSource");
        assertThat(source).isNotNull();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/jobs");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly("http://localhost:4200", "https://job-frontend-gray.vercel.app");
    }
}
