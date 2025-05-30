package com.job_web.security;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.job_web.service.JwtService;

import io.micrometer.core.lang.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
	private final JwtService jwtService;
	private final UserDetailsService userDetailsService;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();
	private final List<String> allowedURL = List.of("/api/account/login"
			, "/api/account/register"
			,"/error"
			,"/api/home/init"
			,"/api/job/detail/**"
	);
	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		if(allowURLDefault(request)) {
			filterChain.doFilter(request, response);
			return;
		}
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		String jwt;
		String userEmail;
		if (authHeader == null || authHeader.trim().isEmpty() || !authHeader.startsWith("Bearer ")) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "missing token");
			return;
		}
		jwt = authHeader.substring(7);
		userEmail = jwtService.extractUsername(jwt);
		if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
			if (jwtService.isTokenValid(jwt, userDetails)) {
				UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				usernamePasswordAuthenticationToken
						.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
			}else {
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "token invalid");
				return;
			}
		}
		filterChain.doFilter(request, response);
	}
	private boolean allowURLDefault(HttpServletRequest request) {
		return allowedURL.stream().anyMatch(allowed -> pathMatcher.match(allowed, request.getServletPath()));
	}
}
