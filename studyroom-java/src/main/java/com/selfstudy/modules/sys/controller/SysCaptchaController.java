package com.selfstudy.modules.sys.controller;

import com.selfstudy.common.utils.R;
import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.selfstudy.modules.sys.support.CaptchaCodeStore;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 图形验证码（PNG + uuid；正文存 {@link CaptchaCodeStore}，默认 Redis 失败时自动内存降级）
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "管理后台-验证码")
public class SysCaptchaController {

	private static final int TTL_MINUTES = 2;

	private final CaptchaCodeStore captchaCodeStore;

	@GetMapping("/captcha.jpg")
	@Operation(summary = "获取验证码图片")
	public void captcha(@RequestParam("uuid") String uuid, HttpServletResponse response) throws IOException {
		if (!StringUtils.hasText(uuid)) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "uuid required");
			return;
		}
		ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(112, 40, 4, 4);
		String code = captcha.getCode();
		captchaCodeStore.save(uuid, code, TTL_MINUTES);

		response.setHeader("Cache-Control", "no-store, no-cache");
		response.setContentType("image/jpeg");
		captcha.write(response.getOutputStream());
		response.flushBuffer();
	}

	/**
	 * 仅校验是否与当前 uuid 下未消费的验证码一致（不删除），供登录页失焦校验；与 {@link CaptchaCodeStore#verifyAndConsume} 规则一致（不区分大小写）。
	 */
	@GetMapping("/captcha/check")
	@Operation(summary = "校验验证码是否与图片一致（不消费）")
	public R captchaCheck(@RequestParam("uuid") String uuid, @RequestParam("code") String code) {
		if (!StringUtils.hasText(uuid) || !StringUtils.hasText(code)) {
			return R.error(400, "参数不完整");
		}
		if (captchaCodeStore.peekMatches(uuid, code)) {
			return R.ok();
		}
		return R.error(400, "验证码不正确");
	}
}
