package com.selfstudy.modules.sys.interceptor;

import com.selfstudy.common.exception.RRException;
import com.selfstudy.modules.sys.dao.SysAdminDao;
import com.selfstudy.modules.sys.entity.SysAdminEntity;
import com.selfstudy.modules.sys.entity.SysAdminTokenEntity;
import com.selfstudy.modules.sys.service.SysAdminTokenService;
import com.selfstudy.modules.sys.support.AdminJwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理后台 /sys/**（除登录等白名单）Token 校验。
 */
@Component
@RequiredArgsConstructor
public class SysAdminAuthInterceptor implements HandlerInterceptor {

	public static final String ADMIN_ID_ATTR = "adminId";
	/** 当前管理员登录名（用于审计日志等） */
	public static final String ADMIN_USERNAME_ATTR = "adminUsername";

	private final AdminJwtUtils adminJwtUtils;
	private final SysAdminTokenService sysAdminTokenService;
	private final SysAdminDao sysAdminDao;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod)) {
			return true;
		}

		String token = request.getHeader(adminJwtUtils.getHeader());
		if (StringUtils.isBlank(token)) {
			token = request.getParameter(adminJwtUtils.getHeader());
		}
		if (StringUtils.isBlank(token)) {
			throw new RRException("请登录", HttpStatus.UNAUTHORIZED.value());
		}

		SysAdminTokenEntity row = sysAdminTokenService.queryByToken(token);
		if (row == null || !token.equals(row.getToken())) {
			throw new RRException(adminJwtUtils.getHeader() + "失效，请重新登录", HttpStatus.UNAUTHORIZED.value());
		}

		Claims claims = adminJwtUtils.getClaimByToken(token);
		if (claims == null || adminJwtUtils.isTokenExpired(claims.getExpiration())) {
			throw new RRException(adminJwtUtils.getHeader() + "失效，请重新登录", HttpStatus.UNAUTHORIZED.value());
		}

		long adminId = Long.parseLong(claims.getSubject());
		SysAdminEntity admin = sysAdminDao.selectById(adminId);
		if (admin == null) {
			throw new RRException("帐号不存在", HttpStatus.UNAUTHORIZED.value());
		}
		if (admin.getStatus() != null && admin.getStatus() == 0) {
			throw new RRException("账号已被禁用", HttpStatus.UNAUTHORIZED.value());
		}

		request.setAttribute(ADMIN_ID_ATTR, adminId);
		request.setAttribute(ADMIN_USERNAME_ATTR, admin.getUsername() != null ? admin.getUsername() : "");
		return true;
	}
}
