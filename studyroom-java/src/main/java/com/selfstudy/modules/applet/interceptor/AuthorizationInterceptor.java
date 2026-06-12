package com.selfstudy.modules.applet.interceptor;


import com.selfstudy.common.exception.RRException;
import com.selfstudy.modules.applet.annotation.Login;
import com.selfstudy.modules.applet.entity.TbTokenEntity;
import com.selfstudy.modules.applet.entity.UserEntity;
import com.selfstudy.modules.applet.service.TbTokenService;
import com.selfstudy.modules.applet.service.UserService;
import com.selfstudy.modules.applet.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 权限(Token)验证
 */
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

	@Autowired
	private JwtUtils jwtUtils;
	@Autowired
	private TbTokenService tbTokenService;
	@Autowired
	private UserService userService;

	public static final String USER_KEY = "userId";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		Login annotation;
		if (handler instanceof HandlerMethod) {
			annotation = ((HandlerMethod) handler).getMethodAnnotation(Login.class);
		} else {
			return true;
		}

		if (annotation == null) {
			return true;
		}

		String token = request.getHeader(jwtUtils.getHeader());
		if (StringUtils.isBlank(token)) {
			token = request.getParameter(jwtUtils.getHeader());
		}

		if (StringUtils.isBlank(token)) {
			throw new RRException("请登录", HttpStatus.UNAUTHORIZED.value());
		}

		TbTokenEntity tbTokenEntity = tbTokenService.queryByToken(token);
		if (tbTokenEntity == null || !tbTokenEntity.getToken().equals(token)) {
			throw new RRException(jwtUtils.getHeader() + "失效，请重新登录", HttpStatus.UNAUTHORIZED.value());
		}

		Claims claims = jwtUtils.getClaimByToken(token);
		if (claims == null || jwtUtils.isTokenExpired(claims.getExpiration())) {
			throw new RRException(jwtUtils.getHeader() + "失效，请重新登录", HttpStatus.UNAUTHORIZED.value());
		}

		UserEntity user = userService.queryUser(tbTokenEntity.getUserId());
		if (user == null) {
			throw new RRException("用户不存在", HttpStatus.UNAUTHORIZED.value());
		}
		if (user.getStatus() == 0) {
			throw new RRException("该用户已被封禁", HttpStatus.UNAUTHORIZED.value());
		}

		request.setAttribute(USER_KEY, Long.parseLong(claims.getSubject()));

		return true;
	}
}
