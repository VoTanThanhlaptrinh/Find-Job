package com.nlu.shared.infrastructure.cache;

import com.nlu.identity.domain.model.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Matches incoming HTTP requests against {@link HotCacheProperties} rules and builds
 * normalized, scope-isolated Redis keys.
 */
@Component
@RequiredArgsConstructor
public class HotCacheKeyBuilder {

    private final HotCacheProperties properties;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public Optional<HotCacheProperties.Rule> findMatchingRule(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return Optional.empty();
        }
        String uri = request.getRequestURI();
        if (isExcluded("GET", uri)) {
            return Optional.empty();
        }
        for (HotCacheProperties.Rule rule : properties.getRules()) {
            if (matchesRulePattern(rule.getPattern(), "GET", uri)) {
                return Optional.of(rule);
            }
        }
        return Optional.empty();
    }

    public Optional<HotCacheKeyContext> buildKeyContext(
            HttpServletRequest request,
            HotCacheProperties.Rule rule,
            Authentication authentication) {

        Optional<String> scopeOpt = resolveScopeSegment(rule.getScope(), authentication);
        if (scopeOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> normalizedQueryOpt = normalizeQueryParams(request.getParameterMap());
        if (normalizedQueryOpt.isEmpty()) {
            return Optional.empty();
        }

        String uri = request.getRequestURI();
        String locale = resolveLocaleCode();
        String resourceTag = extractResourceTag(rule.getPattern(), uri);
        String queryHash = sha256Short(uri + "?" + normalizedQueryOpt.get());

        String suffix = rule.getId()
                + ":" + scopeOpt.get()
                + ":" + locale
                + resourceTag
                + ":" + queryHash;

        return Optional.of(new HotCacheKeyContext(
                rule,
                scopeOpt.get(),
                HotCacheConstants.KEY_PREFIX_ENTRY + suffix,
                HotCacheConstants.KEY_PREFIX_COUNTER + suffix,
                HotCacheConstants.KEY_PREFIX_LOCK + suffix,
                HotCacheConstants.KEY_PREFIX_INDEX + rule.getId()
        ));
    }

    public boolean matchesPattern(String rawPattern, String method, String uri) {
        return matchesRulePattern(rawPattern, method, uri);
    }

    public Map<String, String> extractTemplateVariables(String rawPattern, String uri) {
        String pathPattern = extractPathPattern(rawPattern);
        if (pathPattern == null || !pathMatcher.match(pathPattern, uri)) {
            return Map.of();
        }
        return pathMatcher.extractUriTemplateVariables(pathPattern, uri);
    }

    public Optional<String> resolveUserScope(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if ("anonymousUser".equals(principal)) {
            return Optional.empty();
        }
        if (principal instanceof User user) {
            return Optional.of("user_" + user.getId());
        }
        return Optional.of("user_" + authentication.getName());
    }

    private boolean isExcluded(String method, String uri) {
        List<String> excluded = properties.getExcludedPatterns();
        if (excluded == null || excluded.isEmpty()) {
            return false;
        }
        for (String pattern : excluded) {
            if (matchesRulePattern(pattern, method, uri)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesRulePattern(String rawPattern, String method, String uri) {
        if (rawPattern == null || rawPattern.isBlank() || uri == null) {
            return false;
        }
        String cleaned = stripRuleOptions(rawPattern);
        int spaceIdx = cleaned.indexOf(' ');
        if (spaceIdx <= 0) {
            return pathMatcher.match(cleaned, uri);
        }
        String configuredMethod = cleaned.substring(0, spaceIdx).trim().toUpperCase();
        String pathPattern = cleaned.substring(spaceIdx + 1).trim();
        boolean methodMatches = "*".equals(configuredMethod) || configuredMethod.equalsIgnoreCase(method);
        return methodMatches && pathMatcher.match(pathPattern, uri);
    }

    private String extractPathPattern(String rawPattern) {
        if (rawPattern == null || rawPattern.isBlank()) {
            return null;
        }
        String cleaned = stripRuleOptions(rawPattern);
        int spaceIdx = cleaned.indexOf(' ');
        return spaceIdx > 0 ? cleaned.substring(spaceIdx + 1).trim() : cleaned;
    }

    private String stripRuleOptions(String rawPattern) {
        String trimmed = rawPattern.trim();
        int optionIdx = trimmed.indexOf(" [");
        return optionIdx > 0 ? trimmed.substring(0, optionIdx).trim() : trimmed;
    }

    private Optional<String> resolveScopeSegment(HotCacheScope scope, Authentication authentication) {
        if (scope == HotCacheScope.PUBLIC) {
            return Optional.of("public");
        }
        if (scope == HotCacheScope.USER) {
            return resolveUserScope(authentication);
        }
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if ("anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .collect(Collectors.joining(","));
        if (roles.isBlank()) {
            return Optional.empty();
        }
        return Optional.of("role_" + roles);
    }

    private Optional<String> normalizeQueryParams(Map<String, String[]> parameterMap) {
        if (parameterMap == null || parameterMap.isEmpty()) {
            return Optional.of("");
        }
        TreeMap<String, String[]> sorted = new TreeMap<>(parameterMap);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> entry : sorted.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey().trim() : "";
            if (key.isEmpty() || HotCacheConstants.IGNORED_QUERY_PARAMS.contains(key.toLowerCase())) {
                continue;
            }
            if (!isWithinPaginationBounds(key, entry.getValue())) {
                return Optional.empty();
            }
            String[] values = entry.getValue() != null ? entry.getValue().clone() : new String[0];
            Arrays.sort(values);
            if (!sb.isEmpty()) {
                sb.append('&');
            }
            sb.append(key).append('=').append(String.join(",", values).trim());
        }
        return Optional.of(sb.toString());
    }

    private boolean isWithinPaginationBounds(String paramName, String[] values) {
        if (values == null || values.length == 0 || values[0] == null || values[0].isBlank()) {
            return true;
        }
        boolean isPageParam = "page".equalsIgnoreCase(paramName) || "pageIndex".equalsIgnoreCase(paramName);
        boolean isSizeParam = "size".equalsIgnoreCase(paramName) || "pageSize".equalsIgnoreCase(paramName);
        if (!isPageParam && !isSizeParam) {
            return true;
        }
        try {
            int num = Integer.parseInt(values[0].trim());
            if (isPageParam) {
                return num >= 0 && num <= properties.getMaxPage();
            }
            return num > 0 && num <= properties.getMaxSize();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String resolveLocaleCode() {
        String lang = LocaleContextHolder.getLocale().getLanguage();
        return (lang == null || lang.isBlank()) ? "vi" : lang.toLowerCase();
    }

    private String extractResourceTag(String rawPattern, String uri) {
        Map<String, String> vars = extractTemplateVariables(rawPattern, uri);
        if (vars.isEmpty()) {
            return "";
        }
        TreeMap<String, String> sorted = new TreeMap<>(vars);
        String joined = sorted.entrySet().stream()
                .map(e -> "[" + e.getKey() + "=" + e.getValue() + "]")
                .collect(Collectors.joining());
        return ":r" + joined;
    }

    private String sha256Short(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fullHash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(fullHash, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
