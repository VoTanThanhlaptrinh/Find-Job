package web_application.application;

import com.nlu.applicationProcess.infrastructure.redis.ResumeUploadLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeUploadLockServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ResumeUploadLockService lockService;

    @BeforeEach
    void setUp() {
        lockService = new ResumeUploadLockService(redisTemplate);
    }

    @Test
    @DisplayName("Acquire lock thành công với đúng key, TTL 60s và token")
    void acquireLock_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("resume-upload:initiate:42:idemp-123"), eq("token-abc"), eq(Duration.ofSeconds(60))))
                .thenReturn(Boolean.TRUE);

        Optional<String> result = lockService.acquireLock(42L, "idemp-123", "token-abc");

        assertTrue(result.isPresent());
        assertEquals("token-abc", result.get());
    }

    @Test
    @DisplayName("Acquire lock trả về empty khi đã có process khác giữ lock")
    void acquireLock_AlreadyLocked() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class)))
                .thenReturn(Boolean.FALSE);

        Optional<String> result = lockService.acquireLock(42L, "idemp-123", "token-abc");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Acquire lock graceful degradation khi Redis gặp lỗi kết nối")
    void acquireLock_RedisException_ReturnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        Optional<String> result = lockService.acquireLock(42L, "idemp-123", "token-abc");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Release lock trả về false khi token null")
    void releaseLock_NullToken_ReturnsFalse() {
        boolean released = lockService.releaseLock(42L, "idemp-123", null);
        assertFalse(released);
        verify(redisTemplate, never()).execute(any(), anyList(), any());
    }

    @Test
    @DisplayName("Release lock thành công khi token khớp qua Lua script compare-and-delete")
    void releaseLock_Success_WhenTokenMatches() {
        when(redisTemplate.execute(any(RedisScript.class), eq(Collections.singletonList("resume-upload:initiate:42:idemp-123")), eq("token-abc")))
                .thenReturn(1L);

        boolean released = lockService.releaseLock(42L, "idemp-123", "token-abc");

        assertTrue(released);
    }

    @Test
    @DisplayName("Release lock thất bại khi token không khớp (Lua script trả về 0)")
    void releaseLock_TokenMismatch_ReturnsFalse() {
        when(redisTemplate.execute(any(RedisScript.class), eq(Collections.singletonList("resume-upload:initiate:42:idemp-123")), eq("wrong-token")))
                .thenReturn(0L);

        boolean released = lockService.releaseLock(42L, "idemp-123", "wrong-token");

        assertFalse(released);
    }

    @Test
    @DisplayName("Release lock graceful degradation khi Redis gặp exception")
    void releaseLock_RedisException_ReturnsFalse() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any()))
                .thenThrow(new RuntimeException("Redis timeout"));

        boolean released = lockService.releaseLock(42L, "idemp-123", "token-abc");

        assertFalse(released);
    }

    @Test
    @DisplayName("Kiểm tra serializer thực tế: KeySerializer là StringRedisSerializer, ValueSerializer là GenericJackson2JsonRedisSerializer")
    void testRealSerializerCompatibility() {
        RedisTemplate<String, Object> realTemplate = new RedisTemplate<>();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        realTemplate.setKeySerializer(stringSerializer);
        realTemplate.setValueSerializer(jsonSerializer);

        String lockKey = lockService.buildLockKey(100L, "my-key");
        byte[] serializedKey = stringSerializer.serialize(lockKey);
        assertNotNull(serializedKey);
        assertEquals("resume-upload:initiate:100:my-key", stringSerializer.deserialize(serializedKey));

        String token = "uuid-token-123";
        byte[] serializedValue = jsonSerializer.serialize(token);
        assertNotNull(serializedValue);
        assertEquals("\"uuid-token-123\"", new String(serializedValue));
    }
}
