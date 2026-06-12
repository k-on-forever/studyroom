package com.selfstudy.modules.bas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.bas.entity.BasMessageEntity;
import com.selfstudy.modules.bas.service.BasMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/bas/message")
@Tag(name = "管理-留言")
public class SysBasMessageController {

	private final BasMessageService basMessageService;

	@GetMapping("/page")
	@Operation(summary = "分页（全部类型）")
	public R page(
			@RequestParam(value = "page", defaultValue = "1") long page,
			@RequestParam(value = "limit", defaultValue = "10") long limit,
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "messageType", required = false) Integer messageType) {
		long p = Math.max(1L, page);
		long l = Math.min(100L, Math.max(1L, limit));
		LambdaQueryWrapper<BasMessageEntity> w = new LambdaQueryWrapper<>();
		if (messageType != null) {
			w.eq(BasMessageEntity::getMessageType, messageType);
		}
		if (StringUtils.hasText(keyword)) {
			String k = keyword.trim();
			w.and(q -> q.like(BasMessageEntity::getUsername, k)
					.or().like(BasMessageEntity::getMessage, k));
		}
		w.orderByDesc(BasMessageEntity::getCreateTime);
		Page<BasMessageEntity> pg = basMessageService.page(new Page<>(p, l), w);
		return R.ok().put("list", pg.getRecords()).put("totalCount", pg.getTotal());
	}

	@PostMapping("/delete")
	@Operation(summary = "删除")
	public R delete(@RequestBody Map<String, Long> body) {
		return BaseCrud.delete(basMessageService, body);
	}
}
