package com.selfstudy.modules.applet.config;

import com.selfstudy.modules.applet.interceptor.AuthorizationInterceptor;
import com.selfstudy.modules.applet.resolver.LoginUserHandlerMethodArgumentResolver;
import com.selfstudy.modules.sys.interceptor.AdminPermissionInterceptor;
import com.selfstudy.modules.sys.interceptor.SysAdminAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import com.selfstudy.config.StudyUploadProperties;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * MVC配置
 *
 * @author kon-foreverkon-forever
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired
    private AuthorizationInterceptor authorizationInterceptor;
    @Autowired
    private SysAdminAuthInterceptor sysAdminAuthInterceptor;
    @Autowired
    private AdminPermissionInterceptor adminPermissionInterceptor;
	@Autowired
	private LoginUserHandlerMethodArgumentResolver loginUserHandlerMethodArgumentResolver;
	@Autowired
	private StudyUploadProperties studyUploadProperties;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String dir = studyUploadProperties.resolveAvatarDir().toUri().toString();
		if (!dir.endsWith("/")) {
			dir = dir + "/";
		}
		registry.addResourceHandler("/upload/avatar/**").addResourceLocations(dir);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authorizationInterceptor)
                .addPathPatterns("/applet/**")
                .excludePathPatterns("/applet/wxpay/notify");
        registry.addInterceptor(sysAdminAuthInterceptor)
                .addPathPatterns("/sys/**")
                .excludePathPatterns("/sys/login")
                .order(1);
        registry.addInterceptor(adminPermissionInterceptor)
                .addPathPatterns("/sys/**")
                .excludePathPatterns("/sys/login")
                .order(2);
    }
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(loginUserHandlerMethodArgumentResolver);
    }
}