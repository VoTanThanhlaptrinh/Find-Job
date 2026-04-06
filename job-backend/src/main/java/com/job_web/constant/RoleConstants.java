package com.job_web.constant;

import java.util.Set;

/**
 * Constants for user roles.
 */
public final class RoleConstants {

    private RoleConstants() {
        // Prevent instantiation
    }

    /**
     * Role for regular users (job seekers).
     */
    public static final String USER = "USER";

    /**
     * Role for hirers/recruiters.
     */
    public static final String HIRER = "HIRER";

    /**
     * Role prefix used by Spring Security.
     */
    public static final String ROLE_PREFIX = "ROLE_";

    /**
     * Full role name for users with prefix.
     */
    public static final String ROLE_USER = ROLE_PREFIX + USER;

    /**
     * Full role name for hirers with prefix.
     */
    public static final String ROLE_HIRER = ROLE_PREFIX + HIRER;

    /**
     * Set of valid roles that can be assigned during registration.
     */
    public static final Set<String> VALID_ROLES = Set.of(USER, HIRER, ROLE_USER, ROLE_HIRER);

    /**
     * Check if a role is valid.
     */
    public static boolean isValidRole(String role) {
        return role != null && VALID_ROLES.contains(role.toUpperCase());
    }

    /**
     * Normalize role to standard format (with ROLE_ prefix).
     */
    public static String normalizeRole(String role) {
        if (role == null) {
            return ROLE_USER;
        }
        String upperRole = role.toUpperCase();
        if (upperRole.equals(USER)) {
            return ROLE_USER;
        }
        if (upperRole.equals(HIRER)) {
            return ROLE_HIRER;
        }
        if (upperRole.startsWith(ROLE_PREFIX)) {
            return upperRole;
        }
        return ROLE_PREFIX + upperRole;
    }
}
