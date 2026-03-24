package com.job_web.utills;

import com.github.f4b6a3.uuid.UuidCreator;

public final class KeyGeneratorUtil {

    private KeyGeneratorUtil() {
    }

    public static String generateKey() {
        return UuidCreator.getTimeOrderedEpoch().toString();
    }
}
