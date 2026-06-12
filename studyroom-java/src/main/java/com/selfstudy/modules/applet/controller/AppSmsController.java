package com.selfstudy.modules.applet.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.modules.applet.support.AppletRegisterSmsCodeStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 注册验证码（模拟短信），无需登录。
 */
@RestController
@RequestMapping("/applet/register")
@Tag(name = "小程序注册短信(模拟)")
public class AppSmsController {

	private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");

	private final AppletRegisterSmsCodeStore smsCodeStore;

	public AppSmsController(AppletRegisterSmsCodeStore smsCodeStore) {
		this.smsCodeStore = smsCodeStore;
	}

	@PostMapping("/smsSend")
	@Operation(summary = "发送注册验证码(模拟)")
	public R smsSend(@RequestBody Map<String, String> body) {
		String account = body == null ? null : body.get("account");
		if (!StringUtils.hasText(account)) {
			return R.error("请填写手机号");
		}
		String a = account.trim();
		if (!PHONE.matcher(a).matches()) {
			return R.error("请输入正确手机号");
		}
		if (!smsCodeStore.allowSend(a, 55)) {
			return R.error("发送过于频繁，请稍后再试");
		}
		try {
			smsCodeStore.saveNewCode(a, 5);
		} catch (Exception ex) {
			return R.error("发送失败：" + ex.getMessage());
		}
		return R.ok("验证码已发送（模拟：请查看服务端日志）");
	}
}
