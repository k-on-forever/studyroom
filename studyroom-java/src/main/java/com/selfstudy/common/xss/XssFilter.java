package com.selfstudy.common.xss;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 轻量 XSS 过滤：包装请求，对常见参数做 HTML 转义可在此扩展。
 */
public class XssFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (request instanceof HttpServletRequest) {
			chain.doFilter(new XssHttpServletRequestWrapper((HttpServletRequest) request), response);
		} else {
			chain.doFilter(request, response);
		}
	}
}
