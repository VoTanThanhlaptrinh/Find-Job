package web_application.security;

import com.job_web.service.security.JwtFamilyService;
import com.job_web.service.security.impl.JwtServiceImpl;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {

    @Mock
    private JwtFamilyService jwtFamilyService;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private JwtServiceImpl jwtService;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(
                "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8)
        );
        ReflectionTestUtils.setField(jwtService, "secretkey", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 600_000L);
    }

    @Test
    void generateRefreshTokenUsesProvidedExpiration() {
        UserDetails user = new User(
                "hirer@test.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_HIRER"))
        );
        long refreshTtlMillis = Duration.ofDays(7).toMillis();
        Instant before = Instant.now();

        String token = jwtService.generateRefreshToken(user, refreshTtlMillis);

        Date expiration = jwtService.extractClaims(token, Claims::getExpiration);
        long actualTtlMillis = expiration.toInstant().toEpochMilli() - before.toEpochMilli();

        assertThat(actualTtlMillis).isBetween(refreshTtlMillis - 5_000, refreshTtlMillis + 5_000);
        verify(jwtFamilyService).saveFamilyJti(jwtService.extractFamily(token), jwtService.extractJTI(token));
    }
}
