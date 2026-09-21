package com.nlu.shared.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for extracting and sanitizing client IP addresses.
 * Checks multiple headers for proxy/CDN scenarios (Cloudflare, X-Forwarded-For, X-Real-IP)
 * before falling back to the remote address.
 */
public final class IpUtils {

    public static final String CF_CONNECTING_IP = "CF-Connecting-IP"; // Cloudflare
    public static final String TRUE_CLIENT_IP = "True-Client-IP";     // Cloudflare Enterprise / CDN
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";   // Standard proxy
    public static final String X_REAL_IP = "X-Real-IP";               // Nginx / reverse proxy

    private IpUtils() {
        // Prevent instantiation
    }

    /**
     * Extract client IP from request headers.
     * Priority: Cloudflare (CF-Connecting-IP, True-Client-IP) > X-Forwarded-For > X-Real-IP > Remote Address
     *
     * @param request the HttpServletRequest
     * @return the resolved and sanitized client IP string, or empty string if request is null
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }

        // 1. Cloudflare headers
        String ip = request.getHeader(CF_CONNECTING_IP);
        if (isValidIp(ip)) {
            return sanitizeIp(ip);
        }

        ip = request.getHeader(TRUE_CLIENT_IP);
        if (isValidIp(ip)) {
            return sanitizeIp(ip);
        }

        // 2. Standard proxy headers
        ip = request.getHeader(X_FORWARDED_FOR);
        if (isValidIp(ip)) {
            // X-Forwarded-For can contain multiple IPs, take the first one (original client)
            String[] ips = ip.split(",");
            return sanitizeIp(ips[0].trim());
        }

        ip = request.getHeader(X_REAL_IP);
        if (isValidIp(ip)) {
            return sanitizeIp(ip);
        }

        // 3. Fallback to direct remote address
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? sanitizeIp(remoteAddr) : "";
    }

    /**
     * Validate IP address string.
     *
     * @param ip the IP address candidate
     * @return true if not null, not blank, and not "unknown"
     */
    public static boolean isValidIp(String ip) {
        return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip.trim());
    }

    /**
     * Sanitize IP address to prevent injection or formatting issues.
     * Removes any character that is not valid in IPv4 or IPv6 hex/colons/dots.
     *
     * @param ip the raw IP string
     * @return sanitized IP string
     */
    public static String sanitizeIp(String ip) {
        if (ip == null) {
            return "";
        }
        return ip.replaceAll("[^a-fA-F0-9.:]", "");
    }
}
