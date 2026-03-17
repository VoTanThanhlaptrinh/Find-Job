package com.job_web.security;

import java.io.IOException;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.job_web.service.support.VerificationService;

import lombok.extern.slf4j.Slf4j;
@Component
@Slf4j
@RequiredArgsConstructor
public class VerifyRecoveryFillter extends OncePerRequestFilter {
	private VerificationService verifyService;
	@Override
	protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,@NonNull FilterChain filterChain)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		String token = request.getParameter("token");
		// kiểm tra token tồn tại trong hệ thống hay không
		if (token == null || !verifyService.containsKey(token)) {
			response.sendRedirect("/forgot");
			return;
		}
		filterChain.doFilter(request, response);
	}
	// phương thức này giúp chỉ tính trên url bắt đầu bằng recovery còn lại không liên quan.
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
	    return !request.getServletPath().startsWith("/recovery");
	}
}


