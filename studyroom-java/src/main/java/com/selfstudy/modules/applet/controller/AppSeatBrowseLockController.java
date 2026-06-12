package com.selfstudy.modules.applet.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.common.validator.ValidatorUtils;
import com.selfstudy.modules.applet.annotation.Login;
import com.selfstudy.modules.applet.dto.SeatBrowseLockForm;
import com.selfstudy.modules.bas.dao.BasSeatDao;
import com.selfstudy.modules.bas.entity.BasSeatEntity;
import com.selfstudy.modules.bas.entity.BasStudyRoomEntity;
import com.selfstudy.modules.bas.service.BasStudyRoomService;
import com.selfstudy.modules.reservation.SeatBrowseLockService;
import com.selfstudy.modules.reservation.SeatDayDisplay;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/applet/seatBrowseLock")
@Tag(name = "小程序-选座占用展示")
public class AppSeatBrowseLockController {

	@Autowired
	private SeatBrowseLockService browseLockService;
	@Autowired
	private BasStudyRoomService basStudyRoomService;
	@Autowired
	private BasSeatDao basSeatDao;

	@Login
	@PostMapping("/hold")
	@ConditionalOnProperty(name = "study.browse-lock.enabled", havingValue = "true")
	@Operation(summary = "暂锁座位时段（已默认关闭，见 study.browse-lock.enabled）")
	public R hold(@RequestAttribute("userId") Long userId, @RequestBody SeatBrowseLockForm form) {
		ValidatorUtils.validateEntity(form);
		BasStudyRoomEntity room = resolveRoomForSeat(form.getSeatId());
		boolean ok = browseLockService.tryHold(userId, form.getSeatId(), form.getSeatDay(), room);
		if (!ok) {
			String occ = SeatDayDisplay.withOccupiedPrefix(form.getSeatDay());
			return R.error(
					org.springframework.util.StringUtils.hasText(occ)
							? "该座位已被他人暂锁，" + occ + "，请更换座位或时段"
							: "该座位当前时段已被他人暂锁，请更换座位或时段");
		}
		return R.ok();
	}

	@Login
	@PostMapping("/release")
	@ConditionalOnProperty(name = "study.browse-lock.enabled", havingValue = "true")
	@Operation(summary = "释放本人暂锁")
	public R release(@RequestAttribute("userId") Long userId, @RequestBody SeatBrowseLockForm form) {
		ValidatorUtils.validateEntity(form);
		BasStudyRoomEntity room = resolveRoomForSeat(form.getSeatId());
		browseLockService.release(userId, form.getSeatId(), form.getSeatDay(), room);
		return R.ok();
	}

	@Login
	@GetMapping("/mine")
	@ConditionalOnProperty(name = "study.browse-lock.enabled", havingValue = "true")
	@Operation(summary = "我的暂锁列表")
	public R mine(@RequestAttribute("userId") Long userId) {
		List<Map<String, Object>> list = browseLockService.listMine(userId);
		return R.ok().put("data", list);
	}

	@Login
	@GetMapping("/overlay")
	@Operation(summary = "选座占用：已预约 + 门店授权规则（不含浏览暂锁）")
	public R overlay(@RequestAttribute("userId") Long userId,
			@RequestParam("roomId") Long roomId,
			@RequestParam("bizDate") String bizDate,
			@RequestParam(value = "seatDay", required = false) String seatDay) {
		BasStudyRoomEntity room = basStudyRoomService.getById(roomId);
		if (room == null) {
			return R.error("自习室不存在");
		}
		return R.ok().put("data", browseLockService.overlay(userId, roomId, bizDate, seatDay, room));
	}

	private BasStudyRoomEntity resolveRoomForSeat(Long seatId) {
		BasSeatEntity seat = basSeatDao.selectById(seatId);
		if (seat == null || seat.getRoomId() == null) {
			return null;
		}
		return basStudyRoomService.getById(seat.getRoomId());
	}
}
