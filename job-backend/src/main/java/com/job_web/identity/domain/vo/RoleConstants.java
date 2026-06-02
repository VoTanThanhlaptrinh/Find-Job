package com.job_web.identity.domain.vo;

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
     * Role for administrators.
     */
    public static final String ADMIN = "ADMIN";

    /**
     * Full role name for users with prefix.
     */
    public static final String ROLE_USER = ROLE_PREFIX + USER;

    /**
     * Full role name for hirers with prefix.
     */
    public static final String ROLE_HIRER = ROLE_PREFIX + HIRER;

    /**
     * Full role name for admins with prefix.
     */
    public static final String ROLE_ADMIN = ROLE_PREFIX + ADMIN;

    /**
     * Set of valid roles that can be assigned during registration.
     */
    public static final Set<String> VALID_ROLES = Set.of(USER, HIRER, ADMIN, ROLE_USER, ROLE_HIRER, ROLE_ADMIN);

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
        if (upperRole.equals(ADMIN)) {
            return ROLE_ADMIN;
        }
        if (upperRole.startsWith(ROLE_PREFIX)) {
            return upperRole;
        }
        return ROLE_PREFIX + upperRole;
    }
}
