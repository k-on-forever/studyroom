package com.selfstudy.modules.bas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.user.entity.TbUserEntity;
import com.selfstudy.modules.user.service.TbUserService;
import com.selfstudy.common.annotation.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/bas/mini-user")
@Tag(name = "管理-小程序用户")
public class SysBasMiniUserController {

	private final TbUserService tbUserService;

	@GetMapping("/page")
	@Operation(summary = "分页")
	public R page(
			@RequestParam(value = "page", defaultValue = "1") long page,
			@RequestParam(value = "limit", defaultValue = "10") long limit,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "status", required = false) Integer status) {
		long p = Math.max(1L, page);
		long l = Math.min(100L, Math.max(1L, limit));
		LambdaQueryWrapper<TbUserEntity> w = new LambdaQueryWrapper<>();
		if (status != null) {
			w.eq(TbUserEntity::getStatus, status);
		}
		if (StringUtils.hasText(keyword)) {
			String k = keyword.trim();
			w.and(q -> q.like(TbUserEntity::getMobile, k)
					.or().like(TbUserEntity::getName, k)
					.or().like(TbUserEntity::getUsername, k)
					.or().like(TbUserEntity::getAccount, k)
					.or().like(TbUserEntity::getOpenId, k));
		}
		w.orderByDesc(TbUserEntity::getUserId);
		Page<TbUserEntity> pg = tbUserService.page(new Page<>(p, l), w);
		return R.ok().put("list", pg.getRecords()).put("totalCount", pg.getTotal());
	}

	@GetMapping("/info/{userId}")
	@Operation(summary = "详情")
	public R info(@PathVariable("userId") Long userId) {
		TbUserEntity e = tbUserService.getById(userId);
		if (e == null) {
			return R.error("用户不存在");
		}
		return R.ok().put("data", e);
	}

	@PostMapping("/status")
	@SysLog("封禁/解禁用户")
	@Operation(summary = "封禁/解禁 status：0封禁 1正常")
	public R status(@RequestBody StatusForm form) {
		if (form == null || form.getUserId() == null) {
			return R.error("userId 不能为空");
		}
		int st = form.getStatus() != null && form.getStatus() == 0 ? 0 : 1;
		TbUserEntity e = tbUserService.getById(form.getUserId());
		if (e == null) {
			return R.error("用户不存在");
		}
		e.setStatus(st);
		return tbUserService.updateById(e) ? R.ok() : R.error("更新失败");
	}

	@Data
	public static class StatusForm {
		private Long userId;
		private Integer status;
	}
}
