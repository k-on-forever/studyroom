package com.selfstudy.modules.applet.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.common.validator.ValidatorUtils;
import com.selfstudy.config.MessageProperties;
import com.selfstudy.modules.applet.form.PasswordResetForm;
import com.selfstudy.modules.applet.service.UserService;
import com.selfstudy.modules.applet.support.AppletRegisterSmsCodeStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/applet/password")
@Tag(name = "小程序忘记密码")
public class AppPasswordController {

	private static final Pattern PHONE = Pattern.compile("^1[3-9]\\d{9}$");
	private static final String SMS_PURPOSE = "pwd_rst";

	private final AppletRegisterSmsCodeStore smsCodeStore;
	private final UserService userService;
	private final MessageProperties messageProperties;

	public AppPasswordController(AppletRegisterSmsCodeStore smsCodeStore, UserService userService,
			MessageProperties messageProperties) {
		this.smsCodeStore = smsCodeStore;
		this.userService = userService;
		this.messageProperties = messageProperties;
	}

	@PostMapping("/resetSmsSend")
	@Operation(summary = "发送重置密码验证码(模拟)")
	public R resetSmsSend(@RequestBody Map<String, String> body) {
		String account = body == null ? null : body.get("account");
		if (!StringUtils.hasText(account)) {
			return R.error("请填写手机号");
		}
		String a = account.trim();
		if (!PHONE.matcher(a).matches()) {
			return R.error("请输入正确手机号");
		}
		if (userService.queryByAccount(a) == null) {
			return R.error("该手机号未注册");
		}
		if (!smsCodeStore.allowSend(a, 55, SMS_PURPOSE)) {
			return R.error("发送过于频繁，请稍后再试");
		}
		try {
			smsCodeStore.saveNewCode(a, 5, SMS_PURPOSE);
		} catch (Exception ex) {
			return R.error("发送失败：" + ex.getMessage());
		}
		return R.ok("验证码已发送（模拟：请查看服务端日志）");
	}

	@PostMapping("/reset")
	@Operation(summary = "校验验证码并重置密码")
	public R reset(@RequestBody PasswordResetForm form) {
		ValidatorUtils.validateEntity(form);
		String account = form.getAccount().trim();
		if (!PHONE.matcher(account).matches()) {
			return R.error("请使用中国大陆手机号");
		}
		if (!Objects.equals(form.getPassword(), form.getConfirmPassword())) {
			return R.error("两次输入的密码不一致");
		}
		if (!smsCodeStore.verifyAndConsume(account, form.getSmsCode(), SMS_PURPOSE)) {
			return R.error("验证码错误或已过期");
		}
		if (userService.queryByAccount(account) == null) {
			return R.error("该手机号未注册");
		}
		boolean ok = userService.resetPassword(account, form.getPassword());
		if (ok) {
			return R.ok();
		}
		return R.error(messageProperties.getFormUpdateError());
	}
}
