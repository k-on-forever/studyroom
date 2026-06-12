package com.selfstudy.modules.bas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.bas.entity.BasFloorEntity;
import com.selfstudy.modules.bas.entity.BasStudyRoomEntity;
import com.selfstudy.modules.bas.service.BasFloorService;
import com.selfstudy.modules.bas.service.BasStudyRoomService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理端：楼层 CRUD（需登录管理员，走 /sys/** 鉴权）
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/bas/floor")
@Tag(name = "管理-楼层")
public class SysBasFloorController {

	private final BasFloorService basFloorService;
	private final BasStudyRoomService basStudyRoomService;

	@GetMapping("/list")
	@Operation(summary = "列表")
	public R list() {
		return R.ok().put("data", basFloorService.list());
	}

	@GetMapping("/info/{id}")
	@Operation(summary = "详情")
	public R info(@PathVariable("id") Long id) {
		BasFloorEntity e = basFloorService.getById(id);
		if (e == null) {
			return R.error("记录不存在");
		}
		return R.ok().put("data", e);
	}

	@PostMapping("/save")
	@Operation(summary = "新增")
	public R save(@RequestBody FloorForm form) {
		if (!StringUtils.hasText(form.getFloorName())) {
			return R.error("楼层名称不能为空");
		}
		BasFloorEntity e = new BasFloorEntity();
		e.setFloorName(form.getFloorName().trim());
		return basFloorService.save(e) ? R.ok() : R.error("保存失败");
	}

	@PostMapping("/update")
	@Operation(summary = "修改")
	public R update(@RequestBody FloorForm form) {
		if (form.getId() == null) {
			return R.error("id 不能为空");
		}
		if (!StringUtils.hasText(form.getFloorName())) {
			return R.error("楼层名称不能为空");
		}
		BasFloorEntity e = basFloorService.getById(form.getId());
		if (e == null) {
			return R.error("记录不存在");
		}
		e.setFloorName(form.getFloorName().trim());
		return basFloorService.updateById(e) ? R.ok() : R.error("更新失败");
	}

	@PostMapping("/delete")
	@Operation(summary = "删除")
	public R delete(@RequestBody Map<String, Long> body) {
		Long id = body == null ? null : body.get("id");
		if (id == null) {
			return R.error("id 不能为空");
		}
		long n = basStudyRoomService.count(new LambdaQueryWrapper<BasStudyRoomEntity>()
				.eq(BasStudyRoomEntity::getFloorId, id));
		if (n > 0) {
			return R.error("该楼层下仍有自习室，请先删除或移走自习室");
		}
		return basFloorService.removeById(id) ? R.ok() : R.error("删除失败");
	}

	@Data
	public static class FloorForm {
		private Long id;
		private String floorName;
	}
}
