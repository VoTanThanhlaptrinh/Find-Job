package com.nlu.identity.application.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nlu.identity.application.JwtService;
import com.nlu.identity.application.UserDetailsCacheService;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.domain.vo.EmailAddress;
import com.nlu.identity.domain.vo.Password;
import com.nlu.identity.domain.vo.PhoneNumber;
import com.nlu.identity.infrastructure.config.UserDetailsCacheProperties;
import com.nlu.identity.infrastructure.filter.JwtFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDetailsCacheServiceImplTest {

    private Map<String, byte[]> redisStore;
    private RedisTemplate<String, byte[]> cacheByteRedisTemplate;
    private ValueOperations<String, byte[]> valueOps;
    private UserDetailsService delegateUserDetailsService;
    private UserDetailsCacheProperties properties;
    private ObjectMapper objectMapper;
    private UserDetailsCacheServiceImpl cacheService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisStore = new ConcurrentHashMap<>();
        cacheByteRedisTemplate = mock(RedisTemplate.class);
        valueOps = mock(ValueOperations.class);

        lenient().when(cacheByteRedisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get(anyString())).thenAnswer(inv -> redisStore.get(inv.<String>getArgument(0)));
        lenient().doAnswer(inv -> {
            redisStore.put(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOps).set(anyString(), any(byte[].class), any(Duration.class));
        lenient().when(cacheByteRedisTemplate.delete(anyString()))
                .thenAnswer(inv -> redisStore.remove(inv.<String>getArgument(0)) != null);

        delegateUserDetailsService = mock(UserDetailsService.class);
        properties = new UserDetailsCacheProperties();
        properties.setEnabled(true);
        properties.setTtlSeconds(300L);

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        cacheService = new UserDetailsCacheServiceImpl(
                cacheByteRedisTemplate,
                delegateUserDetailsService,
                properties,
                objectMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loadUserByUsername_cachesPasswordFreeProjection_andEvictForcesReload() {
        User dbUser = buildDomainUser(42L, "alice@example.com", "ROLE_HIRER");
        when(delegateUserDetailsService.loadUserByUsername("alice@example.com")).thenReturn(dbUser);

        UserDetails first = cacheService.loadUserByUsername("alice@example.com");
        UserDetails second = cacheService.loadUserByUsername("alice@example.com");

        verify(delegateUserDetailsService, times(1)).loadUserByUsername("alice@example.com");

        byte[] storedJson = redisStore.get("auth:user:alice@example.com");
        assertNotNull(storedJson);
        String jsonText = new String(storedJson, StandardCharsets.UTF_8);
        assertFalse(jsonText.contains("secretHashValue"));

        User cachedDomainUser = assertInstanceOf(User.class, second);
        assertEquals(42L, cachedDomainUser.getId());
        assertEquals("alice@example.com", cachedDomainUser.getEmail());
        assertEquals("ROLE_HIRER", cachedDomainUser.getRole());
        assertNull(cachedDomainUser.getPassword());
        assertTrue(cachedDomainUser.isEnabled());
        assertTrue(cachedDomainUser.isActive());
        assertFalse(cachedDomainUser.isAccountLocked());
        assertEquals(first.getAuthorities(), cachedDomainUser.getAuthorities());

        cacheService.evictByEmail("alice@example.com");
        assertFalse(redisStore.containsKey("auth:user:alice@example.com"));

        cacheService.loadUserByUsername("alice@example.com");
        verify(delegateUserDetailsService, times(2)).loadUserByUsername("alice@example.com");
    }

    @Test
    void loadUserByUsername_whenDisabled_bypassesRedisAndAlwaysDelegates() {
        properties.setEnabled(false);
        User dbUser = buildDomainUser(7L, "bob@example.com", "ROLE_USER");
        when(delegateUserDetailsService.loadUserByUsername("bob@example.com")).thenReturn(dbUser);

        cacheService.loadUserByUsername("bob@example.com");
        cacheService.loadUserByUsername("bob@example.com");

        verify(delegateUserDetailsService, times(2)).loadUserByUsername("bob@example.com");
        verify(valueOps, never()).get(anyString());
        assertTrue(redisStore.isEmpty());
    }

    @Test
    void jwtFilter_prefersUserDetailsCacheServiceOverRawUserDetailsService() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        UserDetailsService fallbackUds = mock(UserDetailsService.class);
        UserDetailsCacheService cacheMock = mock(UserDetailsCacheService.class);

        User cachedUser = buildDomainUser(99L, "jwt@example.com", "ROLE_ADMIN");
        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("jwt@example.com");
        when(jwtService.isTokenValid("valid.jwt.token", cachedUser)).thenReturn(true);
        when(cacheMock.loadUserByUsername("jwt@example.com")).thenReturn(cachedUser);

        JwtFilter filter = new JwtFilter(jwtService, fallbackUds, cacheMock);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/users");
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        verify(cacheMock, times(1)).loadUserByUsername("jwt@example.com");
        verify(fallbackUds, never()).loadUserByUsername(anyString());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertSameUserId(99L, auth.getPrincipal());
    }

    private void assertSameUserId(long expectedId, Object principal) {
        User user = assertInstanceOf(User.class, principal);
        assertEquals(expectedId, user.getId());
    }

    private User buildDomainUser(long id, String email, String role) {
        return new User(
                id,
                "Test User " + id,
                new Password("$2a$10$secretHashValue"),
                LocalDate.of(1999, 5, 10),
                role,
                new EmailAddress(email),
                "Thu Duc, HCMC",
                new PhoneNumber("0912345678"),
                false,
                true,
                true,
                false,
                List.of(),
                List.of());
    }
}
