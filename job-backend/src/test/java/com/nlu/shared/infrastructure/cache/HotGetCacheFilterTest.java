package com.nlu.shared.infrastructure.cache;

import com.nlu.identity.domain.model.User;
import com.nlu.identity.domain.vo.EmailAddress;
import com.nlu.identity.domain.vo.PhoneNumber;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HotGetCacheFilterTest {

    private Map<String, byte[]> byteStore;
    private Map<String, AtomicLong> counterStore;
    private Map<String, String> lockStore;
    private Map<String, Map<String, Double>> zsetStore;
    private List<HotCacheInvalidatedEvent> publishedEvents;

    private RedisTemplate<String, byte[]> cacheByteRedisTemplate;
    private StringRedisTemplate stringRedisTemplate;
    private HotCacheProperties properties;
    private HotCachePayloadCodec payloadCodec;
    private HotCacheKeyBuilder keyBuilder;
    private HotCacheService hotCacheService;
    private HotGetCacheSupport cacheSupport;
    private PublicHotGetCacheFilter publicFilter;
    private ProtectedHotGetCacheFilter protectedFilter;

    private StubController stubController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        byteStore = new ConcurrentHashMap<>();
        counterStore = new ConcurrentHashMap<>();
        lockStore = new ConcurrentHashMap<>();
        zsetStore = new ConcurrentHashMap<>();
        publishedEvents = new ArrayList<>();

        cacheByteRedisTemplate = mockByteRedisTemplate();
        stringRedisTemplate = mockStringRedisTemplate();

        properties = buildDefaultProperties();
        payloadCodec = new HotCachePayloadCodec();
        keyBuilder = new HotCacheKeyBuilder(properties);
        ApplicationEventPublisher eventPublisher = event -> {
            if (event instanceof HotCacheInvalidatedEvent invalidatedEvent) {
                publishedEvents.add(invalidatedEvent);
            }
        };
        hotCacheService = new HotCacheService(
                cacheByteRedisTemplate,
                stringRedisTemplate,
                properties,
                payloadCodec,
                keyBuilder,
                eventPublisher
        );
        cacheSupport = new HotGetCacheSupport(properties, keyBuilder, hotCacheService);
        publicFilter = new PublicHotGetCacheFilter(cacheSupport);
        protectedFilter = new ProtectedHotGetCacheFilter(cacheSupport, hotCacheService);

        stubController = new StubController();
        mockMvc = MockMvcBuilders.standaloneSetup(stubController)
                .addFilters(publicFilter, new StubSecurityEnforcementFilter(), protectedFilter)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void publicEndpoint_reachesThresholdFive_thenServesCacheHitAndNormalizesQuery() throws Exception {
        for (int i = 1; i <= 4; i++) {
            mockMvc.perform(get("/api/jobs/search")
                            .param("keyword", "java")
                            .param("page", "0")
                            .param("utm_source", "fb_" + i))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_COUNTING));
        }
        assertEquals(4, stubController.jobSearchCalls.get());

        mockMvc.perform(get("/api/jobs/search")
                        .param("page", "0")
                        .param("keyword", "java")
                        .param("_ts", "99999"))
                .andExpect(status().isOk())
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_MISS_STORED));
        assertEquals(5, stubController.jobSearchCalls.get());

        // 6th request with swapped param order and another tracking query param -> must HIT
        MvcResult hitResult = mockMvc.perform(get("/api/jobs/search")
                        .param("keyword", "java")
                        .param("page", "0")
                        .param("fbclid", "ignored-123"))
                .andExpect(status().isOk())
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_HIT))
                .andReturn();

        assertEquals(5, stubController.jobSearchCalls.get());
        assertTrue(hitResult.getResponse().getContentAsString().contains("search-call-5"));
    }

    @Test
    void localeSeparation_keepsVietnameseAndEnglishEntriesDistinct() throws Exception {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/jobs/search").param("keyword", "spring"))
                    .andExpect(status().isOk());
        }
        MvcResult viHit = mockMvc.perform(get("/api/jobs/search").param("keyword", "spring"))
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_HIT))
                .andReturn();
        assertTrue(viHit.getResponse().getContentAsString().contains("lang=vi"));

        LocaleContextHolder.setLocale(Locale.ENGLISH);
        MvcResult enMiss = mockMvc.perform(get("/api/jobs/search").param("keyword", "spring"))
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_COUNTING))
                .andReturn();
        assertTrue(enMiss.getResponse().getContentAsString().contains("lang=en"));
    }

    @Test
    void paginationBoundsAndBlacklist_alwaysBypassCache() throws Exception {
        for (int i = 0; i < 7; i++) {
            mockMvc.perform(get("/api/jobs/search").param("page", "11"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_BYPASS));
        }
        assertEquals(7, stubController.jobSearchCalls.get());

        for (int i = 0; i < 7; i++) {
            mockMvc.perform(get("/api/admin/employers/export"))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist(HotCacheConstants.HEADER_CACHE_STATUS));
        }
        assertEquals(7, stubController.exportCalls.get());
    }

    @Test
    void payloadCodec_compressesBodiesAboveGzipThreshold_andFilterBypassesOversizedBodies() throws Exception {
        byte[] largeJson = ("{\"data\":\"" + "A".repeat(4096) + "\"}").getBytes(StandardCharsets.UTF_8);
        CachedHttpResponse original = new CachedHttpResponse(200, MediaType.APPLICATION_JSON_VALUE, largeJson);

        byte[] encoded = payloadCodec.encode(original, properties.getGzipThresholdBytes());
        assertEquals((byte) 1, encoded[4]);
        assertTrue(encoded.length < largeJson.length);

        CachedHttpResponse decoded = payloadCodec.decode(encoded);
        assertEquals(200, decoded.httpStatus());
        assertArrayEquals(largeJson, decoded.body());

        // Endpoint returning > 2KB (< 64KB) caches with gzip transparently
        stubController.payloadSize = 3500;
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/jobs/1"))
                    .andExpect(status().isOk());
        }
        MvcResult hitLarge = mockMvc.perform(get("/api/jobs/1"))
                .andExpect(status().isOk())
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_HIT))
                .andReturn();
        assertTrue(hitLarge.getResponse().getContentAsString().length() >= 3500);

        // Endpoint returning > maxBodyBytes (8192 for test override) bypasses cache
        properties.setMaxBodyBytes(2000);
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(get("/api/jobs/2"))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_BYPASS));
        }
    }

    @Test
    void zsetQuotaEviction_evictsOldestEntryWhenExceedingMaxEntries() throws Exception {
        // rule job_detail has maxEntries = 2
        for (long jobId = 10; jobId <= 12; jobId++) {
            for (int call = 0; call < 5; call++) {
                mockMvc.perform(get("/api/jobs/" + jobId)).andExpect(status().isOk());
            }
            Thread.sleep(5);
        }

        String indexKey = HotCacheConstants.KEY_PREFIX_INDEX + "job_detail";
        assertEquals(2, zsetStore.getOrDefault(indexKey, Map.of()).size());

        // Oldest jobId=10 must have been evicted -> next request counts from 6 (> threshold 5) and re-hits controller
        int callsBefore = stubController.jobDetailCalls.get();
        mockMvc.perform(get("/api/jobs/10"))
                .andExpect(status().isOk())
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_MISS_STORED));
        assertEquals(callsBefore + 1, stubController.jobDetailCalls.get());
    }

    @Test
    void securityAndUserScopeIsolation_preventsCrossUserAndUnauthorizedCacheLeaks() throws Exception {
        // 1. Unauthenticated request to protected admin endpoint -> 401, no cache
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HotCacheConstants.HEADER_CACHE_STATUS));

        // 2. ADMIN populates admin_dashboard_summary
        Authentication adminAuth = buildAuth(1L, "admin@test.com", "ROLE_ADMIN");
        for (int i = 0; i < 5; i++) {
            SecurityContextHolder.getContext().setAuthentication(adminAuth);
            mockMvc.perform(get("/api/admin/dashboard/summary")).andExpect(status().isOk());
        }
        SecurityContextHolder.getContext().setAuthentication(adminAuth);
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_HIT));

        // 3. Regular USER calling admin endpoint must still get 403 Forbidden (never ADMIN's cached 200)
        Authentication user101 = buildAuth(101L, "u101@test.com", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(user101);
        mockMvc.perform(get("/api/admin/dashboard/summary"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HotCacheConstants.HEADER_CACHE_STATUS));

        // 4. USER 101 populates USER-scoped /api/payments/10; USER 202 must NOT hit USER 101's entry
        for (int i = 0; i < 5; i++) {
            SecurityContextHolder.getContext().setAuthentication(user101);
            mockMvc.perform(get("/api/payments/10")).andExpect(status().isOk());
        }
        SecurityContextHolder.getContext().setAuthentication(user101);
        MvcResult u101Hit = mockMvc.perform(get("/api/payments/10"))
                .andExpect(status().isOk())
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_HIT))
                .andReturn();
        assertTrue(u101Hit.getResponse().getContentAsString().contains("owner=101"));

        Authentication user202 = buildAuth(202L, "u202@test.com", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(user202);
        MvcResult u202Miss = mockMvc.perform(get("/api/payments/10"))
                .andExpect(status().isOk())
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_COUNTING))
                .andReturn();
        assertTrue(u202Miss.getResponse().getContentAsString().contains("owner=202"));
    }

    @Test
    void writeAutoInvalidation_evictsTargetedResourceAndAllScopedRules() throws Exception {
        // Populate /api/jobs/42 and /api/jobs/99
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/api/jobs/42")).andExpect(status().isOk());
            mockMvc.perform(get("/api/jobs/99")).andExpect(status().isOk());
        }

        // Write PUT /api/hirer/jobs/42 as HIRER -> must invalidate /api/jobs/42 only, keep /api/jobs/99
        Authentication hirerAuth = buildAuth(50L, "hirer@test.com", "ROLE_HIRER");
        SecurityContextHolder.getContext().setAuthentication(hirerAuth);
        mockMvc.perform(put("/api/hirer/jobs/42"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/jobs/99"))
                .andExpect(status().isOk())
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_HIT));

        int callsBefore = stubController.jobDetailCalls.get();
        mockMvc.perform(get("/api/jobs/42"))
                .andExpect(status().isOk())
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_MISS_STORED));
        assertEquals(callsBefore + 1, stubController.jobDetailCalls.get());

        // Populate hirer_jobs_posted as hirerAuth (USER scope)
        for (int i = 0; i < 5; i++) {
            SecurityContextHolder.getContext().setAuthentication(hirerAuth);
            mockMvc.perform(get("/api/hirer/jobs/posted")).andExpect(status().isOk());
        }
        SecurityContextHolder.getContext().setAuthentication(hirerAuth);
        mockMvc.perform(get("/api/hirer/jobs/posted"))
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_HIT));

        // Candidate submits application -> [all] trigger must invalidate hirer_jobs_posted across users
        Authentication candidateAuth = buildAuth(303L, "cand@test.com", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(candidateAuth);
        mockMvc.perform(post("/api/user/applications/submit-existing"))
                .andExpect(status().isOk());

        SecurityContextHolder.getContext().setAuthentication(hirerAuth);
        mockMvc.perform(get("/api/hirer/jobs/posted"))
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_MISS_STORED));
        assertTrue(publishedEvents.stream().anyMatch(e -> "hirer_jobs_posted".equals(e.ruleId())));
    }

    @Test
    void singleFlightLock_waitsAndReplaysPopulatedCacheWithoutHittingController() throws Exception {
        // Warm counter to 4 so the next request reaches threshold 5
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(get("/api/jobs/search").param("keyword", "stampede"))
                    .andExpect(status().isOk());
        }
        int callsAfterWarmup = stubController.jobSearchCalls.get();

        // Simulate another request currently holding the single-flight lock for this key
        org.springframework.mock.web.MockHttpServletRequest probeRequest =
                new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/jobs/search");
        probeRequest.setParameter("keyword", "stampede");
        HotCacheProperties.Rule rule = keyBuilder.findMatchingRule(probeRequest).orElseThrow();
        HotCacheKeyContext ctx = keyBuilder.buildKeyContext(probeRequest, rule, null).orElseThrow();
        lockStore.put(ctx.lockKey(), "1");

        // On retry #2 of waitForSingleFlightCache, simulate leader request completing and writing Redis entry
        AtomicInteger readAttempt = new AtomicInteger(0);
        when(cacheByteRedisTemplate.opsForValue().get(ctx.entryKey())).thenAnswer(inv -> {
            int attempt = readAttempt.incrementAndGet();
            if (attempt < 3) {
                return byteStore.get(ctx.entryKey());
            }
            CachedHttpResponse leaderResp = new CachedHttpResponse(
                    200,
                    MediaType.APPLICATION_JSON_VALUE,
                    "{\"leader\":\"cached\"}".getBytes(StandardCharsets.UTF_8));
            byte[] encoded = payloadCodec.encode(leaderResp, properties.getGzipThresholdBytes());
            byteStore.put(ctx.entryKey(), encoded);
            return encoded;
        });

        MvcResult result = mockMvc.perform(get("/api/jobs/search").param("keyword", "stampede"))
                .andExpect(status().isOk())
                .andExpect(header().string(HotCacheConstants.HEADER_CACHE_STATUS, HotCacheConstants.CACHE_HIT))
                .andReturn();

        assertEquals(callsAfterWarmup, stubController.jobSearchCalls.get());
        assertTrue(result.getResponse().getContentAsString().contains("leader"));
    }

    private HotCacheProperties buildDefaultProperties() {
        HotCacheProperties props = new HotCacheProperties();
        props.setEnabled(true);
        props.setDefaultThreshold(5);
        props.setDefaultWindowSeconds(60);
        props.setMaxBodyBytes(65536);
        props.setGzipThresholdBytes(2048);
        props.setLockTtlSeconds(5);
        props.setSingleFlightWaitMs(10);
        props.setSingleFlightMaxWaitRetries(3);
        props.setMaxPage(10);
        props.setMaxSize(50);
        props.setExcludedPatterns(List.of("GET /api/admin/employers/export"));

        HotCacheProperties.Rule jobSearch = new HotCacheProperties.Rule();
        jobSearch.setId("jobs_search");
        jobSearch.setPattern("GET /api/jobs/search");
        jobSearch.setScope(HotCacheScope.PUBLIC);
        jobSearch.setTtlSeconds(120);
        jobSearch.setMaxEntries(10);

        HotCacheProperties.Rule jobDetail = new HotCacheProperties.Rule();
        jobDetail.setId("job_detail");
        jobDetail.setPattern("GET /api/jobs/{id}");
        jobDetail.setScope(HotCacheScope.PUBLIC);
        jobDetail.setTtlSeconds(300);
        jobDetail.setMaxEntries(2);
        jobDetail.setInvalidateOn(List.of("PUT /api/hirer/jobs/{id}"));

        HotCacheProperties.Rule adminSummary = new HotCacheProperties.Rule();
        adminSummary.setId("admin_dashboard_summary");
        adminSummary.setPattern("GET /api/admin/dashboard/summary");
        adminSummary.setScope(HotCacheScope.ROLE);
        adminSummary.setTtlSeconds(180);
        adminSummary.setMaxEntries(5);

        HotCacheProperties.Rule paymentDetail = new HotCacheProperties.Rule();
        paymentDetail.setId("payment_detail");
        paymentDetail.setPattern("GET /api/payments/{id}");
        paymentDetail.setScope(HotCacheScope.USER);
        paymentDetail.setTtlSeconds(600);
        paymentDetail.setMaxEntries(10);

        HotCacheProperties.Rule hirerPosted = new HotCacheProperties.Rule();
        hirerPosted.setId("hirer_jobs_posted");
        hirerPosted.setPattern("GET /api/hirer/jobs/posted");
        hirerPosted.setScope(HotCacheScope.USER);
        hirerPosted.setTtlSeconds(120);
        hirerPosted.setMaxEntries(10);
        hirerPosted.setInvalidateOn(List.of("POST /api/user/applications/** [all]"));

        props.setRules(List.of(jobSearch, jobDetail, adminSummary, paymentDetail, hirerPosted));
        return props;
    }

    private Authentication buildAuth(long id, String email, String role) {
        User user = new User(
                id,
                "User " + id,
                null,
                LocalDate.of(1998, 1, 1),
                role,
                new EmailAddress(email),
                "HCM",
                new PhoneNumber("0912345678"),
                false,
                true,
                true,
                false,
                List.of(),
                List.of()
        );
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    @SuppressWarnings("unchecked")
    private RedisTemplate<String, byte[]> mockByteRedisTemplate() {
        RedisTemplate<String, byte[]> template = mock(RedisTemplate.class);
        ValueOperations<String, byte[]> valueOps = mock(ValueOperations.class);
        lenient().when(template.opsForValue()).thenReturn(valueOps);
        lenient().when(valueOps.get(anyString())).thenAnswer(inv -> byteStore.get(inv.<String>getArgument(0)));
        lenient().doAnswer(inv -> {
            String key = inv.getArgument(0);
            byte[] val = inv.getArgument(1);
            byteStore.put(key, val);
            return null;
        }).when(valueOps).set(anyString(), any(byte[].class), any(Duration.class));
        lenient().when(template.delete(anyCollection())).thenAnswer(inv -> {
            Collection<String> keys = inv.getArgument(0);
            long count = 0;
            for (String key : keys) {
                if (byteStore.remove(key) != null) {
                    count++;
                }
            }
            return count;
        });
        lenient().when(template.delete(anyString())).thenAnswer(inv -> byteStore.remove(inv.<String>getArgument(0)) != null);
        return template;
    }

    @SuppressWarnings("unchecked")
    private StringRedisTemplate mockStringRedisTemplate() {
        StringRedisTemplate template = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        ZSetOperations<String, String> zsetOps = mock(ZSetOperations.class);

        lenient().when(template.opsForValue()).thenReturn(valueOps);
        lenient().when(template.opsForZSet()).thenReturn(zsetOps);
        lenient().when(template.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        lenient().when(template.delete(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            boolean removed = lockStore.remove(key) != null;
            removed |= zsetStore.remove(key) != null;
            removed |= counterStore.remove(key) != null;
            return removed;
        });

        lenient().when(valueOps.increment(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            return counterStore.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        });
        lenient().when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            String val = inv.getArgument(1);
            return lockStore.putIfAbsent(key, val) == null;
        });

        lenient().when(zsetOps.removeRangeByScore(anyString(), anyDouble(), anyDouble())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            double min = inv.getArgument(1);
            double max = inv.getArgument(2);
            Map<String, Double> entries = zsetStore.get(key);
            if (entries == null) {
                return 0L;
            }
            List<String> expired = entries.entrySet().stream()
                    .filter(e -> e.getValue() >= min && e.getValue() <= max)
                    .map(Map.Entry::getKey)
                    .toList();
            expired.forEach(entries::remove);
            return (long) expired.size();
        });
        lenient().when(zsetOps.add(anyString(), anyString(), anyDouble())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            String member = inv.getArgument(1);
            double score = inv.getArgument(2);
            Map<String, Double> entries = zsetStore.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
            boolean isNew = !entries.containsKey(member);
            entries.put(member, score);
            return isNew;
        });
        lenient().when(zsetOps.zCard(anyString())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            return (long) zsetStore.getOrDefault(key, Map.of()).size();
        });
        lenient().when(zsetOps.popMin(anyString(), anyLong())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            long count = inv.getArgument(1);
            Map<String, Double> entries = zsetStore.get(key);
            if (entries == null || entries.isEmpty()) {
                return Set.of();
            }
            List<Map.Entry<String, Double>> sorted = entries.entrySet().stream()
                    .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                    .limit(count)
                    .toList();
            Set<ZSetOperations.TypedTuple<String>> popped = new LinkedHashSet<>();
            for (Map.Entry<String, Double> entry : sorted) {
                entries.remove(entry.getKey());
                popped.add(new DefaultTypedTuple<>(entry.getKey(), entry.getValue()));
            }
            return popped;
        });
        lenient().when(zsetOps.range(anyString(), anyLong(), anyLong())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            Map<String, Double> entries = zsetStore.get(key);
            return entries == null ? Set.of() : new LinkedHashSet<>(entries.keySet());
        });

        return template;
    }

    /**
     * Simulates SecurityConfig's JwtFilter + AuthorizationFilter positioned between
     * PublicHotGetCacheFilter and ProtectedHotGetCacheFilter.
     */
    private static class StubSecurityEnforcementFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {

            String uri = request.getRequestURI();
            boolean isProtected = uri.startsWith("/api/admin/")
                    || uri.startsWith("/api/payments/")
                    || uri.startsWith("/api/hirer/")
                    || uri.startsWith("/api/user/");

            if (!isProtected) {
                filterChain.doFilter(request, response);
                return;
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }

            if (uri.startsWith("/api/admin/") && !hasRole(auth, "ROLE_ADMIN")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

            filterChain.doFilter(request, response);
        }

        private boolean hasRole(Authentication auth, String role) {
            return auth.getAuthorities().stream().anyMatch(a -> role.equals(a.getAuthority()));
        }
    }

    @RestController
    static class StubController {
        final AtomicInteger jobSearchCalls = new AtomicInteger();
        final AtomicInteger jobDetailCalls = new AtomicInteger();
        final AtomicInteger exportCalls = new AtomicInteger();
        volatile int payloadSize = 0;

        @GetMapping(value = "/api/jobs/search", produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, Object> searchJobs(@RequestParam(required = false) String keyword) {
            int n = jobSearchCalls.incrementAndGet();
            String lang = LocaleContextHolder.getLocale().getLanguage();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("marker", "search-call-" + n);
            body.put("keyword", keyword);
            body.put("lang", lang);
            return body;
        }

        @GetMapping(value = "/api/jobs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, Object> getJob(@PathVariable long id) {
            int n = jobDetailCalls.incrementAndGet();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("id", id);
            body.put("call", n);
            if (payloadSize > 0) {
                body.put("padding", "X".repeat(payloadSize));
            }
            return body;
        }

        @GetMapping(value = "/api/admin/employers/export", produces = MediaType.TEXT_PLAIN_VALUE)
        public String exportEmployers() {
            return "export-" + exportCalls.incrementAndGet();
        }

        @GetMapping(value = "/api/admin/dashboard/summary", produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, Object> adminSummary() {
            return Map.of("totalUsers", 100);
        }

        @GetMapping(value = "/api/payments/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, Object> paymentDetail(@PathVariable long id) {
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return Map.of("paymentId", id, "owner", "owner=" + user.getId());
        }

        @GetMapping(value = "/api/hirer/jobs/posted", produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, Object> hirerPosted() {
            return Map.of("count", 5);
        }

        @PutMapping(value = "/api/hirer/jobs/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, Object> updateJob(@PathVariable long id) {
            return Map.of("updated", id);
        }

        @PostMapping(value = "/api/user/applications/submit-existing", produces = MediaType.APPLICATION_JSON_VALUE)
        public Map<String, Object> submitExisting() {
            return Map.of("submitted", true);
        }
    }
}
