package com.selfstudy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 生产 profile 启动时检查是否仍使用仓库内默认 JWT 密钥（应通过环境变量覆盖）。
 */
@Component
public class StudyProdSecurityListener {

	private static final Logger log = LoggerFactory.getLogger(StudyProdSecurityListener.class);

	private static final String DEFAULT_JWT = "f4e2e52034348f86b67cde581c0f9eb5";

	private final Environment environment;

	public StudyProdSecurityListener(Environment environment) {
		this.environment = environment;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
			return;
		}
		String adminSecret = environment.getProperty("study.admin.jwt.secret");
		String appletSecret = environment.getProperty("applet.jwt.secret");
		boolean badAdmin = DEFAULT_JWT.equals(adminSecret);
		boolean badApplet = DEFAULT_JWT.equals(appletSecret);
		if (badAdmin || badApplet) {
			log.error("【安全】生产环境检测到默认 JWT 密钥，请设置环境变量 STUDY_ADMIN_JWT_SECRET / APPLET_JWT_SECRET");
		}
		if ("true".equalsIgnoreCase(environment.getProperty("springdoc.api-docs.enabled", "true"))
				|| "true".equalsIgnoreCase(environment.getProperty("springdoc.swagger-ui.enabled", "true"))) {
			log.warn("【安全】生产环境仍启用 Swagger/OpenAPI，建议在 application-prod 中关闭 springdoc");
		}
	}
}
