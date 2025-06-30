package com.job_web.security;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.job_web.service.IVerifyService;

import lombok.extern.slf4j.Slf4j;
@Component
@Slf4j
public class VerifyRecoveryFillter extends OncePerRequestFilter {
	private IVerifyService verifyService;
	public VerifyRecoveryFillter(IVerifyService verifyService) {
		super();
		this.verifyService = verifyService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		String token = request.getParameter("token");
		// kiểm tra token tồn tại trong hệ thống hay không
		if (token == null || !verifyService.containsKey("recovery:" + token)) {
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
