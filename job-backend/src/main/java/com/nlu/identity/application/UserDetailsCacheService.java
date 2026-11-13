package com.nlu.identity.application;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Redis-backed read-through cache for {@link UserDetails} during JWT filter validation.
 */
public interface UserDetailsCacheService {

    UserDetails loadUserByUsername(String email);

    void evictByEmail(String email);
}

