package web_application.security;

import com.nlu.identity.application.JwtFamilyService;
import com.nlu.identity.application.JwtService;
import com.nlu.identity.application.impl.RefreshTokenServiceImpl;
import com.nlu.identity.domain.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private JwtFamilyService jwtFamilyService;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @Test
    void isValid_BlankToken_ReturnsFalse() {
        assertThat(refreshTokenService.isValid("  ")).isFalse();
        assertThat(refreshTokenService.isValid(null)).isFalse();
    }

    @Test
    void deleteRefreshToken_BlankToken_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> refreshTokenService.deleteRefreshToken(""));
    }

    @Test
    void deleteRefreshToken_ExpiredJwtStillRevokesFamilyFromClaims() {
        Claims expiredClaims = Jwts.claims().add("familyId", "fam-123").build();
        when(jwtService.extractFamily("expired-rt"))
                .thenThrow(new ExpiredJwtException(null, expiredClaims, "expired"));

        refreshTokenService.deleteRefreshToken("expired-rt");

        verify(jwtFamilyService).deleteFamily("fam-123");
    }
}

