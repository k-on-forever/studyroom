package com.selfstudy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域：来源由 study.cors.allowed-origin-patterns 配置，生产请填写管理端/正式域名。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final StudyCorsProperties corsProperties;

	public CorsConfig(StudyCorsProperties corsProperties) {
		this.corsProperties = corsProperties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		if (CollectionUtils.isEmpty(corsProperties.getAllowedOriginPatterns())) {
			return;
		}
		registry.addMapping("/**")
				.allowedOriginPatterns(corsProperties.getAllowedOriginPatterns()
						.toArray(new String[0]))
				.allowCredentials(corsProperties.isAllowCredentials())
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.maxAge(3600);
	}
}
