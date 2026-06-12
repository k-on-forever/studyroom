package com.selfstudy.config;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 根路径说明页：便于确认服务已监听，并给出 Swagger 入口（相对路径，适配 context-path）。
 */
@RestController
public class WelcomeController {

	@GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE + ";charset=UTF-8")
	public String home() {
		return "<!DOCTYPE html>\n"
				+ "<html lang=\"zh-CN\">\n"
				+ "<head><meta charset=\"UTF-8\"/><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/><title>自习室 API</title></head>\n"
				+ "<body style=\"font-family:sans-serif;max-width:720px;margin:2rem auto;line-height:1.6\">\n"
				+ "<h1>自习室后端已就绪</h1>\n"
				+ "<p>若你能看到本页，说明 Tomcat 已在当前地址监听。</p>\n"
				+ "<ul>\n"
				+ "  <li><a href=\"swagger-ui/index.html\"><strong>Swagger UI（推荐）</strong></a></li>\n"
				+ "  <li><a href=\"swagger-ui.html\">swagger-ui.html（重定向入口）</a></li>\n"
				+ "  <li><a href=\"v3/api-docs\">OpenAPI JSON（v3/api-docs）</a></li>\n"
				+ "</ul>\n"
				+ "<hr/>\n"
				+ "<p><strong>Cursor 内置 Simple Browser</strong> 有时无法访问本机 <code>localhost</code>，会出现 <code>ERR_CONNECTION_REFUSED</code>。"
				+ "请改用系统里的 <strong>Chrome / Edge</strong> 打开同一地址；或把地址里的 <code>localhost</code> 换成 <code>127.0.0.1</code> 再试。</p>\n"
				+ "<p>若本页也打不开，请在 IntelliJ 先启动 <code>Application</code>，并确认控制台出现 <code>Tomcat started on port 8080</code>。</p>\n"
				+ "</body></html>\n";
	}
}
