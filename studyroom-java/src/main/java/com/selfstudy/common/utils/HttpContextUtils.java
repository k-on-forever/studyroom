package com.selfstudy.common.utils;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

public final class HttpContextUtils {

	private HttpContextUtils() {
	}

	public static HttpServletRequest getHttpServletRequest() {
		RequestAttributes ra = RequestContextHolder.getRequestAttributes();
		if (ra instanceof ServletRequestAttributes) {
			return ((ServletRequestAttributes) ra).getRequest();
		}
		return null;
	}
}
