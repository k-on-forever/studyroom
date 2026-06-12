package com.selfstudy.modules.sys.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理后台登录（与 Vue 端字段一致：username / password / uuid / captcha）
 */
@Data
@Schema(description = "管理后台登录表单")
public class SysWebLoginForm {

	@NotBlank(message = "帐号不能为空")
	@Schema(description = "帐号")
	private String username;

	@NotBlank(message = "密码不能为空")
	@Schema(description = "密码")
	private String password;

	@NotBlank(message = "uuid不能为空")
	@Schema(description = "验证码会话 uuid")
	private String uuid;

	@NotBlank(message = "验证码不能为空")
	@Schema(description = "验证码")
	private String captcha;
}
