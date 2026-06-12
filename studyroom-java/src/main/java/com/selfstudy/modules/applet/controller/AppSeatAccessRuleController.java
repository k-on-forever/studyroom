package com.selfstudy.modules.applet.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.modules.applet.annotation.Login;
import com.selfstudy.modules.bas.service.BasSeatAccessRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 小程序：后台「时段策略」白名单（手机号凭证，非 Redis 暂锁）。
 */
@RestController
@RequestMapping("/applet/seatAccessRule")
@Tag(name = "小程序-座位时段策略")
public class AppSeatAccessRuleController {

	@Autowired
	private BasSeatAccessRuleService basSeatAccessRuleService;

	@Login
	@GetMapping("/mineWhitelist")
	@Operation(summary = "门店授权可订时段（按注册手机号匹配后台白名单）")
	public R mineWhitelist(@RequestAttribute("userId") Long userId) {
		Map<String, Object> data = basSeatAccessRuleService.whitelistPrivilegesForUser(userId);
		return R.ok().put("data", data);
	}
}
