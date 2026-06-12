package com.selfstudy.modules.applet.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 注册表单
 */
@Data
@Schema(description = "注册表单")
public class RegisterForm {
	@Schema(description = "手机号")
	@NotBlank(message = "手机号不能为空")
	private String account;

	@Schema(description = "密码")
	@NotBlank(message = "密码不能为空")
	private String password;

	@Schema(description = "确认密码")
	@NotBlank(message = "请再次输入密码")
	private String confirmPassword;

	@Schema(description = "短信验证码（模拟）")
	@NotBlank(message = "验证码不能为空")
	private String smsCode;

}
