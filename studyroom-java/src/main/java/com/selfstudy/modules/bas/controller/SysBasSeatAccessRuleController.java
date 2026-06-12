package com.selfstudy.modules.bas.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.modules.bas.entity.BasSeatAccessRuleEntity;
import com.selfstudy.modules.bas.service.BasSeatAccessRuleService;
import com.selfstudy.modules.bas.service.BasSeatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端：座位时段策略（全员锁座 / 仅白名单可订）
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/bas/seat-access-rule")
@Tag(name = "管理-座位时段策略")
public class SysBasSeatAccessRuleController {

	private final BasSeatAccessRuleService basSeatAccessRuleService;
	private final BasSeatService basSeatService;

	@GetMapping("/list")
	@Operation(summary = "按座位列出策略")
	public R list(@RequestParam("seatId") Long seatId) {
		if (seatId == null) {
			return R.error("seatId 不能为空");
		}
		if (basSeatService.getById(seatId) == null) {
			return R.error("座位不存在");
		}
		List<BasSeatAccessRuleEntity> list = basSeatAccessRuleService.listBySeatId(seatId);
		return R.ok().put("data", list);
	}

	@PostMapping("/save")
	@Operation(summary = "新增或修改")
	public R save(@RequestBody BasSeatAccessRuleEntity body) {
		try {
			basSeatAccessRuleService.saveRule(body);
			return R.ok();
		} catch (Exception e) {
			return R.error(e.getMessage() != null ? e.getMessage() : "保存失败");
		}
	}

	@PostMapping("/delete")
	@Operation(summary = "删除")
	public R delete(@RequestBody Map<String, Long> body) {
		Long id = body == null ? null : body.get("id");
		if (id == null) {
			return R.error("id 不能为空");
		}
		return basSeatAccessRuleService.removeById(id) ? R.ok() : R.error("删除失败");
	}

	@Data
	public static class ToggleBody {
		private Long id;
		private Integer enabled;
	}

	@PostMapping("/toggle-enabled")
	@Operation(summary = "启用/停用")
	public R toggle(@RequestBody ToggleBody body) {
		if (body == null || body.getId() == null || body.getEnabled() == null) {
			return R.error("参数不完整");
		}
		BasSeatAccessRuleEntity e = basSeatAccessRuleService.getById(body.getId());
		if (e == null) {
			return R.error("记录不存在");
		}
		e.setEnabled(body.getEnabled() == 1 ? 1 : 0);
		return basSeatAccessRuleService.updateById(e) ? R.ok() : R.error("更新失败");
	}
}
