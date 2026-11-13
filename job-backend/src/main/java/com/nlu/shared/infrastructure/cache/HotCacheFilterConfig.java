package com.nlu.shared.infrastructure.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares {@link PublicHotGetCacheFilter} and {@link ProtectedHotGetCacheFilter} without
 * auto-registering them onto the outer servlet container chain (they run strictly inside
 * Spring Security's filter chain at explicit positions).
 */
@Configuration
@ConditionalOnBean(HotCacheService.class)
public class HotCacheFilterConfig {

    @Bean
    PublicHotGetCacheFilter publicHotGetCacheFilter(HotGetCacheSupport cacheSupport) {
        return new PublicHotGetCacheFilter(cacheSupport);
    }

    @Bean
    ProtectedHotGetCacheFilter protectedHotGetCacheFilter(
            HotGetCacheSupport cacheSupport,
            HotCacheService hotCacheService) {
        return new ProtectedHotGetCacheFilter(cacheSupport, hotCacheService);
    }

    @Bean
    FilterRegistrationBean<PublicHotGetCacheFilter> publicHotGetCacheFilterRegistration(
            PublicHotGetCacheFilter filter) {
        FilterRegistrationBean<PublicHotGetCacheFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<ProtectedHotGetCacheFilter> protectedHotGetCacheFilterRegistration(
            ProtectedHotGetCacheFilter filter) {
        FilterRegistrationBean<ProtectedHotGetCacheFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}

