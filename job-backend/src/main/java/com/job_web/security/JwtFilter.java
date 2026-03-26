package com.job_web.security;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.job_web.constant.ApiConstants;
import com.job_web.service.security.RefreshTokenService;
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

import com.job_web.service.security.JwtService;

import io.micrometer.core.lang.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private AntPathMatcher pathMatcher;
	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
		// TODO Auto-generated method stub
		String url = request.getServletPath();
		for (String allowedOrigin : ApiConstants.PUBLIC_ENDPOINTS) {
			if (pathMatcher.match(allowedOrigin, url)) {
				filterChain.doFilter(request, response);
				return;
			}
		}
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		String jwt;
		String userEmail;
        UserDetails userDetails;
		if (authHeader == null || authHeader.trim().isEmpty() || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		jwt = authHeader.substring(7);
		try {
			userEmail = jwtService.extractUsername(jwt);
			if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
				 userDetails = userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                }
			}
		}catch (ExpiredJwtException e) {
            log.error(e.getMessage());
			response.setContentType("application/json");
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.getWriter().write("{\"message\":\"Expired JWT token\"}");
			response.getWriter().flush();
			response.setHeader("Connection", "close");
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
		filterChain.doFilter(request, response);
	}

	@Override
	protected void initFilterBean() throws ServletException {
		pathMatcher = new AntPathMatcher();
	}
}


