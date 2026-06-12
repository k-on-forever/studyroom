package com.selfstudy.modules.bas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.bas.entity.BasNoticeEntity;
import com.selfstudy.modules.bas.service.BasNoticeService;
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

import java.util.Date;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/bas/notice")
@Tag(name = "管理-公告")
public class SysBasNoticeController {

	private final BasNoticeService basNoticeService;

	@GetMapping("/page")
	@Operation(summary = "分页")
	public R page(
			@RequestParam(value = "page", defaultValue = "1") long page,
			@RequestParam(value = "limit", defaultValue = "10") long limit,
			@RequestParam(value = "keyword", required = false) String keyword) {
		long p = Math.max(1L, page);
		long l = Math.min(100L, Math.max(1L, limit));
		LambdaQueryWrapper<BasNoticeEntity> w = new LambdaQueryWrapper<>();
		if (StringUtils.hasText(keyword)) {
			String k = keyword.trim();
			w.and(q -> q.like(BasNoticeEntity::getTitle, k).or().like(BasNoticeEntity::getContent, k));
		}
		w.orderByDesc(BasNoticeEntity::getCreateTime);
		Page<BasNoticeEntity> pg = basNoticeService.page(new Page<>(p, l), w);
		return R.ok().put("list", pg.getRecords()).put("totalCount", pg.getTotal());
	}

	@GetMapping("/info/{id}")
	@Operation(summary = "详情")
	public R info(@PathVariable("id") Long id) {
		return BaseCrud.info(basNoticeService, id);
	}

	@PostMapping("/save")
	@SysLog("新增公告")
	@Operation(summary = "新增")
	public R save(@RequestBody NoticeForm form) {
		if (!StringUtils.hasText(form.getTitle())) {
			return R.error("标题不能为空");
		}
		BasNoticeEntity e = new BasNoticeEntity();
		e.setTitle(form.getTitle().trim());
		e.setContent(StringUtils.hasText(form.getContent()) ? form.getContent().trim() : null);
		e.setCreateTime(new Date());
		return basNoticeService.save(e) ? R.ok() : R.error("保存失败");
	}

	@PostMapping("/update")
	@SysLog("修改公告")
	@Operation(summary = "修改")
	public R update(@RequestBody NoticeForm form) {
		if (form.getId() == null) {
			return R.error("id 不能为空");
		}
		if (!StringUtils.hasText(form.getTitle())) {
			return R.error("标题不能为空");
		}
		BasNoticeEntity e = basNoticeService.getById(form.getId());
		if (e == null) {
			return R.error("记录不存在");
		}
		e.setTitle(form.getTitle().trim());
		e.setContent(StringUtils.hasText(form.getContent()) ? form.getContent().trim() : null);
		return basNoticeService.updateById(e) ? R.ok() : R.error("更新失败");
	}

	@PostMapping("/delete")
	@SysLog("删除公告")
	@Operation(summary = "删除")
	public R delete(@RequestBody Map<String, Long> body) {
		return BaseCrud.delete(basNoticeService, body);
	}

	@Data
	public static class NoticeForm {
		private Long id;
		private String title;
		private String content;
	}
}
