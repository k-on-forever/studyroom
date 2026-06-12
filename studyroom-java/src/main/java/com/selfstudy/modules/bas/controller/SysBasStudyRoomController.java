package com.selfstudy.modules.bas.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.bas.entity.BasSeatEntity;
import com.selfstudy.modules.bas.entity.BasStudyRoomEntity;
import com.selfstudy.config.ReservationProperties;
import com.selfstudy.modules.bas.service.BasSeatService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 管理端：自习室 CRUD
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/bas/studyroom")
@Tag(name = "管理-自习室")
public class SysBasStudyRoomController {

	private final BasStudyRoomService basStudyRoomService;
	private final BasSeatService basSeatService;
	private final ReservationProperties reservationProperties;

	@GetMapping("/list")
	@Operation(summary = "列表，可按 floorId 筛选")
	public R list(@RequestParam(value = "floorId", required = false) Long floorId) {
		if (floorId == null) {
			return R.ok().put("data", basStudyRoomService.list());
		}
		List<BasStudyRoomEntity> list = basStudyRoomService.getRoomByFloor(floorId);
		return R.ok().put("data", list);
	}

	@GetMapping("/info/{id}")
	@Operation(summary = "详情")
	public R info(@PathVariable("id") Long id) {
		BasStudyRoomEntity e = basStudyRoomService.getById(id);
		if (e == null) {
			return R.error("记录不存在");
		}
		return R.ok().put("data", e);
	}

	@PostMapping("/save")
	@Operation(summary = "新增")
	public R save(@RequestBody StudyRoomForm form) {
		R slotErr = validateSlotStep(form.getSlotStepMinutes());
		if (slotErr != null) {
			return slotErr;
		}
		if (form.getFloorId() == null) {
			return R.error("楼层不能为空");
		}
		if (!StringUtils.hasText(form.getRoomName())) {
			return R.error("自习室名称不能为空");
		}
		BasStudyRoomEntity e = new BasStudyRoomEntity();
		e.setFloorId(form.getFloorId());
		e.setRoomName(form.getRoomName().trim());
		e.setRoomLocation(StringUtils.hasText(form.getRoomLocation()) ? form.getRoomLocation().trim() : null);
		e.setOpeningTime(StringUtils.hasText(form.getOpeningTime()) ? form.getOpeningTime().trim() : null);
		e.setCloseTime(StringUtils.hasText(form.getCloseTime()) ? form.getCloseTime().trim() : null);
		e.setSeatRows(form.getSeatRows() == null ? 0 : form.getSeatRows());
		e.setSeatCols(form.getSeatCols() == null ? 0 : form.getSeatCols());
		e.setSlotStepMinutes(form.getSlotStepMinutes() != null ? form.getSlotStepMinutes()
				: reservationProperties.getSlotMinutes());
		return basStudyRoomService.save(e) ? R.ok() : R.error("保存失败");
	}

	@PostMapping("/update")
	@Operation(summary = "修改")
	public R update(@RequestBody StudyRoomForm form) {
		R slotErr = validateSlotStep(form.getSlotStepMinutes());
		if (slotErr != null) {
			return slotErr;
		}
		if (form.getId() == null) {
			return R.error("id 不能为空");
		}
		if (form.getFloorId() == null) {
			return R.error("楼层不能为空");
		}
		if (!StringUtils.hasText(form.getRoomName())) {
			return R.error("自习室名称不能为空");
		}
		BasStudyRoomEntity e = basStudyRoomService.getById(form.getId());
		if (e == null) {
			return R.error("记录不存在");
		}
		e.setFloorId(form.getFloorId());
		e.setRoomName(form.getRoomName().trim());
		e.setRoomLocation(StringUtils.hasText(form.getRoomLocation()) ? form.getRoomLocation().trim() : null);
		e.setOpeningTime(StringUtils.hasText(form.getOpeningTime()) ? form.getOpeningTime().trim() : null);
		e.setCloseTime(StringUtils.hasText(form.getCloseTime()) ? form.getCloseTime().trim() : null);
		if (form.getSeatRows() != null) {
			e.setSeatRows(form.getSeatRows());
		}
		if (form.getSeatCols() != null) {
			e.setSeatCols(form.getSeatCols());
		}
		if (form.getSlotStepMinutes() != null) {
			e.setSlotStepMinutes(form.getSlotStepMinutes());
		}
		return basStudyRoomService.updateById(e) ? R.ok() : R.error("更新失败");
	}

	private R validateSlotStep(Integer slotStepMinutes) {
		int global = reservationProperties.getSlotMinutes();
		int step = slotStepMinutes != null ? slotStepMinutes : global;
		if (step != 10 && step != 30 && step != 60) {
			return R.error("默认时段粒度仅支持 10、30 或 60 分钟");
		}
		if (step != global) {
			return R.error("须与系统全局预约槽一致（当前为 " + global + " 分钟，配置项 study.reservation.slot-minutes）。"
					+ "修改全局槽宽后，所有自习室应使用同一粒度。");
		}
		return null;
	}

	@PostMapping("/delete")
	@Operation(summary = "删除")
	public R delete(@RequestBody Map<String, Long> body) {
		Long id = body == null ? null : body.get("id");
		if (id == null) {
			return R.error("id 不能为空");
		}
		long n = basSeatService.count(new LambdaQueryWrapper<BasSeatEntity>()
				.eq(BasSeatEntity::getRoomId, id));
		if (n > 0) {
			return R.error("该自习室下仍有座位，请先删除座位");
		}
		return basStudyRoomService.removeById(id) ? R.ok() : R.error("删除失败");
	}

	@Data
	public static class StudyRoomForm {
		private Long id;
		private Long floorId;
		private String roomName;
		private String roomLocation;
		private String openingTime;
		private String closeTime;
		private Integer seatRows;
		private Integer seatCols;
		/** 默认时段长度（分钟），须等于 study.reservation.slot-minutes */
		private Integer slotStepMinutes;
	}
}
