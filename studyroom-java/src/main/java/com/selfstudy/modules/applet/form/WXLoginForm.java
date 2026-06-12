package com.selfstudy.modules.applet.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信登录表单对象
 */
@Data
@Schema(description = "微信登录表单对象")
public class WXLoginForm {

	@NotBlank(message = "登录凭证不能为空")
	@Schema(description = "登录凭证", requiredMode = Schema.RequiredMode.REQUIRED)
	private String code;

	private String nickName;

	private String avatarUrl;

}
