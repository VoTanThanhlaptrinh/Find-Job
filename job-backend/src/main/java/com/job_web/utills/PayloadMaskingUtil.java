package com.job_web.utills;

import java.util.Set;
import java.util.regex.Pattern;

public final class PayloadMaskingUtil {

    private PayloadMaskingUtil() {
    }

    private static final String MASK_REPLACEMENT = "[PROTECTED]";

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "oldPassword", "newPassword", "confirmPassword",
            "token", "refreshToken", "accessToken",
            "authorization",
            "email",
            "phone", "phoneNumber"
    );
    private static final Pattern SENSITIVE_PATTERN;

    static {
        String keyAlternation = String.join("|", SENSITIVE_KEYS);
        String regex = "(?i)(\"(?:" + keyAlternation + ")\"\\s*:\\s*)(\"(?:[^\"\\\\]|\\\\.)*\"|[^\\s,}\\]]+)";
        SENSITIVE_PATTERN = Pattern.compile(regex);
    }
    public static String maskSensitiveData(String payload) {
        if (payload == null || payload.isBlank()) {
            return "[empty]";
        }
        return SENSITIVE_PATTERN.matcher(payload).replaceAll("$1\"" + MASK_REPLACEMENT + "\"");
    }
}
