package com.selfstudy.modules.bas.controller;

import com.selfstudy.common.annotation.RequiresPerm;
import com.selfstudy.common.annotation.SysLog;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.sys.entity.SysReservationRuleEntity;
import com.selfstudy.modules.sys.service.ReservationRuleConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预约业务规则：提前预约天数、单次最长时间、取消前提前量
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/bas/reservation-rule")
@Tag(name = "管理-预约规则")
public class SysReservationRuleController {

	private final ReservationRuleConfigService reservationRuleConfig;

	@GetMapping("/info")
	@RequiresPerm("bas:reservation:rule")
	@Operation(summary = "当前规则")
	public R info() {
		try {
			return R.ok()
					.put("data", reservationRuleConfig.getForAdmin())
					.put("operatingSpanMinutes", reservationRuleConfig.getOperatingSpanMinutes());
		} catch (Exception e) {
			log.warn("加载预约规则失败", e);
			return R.error("加载失败：请确认库中已存在表 sys_reservation_rule。可在业务库执行 studyroom-java/src/main/resources/db/schema-studyroom-migration-sys-reservation-rule.sql（或 schema-studyroom-core.sql / schema-studyroom-migration-v2-bas-reservation.sql 中的建表语句），执行后重启后端再试。");
		}
	}

	@PostMapping("/save")
	@RequiresPerm("bas:reservation:rule")
	@SysLog("保存预约规则")
	@Operation(summary = "保存（单行覆盖 id=1）")
	public R save(@RequestBody SysReservationRuleEntity body) {
		try {
			if (body.getId() == null) {
				body.setId(ReservationRuleConfigService.DEFAULT_ID);
			}
			reservationRuleConfig.saveRule(body);
			return R.ok();
		} catch (IllegalArgumentException e) {
			return R.error(e.getMessage());
		} catch (Exception e) {
			return R.error("保存失败");
		}
	}
}
