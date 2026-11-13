package com.nlu.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.infrastructure.CurrentUserArgumentResolver;
import com.nlu.payment.api.PaymentController;
import com.nlu.payment.api.dto.req.CreatePaymentRequest;
import com.nlu.payment.application.impl.PaymentServiceImpl;
import com.nlu.payment.domain.model.Payment;
import com.nlu.payment.domain.model.PaymentMethod;
import com.nlu.payment.domain.repository.PaymentRepository;
import com.nlu.shared.domain.exception.GlobalExceptionHandler;
import com.nlu.shared.infrastructure.idempotency.IdempotencyConstants;
import com.nlu.shared.infrastructure.idempotency.IdempotencyFilter;
import com.nlu.shared.infrastructure.idempotency.IdempotencyProperties;
import com.nlu.shared.infrastructure.idempotency.IdempotencyRecord;
import com.nlu.shared.infrastructure.idempotency.IdempotencyService;
import com.nlu.shared.infrastructure.idempotency.IdempotencyStatus;
import com.nlu.shared.utils.MessageUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IdempotencyFilterAndPaymentTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private PaymentRepository paymentRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Map<String, Object> fakeRedisStore;
    private Map<Long, Payment> fakePaymentDb;
    private AtomicLong idSequence;
    private User testUser;

    @BeforeEach
    void setUp() {
        new MessageUtils(new StaticMessageSource());

        objectMapper = new ObjectMapper().findAndRegisterModules();
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        fakeRedisStore = new ConcurrentHashMap<>();
        fakePaymentDb = new ConcurrentHashMap<>();
        idSequence = new AtomicLong(1);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(anyString(), any(), any(Duration.class)))
                .thenAnswer(invocation -> {
                    String key = invocation.getArgument(0);
                    Object value = invocation.getArgument(1);
                    return fakeRedisStore.putIfAbsent(key, value) == null;
                });
        lenient().when(valueOperations.get(anyString()))
                .thenAnswer(invocation -> fakeRedisStore.get(invocation.<String>getArgument(0)));
        lenient().doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            Object value = invocation.getArgument(1);
            fakeRedisStore.put(key, value);
            return null;
        }).when(valueOperations).set(anyString(), any(), any(Duration.class));
        lenient().when(redisTemplate.delete(anyString()))
                .thenAnswer(invocation -> fakeRedisStore.remove(invocation.<String>getArgument(0)) != null);

        lenient().when(paymentRepository.save(any(Payment.class)))
                .thenAnswer(invocation -> {
                    Payment payment = invocation.getArgument(0);
                    if (payment.getId() == null) {
                        payment.setId(idSequence.getAndIncrement());
                    }
                    fakePaymentDb.put(payment.getId(), payment);
                    return payment;
                });
        lenient().when(paymentRepository.findByUserIdOrderByCreatedAtDesc(any(Long.class)))
                .thenAnswer(invocation -> {
                    Long uid = invocation.getArgument(0);
                    return fakePaymentDb.values().stream()
                            .filter(p -> uid.equals(p.getUserId()))
                            .toList();
                });

        IdempotencyProperties properties = new IdempotencyProperties();
        IdempotencyService idempotencyService = new IdempotencyService(redisTemplate, properties, objectMapper);
        IdempotencyFilter idempotencyFilter = new IdempotencyFilter(properties, idempotencyService, objectMapper);

        PaymentServiceImpl paymentService = new PaymentServiceImpl(paymentRepository);
        PaymentController paymentController = new PaymentController(paymentService);

        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .addFilters(idempotencyFilter)
                .build();

        testUser = new User();
        testUser.setId(101L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPayment_withoutIdempotencyKey_returnsBadRequestAndDoesNotSave() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("199000.00"),
                "VND",
                PaymentMethod.VNPAY,
                "Order #1001"
        );

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(paymentRepository, never()).save(any(Payment.class));
        assertThat(fakePaymentDb).isEmpty();
    }

    @Test
    void createPayment_sameIdempotencyKeyAndPayload_replaysCachedResponseAndSavesOnce() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("499000.00"),
                "VND",
                PaymentMethod.MOMO,
                "Pro tier subscription"
        );
        String body = objectMapper.writeValueAsString(request);
        String idempotencyKey = "pay-key-001";

        MvcResult firstResult = mockMvc.perform(post("/api/payments")
                        .header(IdempotencyConstants.HEADER_IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string(IdempotencyConstants.HEADER_IDEMPOTENT_REPLAYED, "false"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andReturn();

        MvcResult secondResult = mockMvc.perform(post("/api/payments")
                        .header(IdempotencyConstants.HEADER_IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string(IdempotencyConstants.HEADER_IDEMPOTENT_REPLAYED, "true"))
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());

        assertThat(secondJson.get("data").get("id").asLong())
                .isEqualTo(firstJson.get("data").get("id").asLong());
        assertThat(secondJson.get("data").get("transactionCode").asText())
                .isEqualTo(firstJson.get("data").get("transactionCode").asText());

        verify(paymentRepository, times(1)).save(any(Payment.class));
        assertThat(fakePaymentDb).hasSize(1);
    }

    @Test
    void createPayment_sameIdempotencyKeyWithDifferentPayload_returnsConflict() throws Exception {
        CreatePaymentRequest firstRequest = new CreatePaymentRequest(
                new BigDecimal("100000.00"),
                "VND",
                PaymentMethod.BANK_TRANSFER,
                "First order"
        );
        CreatePaymentRequest modifiedRequest = new CreatePaymentRequest(
                new BigDecimal("250000.00"),
                "VND",
                PaymentMethod.BANK_TRANSFER,
                "First order"
        );
        String idempotencyKey = "pay-key-002";

        mockMvc.perform(post("/api/payments")
                        .header(IdempotencyConstants.HEADER_IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payments")
                        .header(IdempotencyConstants.HEADER_IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(modifiedRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void createPayment_whileRequestInProgress_returnsConflict() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                new BigDecimal("350000.00"),
                "VND",
                PaymentMethod.CREDIT_CARD,
                "Concurrent payment"
        );
        String idempotencyKey = "pay-key-003";
        String redisKey = IdempotencyConstants.REDIS_KEY_PREFIX + "user:" + testUser.getId() + ":" + idempotencyKey;

        // Pre-seed IN_PROGRESS record with matching request hash computed via a real first call?
        // To simulate exact in-flight collision, acquire lock directly via filter's hash formula or test hash-independent/matching state.
        // We compute the exact SHA-256 used by IdempotencyFilter for POST /api/payments.
        String body = objectMapper.writeValueAsString(request);
        String requestHash = sha256("POST|/api/payments||" + body);

        fakeRedisStore.put(redisKey, IdempotencyRecord.builder()
                .status(IdempotencyStatus.IN_PROGRESS)
                .requestHash(requestHash)
                .lockToken("other-worker-token")
                .createdAtEpochMs(System.currentTimeMillis())
                .build());

        mockMvc.perform(post("/api/payments")
                        .header(IdempotencyConstants.HEADER_IDEMPOTENCY_KEY, idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void getMyPayments_notConfiguredAsIdempotent_succeedsWithoutIdempotencyKey() throws Exception {
        mockMvc.perform(get("/api/payments/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));
    }

    private String sha256(String input) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(hash);
    }
}

