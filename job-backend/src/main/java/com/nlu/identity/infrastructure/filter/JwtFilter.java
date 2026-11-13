package com.nlu.identity.infrastructure.filter;

import java.io.IOException;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.domain.vo.ApiConstants;
import com.nlu.shared.domain.exception.UnauthorizedException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nlu.identity.application.JwtService;
import com.nlu.identity.application.UserDetailsCacheService;

import io.micrometer.core.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

@Component
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final UserDetailsCacheService userDetailsCacheService;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	public JwtFilter(JwtService jwtService, UserDetailsService userDetailsService) {
		this(jwtService, userDetailsService, null);
	}

	@Autowired
	public JwtFilter(
			JwtService jwtService,
			UserDetailsService userDetailsService,
			@Nullable UserDetailsCacheService userDetailsCacheService) {
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.userDetailsCacheService = userDetailsCacheService;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String url = request.getServletPath();
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		if ("/api/blogs/me".equals(url)) {
			return false;
		}
		for (String publicEndpoint : ApiConstants.PUBLIC_ENDPOINTS) {
			if (pathMatcher.match(publicEndpoint, url)) {
				return true;
			}
		}
		if ("GET".equalsIgnoreCase(request.getMethod())) {
			for (String publicGetEndpoint : ApiConstants.PUBLIC_GET_ENDPOINTS) {
				if (pathMatcher.match(publicGetEndpoint, url)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authHeader == null || authHeader.trim().isEmpty() || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		String jwt = authHeader.substring(7);
		long filterStartNs = System.nanoTime();
		long javaExtractMs = 0;
		long dbLoadUserMs = 0;
		long javaValidateMs = 0;
		try {
			long extractStartNs = System.nanoTime();
			String userEmail = jwtService.extractUsername(jwt);
			javaExtractMs = (System.nanoTime() - extractStartNs) / 1_000_000L;
			if (userEmail == null || userEmail.isEmpty()) {
				writeUnauthorizedResponse(response, "Invalid JWT token");
				return;
			}
			long loadUserStartNs = System.nanoTime();
			UserDetails userDetails = userDetailsCacheService != null
					? userDetailsCacheService.loadUserByUsername(userEmail)
					: userDetailsService.loadUserByUsername(userEmail);
			dbLoadUserMs = (System.nanoTime() - loadUserStartNs) / 1_000_000L;

			boolean isAccountActive = !(userDetails instanceof User user) || user.isActive();
			if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked() || !isAccountActive) {
				writeUnauthorizedResponse(response, "Account is locked, disabled, or not activated");
				return;
			}
			long validateStartNs = System.nanoTime();
			if (jwtService.isTokenValid(jwt, userDetails)) {
				UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				usernamePasswordAuthenticationToken
						.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
			}
			javaValidateMs = (System.nanoTime() - validateStartNs) / 1_000_000L;
		} catch (ExpiredJwtException e) {
			log.error(e.getMessage());
			writeUnauthorizedResponse(response, "Expired JWT token");
			return;
		} catch (JwtException e) {
			log.error(e.getMessage());
			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().write("{\"message\":\"Invalid JWT token\"}");
			response.getWriter().flush();
			response.setHeader("Connection", "close");
			return;
		}
		long selfTotalMs = (System.nanoTime() - filterStartNs) / 1_000_000L;
		long downstreamStartNs = System.nanoTime();
		try {
			filterChain.doFilter(request, response);
		} finally {
			long downstreamMs = (System.nanoTime() - downstreamStartNs) / 1_000_000L;
			log.info("[PERF][Filter:Jwt] {} {} | selfTotal={}ms (javaExtractJwt={}ms, dbLoadUser={}ms, javaValidateJwt={}ms) | downstreamChain={}ms",
					request.getMethod(), request.getRequestURI(),
					selfTotalMs, javaExtractMs, dbLoadUserMs, javaValidateMs, downstreamMs);
		}
	}

	private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
		response.setContentType("application/json");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.getWriter().write("{\"message\":\"" + message + "\"}");
		response.getWriter().flush();
		response.setHeader("Connection", "close");
	}
}
