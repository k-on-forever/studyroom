package com.selfstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域配置。生产环境勿使用 {@code *} + credentials，在 application-prod 中配置具体域名。
 */
@Component
@ConfigurationProperties(prefix = "study.cors")
public class StudyCorsProperties {

	private List<String> allowedOriginPatterns = new ArrayList<>(List.of(
			"http://localhost:*",
			"http://127.0.0.1:*"));

	private boolean allowCredentials = true;

	public List<String> getAllowedOriginPatterns() {
		return allowedOriginPatterns;
	}

	public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
		this.allowedOriginPatterns = allowedOriginPatterns != null ? allowedOriginPatterns : new ArrayList<>();
	}

	public boolean isAllowCredentials() {
		return allowCredentials;
	}

	public void setAllowCredentials(boolean allowCredentials) {
		this.allowCredentials = allowCredentials;
	}
}
