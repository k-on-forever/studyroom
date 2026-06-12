package com.selfstudy.modules.sys.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.modules.sys.interceptor.SysAdminAuthInterceptor;
import com.selfstudy.modules.sys.service.SysNavService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前登录管理员的菜单树与权限标识（与前端动态路由约定字段一致）。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "管理后台-菜单")
public class SysMenuNavController {

	private final SysNavService sysNavService;

	@GetMapping("/sys/menu/nav")
	@Operation(summary = "当前用户菜单与权限")
	public R nav(@RequestAttribute(SysAdminAuthInterceptor.ADMIN_ID_ATTR) Long adminId) {
		return R.ok(sysNavService.buildNavPayload(adminId));
	}
}
