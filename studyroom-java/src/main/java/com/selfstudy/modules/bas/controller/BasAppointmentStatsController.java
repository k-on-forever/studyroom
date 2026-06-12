package com.selfstudy.modules.bas.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.modules.bas.dao.BasAppointmentDao;
import com.selfstudy.modules.bas.vo.AppointmentRoomCountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端首页统计（Vue：GET /basappointment/count?date=yyyy-MM-dd）
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "预约统计")
public class BasAppointmentStatsController {

	private final BasAppointmentDao basAppointmentDao;

	@GetMapping("/basappointment/count")
	@Operation(summary = "按日期统计各自习室预约次数")
	public R countByRoom(@RequestParam(value = "date", required = false) String date) {
		String d = StringUtils.hasText(date) ? date.trim() : LocalDate.now().toString();
		List<AppointmentRoomCountVO> rows = basAppointmentDao.selectCountGroupByRoom(d);
		Map<String, Object> body = new HashMap<>();
		body.put("data", rows);
		return R.ok(body);
	}
}
