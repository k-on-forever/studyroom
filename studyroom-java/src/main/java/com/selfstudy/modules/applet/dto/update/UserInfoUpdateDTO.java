package com.selfstudy.modules.applet.dto.update;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户信息修改传输模型（非填项可空；有值则须符合格式）。
 */
@Data
public class UserInfoUpdateDTO {

	@Size(max = 32, message = "姓名最多 32 字")
	private String name;

	/** 空串视为未填；有值须为大陆 11 位手机号 */
	@Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
	private String mobile;

	/** 5～12 位十进制；可空 */
	@Min(value = 10_000L, message = "QQ 号格式不正确")
	@Max(value = 9_999_999_999_999L, message = "QQ 号格式不正确")
	private Long qq;

	/** 空串视为未填；有值须含 @ 与域名 */
	@Pattern(
			regexp = "^$|^[^@\\s]{1,64}@[^@\\s]{1,255}\\.[^@\\s.]{2,}$",
			message = "邮箱格式不正确")
	private String email;

	@Size(max = 100, message = "备注最多 100 字")
	private String bz;

	@Size(max = 512, message = "头像地址过长")
	private String userImg;
}
