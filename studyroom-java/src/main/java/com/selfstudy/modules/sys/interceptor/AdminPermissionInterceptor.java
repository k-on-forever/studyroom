package com.selfstudy.modules.sys.interceptor;

import com.selfstudy.common.annotation.RequiresPerm;
import com.selfstudy.common.exception.RRException;
import com.selfstudy.modules.sys.service.AdminPermissionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 在 {@link SysAdminAuthInterceptor} 之后执行；仅拦截带 {@link RequiresPerm} 的处理器。
 */
@Component
@RequiredArgsConstructor
public class AdminPermissionInterceptor implements HandlerInterceptor {

	private final AdminPermissionService adminPermissionService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod)) {
			return true;
		}
		HandlerMethod hm = (HandlerMethod) handler;
		RequiresPerm ann = hm.getMethodAnnotation(RequiresPerm.class);
		if (ann == null) {
			ann = hm.getBeanType().getAnnotation(RequiresPerm.class);
		}
		if (ann == null) {
			return true;
		}

		Long adminId = (Long) request.getAttribute(SysAdminAuthInterceptor.ADMIN_ID_ATTR);
		if (adminId == null) {
			throw new RRException("请登录", HttpStatus.UNAUTHORIZED.value());
		}
		String perm = ann.value();
		if (!adminPermissionService.hasPermission(adminId, perm)) {
			throw new RRException("无权执行该操作", HttpStatus.FORBIDDEN.value());
		}
		return true;
	}
}
