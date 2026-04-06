package com.job_web.service.security;

/**
 * Service interface for rate limiting functionality.
 */
public interface RateLimitService {

    /**
     * Check if the client is allowed to make a request.
     * 
     * @param clientId The unique identifier for the client (IP address)
     * @param limit The maximum number of requests allowed in the time window
     * @return true if the request is allowed, false if rate limit exceeded
     */
    boolean isAllowed(String clientId, int limit);

    /**
     * Check if the client is currently blocked (in penalty period).
     * 
     * @param clientId The unique identifier for the client
     * @return true if the client is blocked, false otherwise
     */
    boolean isBlocked(String clientId);

    /**
     * Block a client for the penalty duration.
     * 
     * @param clientId The unique identifier for the client
     */
    void blockClient(String clientId);

    /**
     * Get remaining requests for a client in the current time window.
     * 
     * @param clientId The unique identifier for the client
     * @param limit The maximum number of requests allowed
     * @return The number of remaining requests
     */
    int getRemainingRequests(String clientId, int limit);

    /**
     * Get the time until the block expires (in seconds).
     * 
     * @param clientId The unique identifier for the client
     * @return Seconds until block expires, 0 if not blocked
     */
    long getBlockTimeRemaining(String clientId);

    /**
     * Reset the rate limit counter for a client.
     * 
     * @param clientId The unique identifier for the client
     */
    void resetLimit(String clientId);
}
