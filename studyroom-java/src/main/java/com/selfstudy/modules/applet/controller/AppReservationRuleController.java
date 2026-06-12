package com.selfstudy.modules.applet.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.modules.sys.service.ReservationRuleConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小程序读取预约规则（与 sys_reservation_rule 一致，无需登录）
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/applet")
@Tag(name = "小程序-预约规则")
public class AppReservationRuleController {

	private final ReservationRuleConfigService reservationRuleConfig;

	@GetMapping("/reservation-rule")
	@Operation(summary = "提前预约天数等（与选座页、TimeSlotCodec 校验一致）")
	public R reservationRule() {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("advanceBookingDays", reservationRuleConfig.getAdvanceBookingDays());
		return R.ok().put("data", data);
	}
}
