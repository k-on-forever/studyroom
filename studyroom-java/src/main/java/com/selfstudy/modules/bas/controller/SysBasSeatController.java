package com.selfstudy.modules.bas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.bas.entity.BasAppointmentEntity;
import com.selfstudy.modules.bas.entity.BasSeatEntity;
import com.selfstudy.modules.bas.service.BasAppointmentService;
import com.selfstudy.modules.bas.service.BasSeatLayoutService;
import com.selfstudy.modules.bas.service.BasSeatService;
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

import java.util.List;
import java.util.Map;

/**
 * 管理端：座位 CRUD
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/bas/seat")
@Tag(name = "管理-座位")
public class SysBasSeatController {

	private final BasSeatService basSeatService;
	private final BasAppointmentService basAppointmentService;
	private final BasSeatLayoutService basSeatLayoutService;

	@GetMapping("/list")
	@Operation(summary = "列表，可按 roomId 筛选")
	public R list(@RequestParam(value = "roomId", required = false) Long roomId) {
		if (roomId == null) {
			return R.ok().put("data", basSeatService.list());
		}
		List<BasSeatEntity> list = basSeatService.getSeatByRoom(roomId);
		return R.ok().put("data", list);
	}

	@GetMapping("/info/{id}")
	@Operation(summary = "详情")
	public R info(@PathVariable("id") Long id) {
		BasSeatEntity e = basSeatService.getById(id);
		if (e == null) {
			return R.error("记录不存在");
		}
		return R.ok().put("data", e);
	}

	@PostMapping("/save")
	@Operation(summary = "新增")
	public R save(@RequestBody SeatForm form) {
		if (form.getRoomId() == null) {
			return R.error("自习室不能为空");
		}
		if (!StringUtils.hasText(form.getSeatName())) {
			return R.error("座位名称/编号不能为空");
		}
		BasSeatEntity e = new BasSeatEntity();
		e.setRoomId(form.getRoomId());
		e.setSeatName(form.getSeatName().trim());
		e.setGridRow(form.getGridRow() != null ? form.getGridRow() : 0);
		e.setGridCol(form.getGridCol() != null ? form.getGridCol() : 0);
		e.setSeatType(form.getSeatType() != null ? form.getSeatType() : 0);
		e.setLocked(form.getLocked() != null && form.getLocked() == 1 ? 1 : 0);
		return basSeatService.save(e) ? R.ok() : R.error("保存失败");
	}

	@PostMapping("/update")
	@Operation(summary = "修改")
	public R update(@RequestBody SeatForm form) {
		if (form.getId() == null) {
			return R.error("id 不能为空");
		}
		if (form.getRoomId() == null) {
			return R.error("自习室不能为空");
		}
		if (!StringUtils.hasText(form.getSeatName())) {
			return R.error("座位名称/编号不能为空");
		}
		BasSeatEntity e = basSeatService.getById(form.getId());
		if (e == null) {
			return R.error("记录不存在");
		}
		e.setRoomId(form.getRoomId());
		e.setSeatName(form.getSeatName().trim());
		if (form.getGridRow() != null) {
			e.setGridRow(form.getGridRow());
		}
		if (form.getGridCol() != null) {
			e.setGridCol(form.getGridCol());
		}
		if (form.getSeatType() != null) {
			e.setSeatType(form.getSeatType());
		}
		if (form.getLocked() != null) {
			e.setLocked(form.getLocked() == 1 ? 1 : 0);
		}
		return basSeatService.updateById(e) ? R.ok() : R.error("更新失败");
	}

	@PostMapping("/lock")
	@Operation(summary = "锁座/解锁：locked=1 不可预约")
	public R setLock(@RequestBody SeatLockForm form) {
		if (form.getId() == null) {
			return R.error("id 不能为空");
		}
		BasSeatEntity e = basSeatService.getById(form.getId());
		if (e == null) {
			return R.error("记录不存在");
		}
		e.setLocked(form.getLocked() != null && form.getLocked() == 1 ? 1 : 0);
		return basSeatService.updateById(e) ? R.ok() : R.error("更新失败");
	}

	@PostMapping("/layout-save")
	@Operation(summary = "可视化座位布局保存（行列从1起）")
	public R layoutSave(@RequestBody Map<String, Object> body) {
		if (body == null || body.get("roomId") == null) {
			return R.error("roomId 不能为空");
		}
		long roomId = Long.parseLong(String.valueOf(body.get("roomId")));
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> raw = (List<Map<String, Object>>) body.get("cells");
		if (raw == null) {
			return R.error("cells 不能为空");
		}
		List<BasSeatLayoutService.CanvasSeatCell> cells = new java.util.ArrayList<>();
		for (Map<String, Object> m : raw) {
			if (m == null) {
				continue;
			}
			BasSeatLayoutService.CanvasSeatCell c = new BasSeatLayoutService.CanvasSeatCell();
			if (m.get("id") != null) {
				c.setId(Long.parseLong(String.valueOf(m.get("id"))));
			}
			if (m.get("r") != null) {
				c.setR(Integer.parseInt(String.valueOf(m.get("r"))));
			}
			if (m.get("c") != null) {
				c.setC(Integer.parseInt(String.valueOf(m.get("c"))));
			}
			if (m.get("seatName") != null) {
				c.setSeatName(String.valueOf(m.get("seatName")));
			}
			if (m.get("seatType") != null) {
				c.setSeatType(Integer.parseInt(String.valueOf(m.get("seatType"))));
			}
			if (m.get("deleted") != null) {
				c.setDeleted(Boolean.parseBoolean(String.valueOf(m.get("deleted"))));
			}
			cells.add(c);
		}
		try {
			basSeatLayoutService.saveCanvasLayout(roomId, cells);
			return R.ok();
		} catch (IllegalArgumentException | IllegalStateException ex) {
			return R.error(ex.getMessage());
		} catch (Exception e) {
			return R.error("保存失败");
		}
	}

	@PostMapping("/batch-generate")
	@Operation(summary = "按行列批量重排本室座位（有预约的座位存在时禁止）")
	public R batchGenerate(@RequestBody Map<String, Object> body) {
		if (body == null || body.get("roomId") == null) {
			return R.error("roomId 不能为空");
		}
		long roomId = Long.parseLong(String.valueOf(body.get("roomId")));
		int rows = body.get("rows") != null ? Integer.parseInt(String.valueOf(body.get("rows"))) : 0;
		int cols = body.get("cols") != null ? Integer.parseInt(String.valueOf(body.get("cols"))) : 0;
		String prefix = body.get("namePrefix") == null ? null : String.valueOf(body.get("namePrefix"));
		try {
			basSeatLayoutService.batchRegenerateSeats(roomId, rows, cols, prefix);
			return R.ok();
		} catch (IllegalArgumentException | IllegalStateException ex) {
			return R.error(ex.getMessage());
		} catch (Exception e) {
			return R.error("重排失败");
		}
	}

	@PostMapping("/delete")
	@Operation(summary = "删除")
	public R delete(@RequestBody Map<String, Long> body) {
		Long id = body == null ? null : body.get("id");
		if (id == null) {
			return R.error("id 不能为空");
		}
		long n = basAppointmentService.count(new LambdaQueryWrapper<BasAppointmentEntity>()
				.eq(BasAppointmentEntity::getSeatId, id));
		if (n > 0) {
			return R.error("该座位已有关联预约，无法删除");
		}
		return basSeatService.removeById(id) ? R.ok() : R.error("删除失败");
	}

	@Data
	public static class SeatForm {
		private Long id;
		private Long roomId;
		private String seatName;
		private Integer gridRow;
		private Integer gridCol;
		private Integer seatType;
		private Integer locked;
	}

	@Data
	public static class SeatLockForm {
		private Long id;
		private Integer locked;
	}
}
