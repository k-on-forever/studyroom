package com.selfstudy.common.aspect;

import com.google.gson.Gson;
import com.selfstudy.common.utils.HttpContextUtils;
import com.selfstudy.common.utils.IPUtils;
import com.selfstudy.modules.sys.entity.SysLogEntity;
import com.selfstudy.modules.sys.entity.SysUserEntity;
import com.selfstudy.modules.sys.service.SysLogService;
import com.selfstudy.common.annotation.SysLog;
import com.selfstudy.modules.sys.interceptor.SysAdminAuthInterceptor;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Date;


/**
 * 系统日志，切面处理类
 *
 * @author kon-foreverkon-forever
 */
@Aspect
@Component
public class SysLogAspect {
	@Autowired
	private SysLogService sysLogService;
	
	@Pointcut("@annotation(com.selfstudy.common.annotation.SysLog)")
	public void logPointCut() { 
		
	}

	@Around("logPointCut()")
	public Object around(ProceedingJoinPoint point) throws Throwable {
		long beginTime = System.currentTimeMillis();
		//执行方法
		Object result = point.proceed();
		//执行时长(毫秒)
		long time = System.currentTimeMillis() - beginTime;

		//保存日志
		saveSysLog(point, time);

		return result;
	}

	private void saveSysLog(ProceedingJoinPoint joinPoint, long time) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();

		SysLogEntity sysLog = new SysLogEntity();
		SysLog syslog = method.getAnnotation(SysLog.class);
		if(syslog != null){
			//注解上的描述
			sysLog.setOperation(syslog.value());
		}

		//请求的方法名
		String className = joinPoint.getTarget().getClass().getName();
		String methodName = signature.getName();
		sysLog.setMethod(className + "." + methodName + "()");

		//请求的参数
		Object[] args = joinPoint.getArgs();
		try{
			String params = new Gson().toJson(args);
			sysLog.setParams(params);
		}catch (Exception e){

		}

		//获取request
		HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
		//设置IP地址
		sysLog.setIp(IPUtils.getIpAddr(request));

		String username = "anonymous";
		if (request != null) {
			Object u = request.getAttribute(SysAdminAuthInterceptor.ADMIN_USERNAME_ATTR);
			if (u != null && u.toString().length() > 0) {
				username = u.toString();
			}
		}
		if ("anonymous".equals(username)) {
			try {
				Object principal = SecurityUtils.getSubject().getPrincipal();
				if (principal instanceof SysUserEntity) {
					username = ((SysUserEntity) principal).getUsername();
				}
			} catch (UnavailableSecurityManagerException | ClassCastException ignored) {
				// 小程序等场景未配置 Shiro SecurityManager
			}
		}
		sysLog.setUsername(username);

		sysLog.setTime(time);
		sysLog.setCreateDate(new Date());
		//保存系统日志
		sysLogService.save(sysLog);
	}
}
