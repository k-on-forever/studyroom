package com.selfstudy.modules.applet.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "忘记密码重置（短信模拟）")
public class PasswordResetForm {
	@NotBlank(message = "手机号不能为空")
	private String account;
	@NotBlank(message = "密码不能为空")
	private String password;
	@NotBlank(message = "请再次输入密码")
	private String confirmPassword;
	@NotBlank(message = "验证码不能为空")
	private String smsCode;
}
