package com.selfstudy.modules.sys.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.sys.dao.SysAdminDao;
import com.selfstudy.modules.sys.entity.SysAdminEntity;
import com.selfstudy.modules.sys.entity.SysAdminTokenEntity;
import com.selfstudy.modules.sys.form.SysWebLoginForm;
import com.selfstudy.modules.sys.service.SysAdminTokenService;
import com.selfstudy.modules.sys.support.AdminJwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import com.selfstudy.modules.sys.support.CaptchaCodeStore;
import com.selfstudy.modules.sys.support.CaptchaCodeStore.CaptchaOutcome;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理后台登录（Vue studyroom-vue），账号来自 {@code sys_admin}。
 */
@RestController
@Validated
@RequiredArgsConstructor
@Tag(name = "管理后台-登录")
public class SysLoginController {

	private final CaptchaCodeStore captchaCodeStore;
	private final SysAdminDao sysAdminDao;
	private final AdminJwtUtils adminJwtUtils;
	private final SysAdminTokenService sysAdminTokenService;

	@PostMapping("/sys/login")
	@Operation(summary = "登录")
	public R login(@Valid @RequestBody SysWebLoginForm form) {
		CaptchaOutcome cap = captchaCodeStore.verifyAndConsume(form.getUuid(), form.getCaptcha());
		if (cap == CaptchaOutcome.MISSING_OR_EXPIRED) {
			return R.error("验证码已失效，请刷新");
		}
		if (cap == CaptchaOutcome.MISMATCH) {
			return R.error("验证码错误");
		}

		SysAdminEntity admin = sysAdminDao.selectOne(new QueryWrapper<SysAdminEntity>()
				.eq("username", form.getUsername())
				.last("LIMIT 1"));
		if (admin == null) {
			return R.error("帐号或密码错误");
		}
		if (admin.getStatus() != null && admin.getStatus() == 0) {
			return R.error("账号已被禁用");
		}
		String hash = DigestUtils.sha256Hex(form.getPassword());
		if (admin.getPassword() == null || !admin.getPassword().equalsIgnoreCase(hash)) {
			return R.error("帐号或密码错误");
		}

		String token = adminJwtUtils.generateToken(admin.getAdminId());
		Map<String, Object> map = new HashMap<>();
		map.put("token", token);
		map.put("expire", adminJwtUtils.getExpire());

		Date now = new Date();
		Date expireTime = new Date(now.getTime() + adminJwtUtils.getExpire() * 1000L);
		SysAdminTokenEntity tokenRow = new SysAdminTokenEntity();
		tokenRow.setToken(token);
		tokenRow.setAdminId(admin.getAdminId());
		tokenRow.setUpdateTime(now);
		tokenRow.setExpireTime(expireTime);
		sysAdminTokenService.saveOrUpdate(tokenRow);

		return R.ok(map);
	}
}
