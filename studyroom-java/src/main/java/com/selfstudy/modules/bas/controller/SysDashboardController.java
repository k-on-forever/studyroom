package com.selfstudy.modules.bas.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.modules.bas.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/dashboard")
@RequiredArgsConstructor
@Tag(name = "管理-首页看板")
public class SysDashboardController {

	private final AdminAnalyticsService adminAnalyticsService;

	@GetMapping("/overview")
	@Operation(summary = "预约总览与实时占用（可选业务日）")
	public R overview(@RequestParam(value = "bizDate", required = false) String bizDate) {
		String d = StringUtils.hasText(bizDate) ? bizDate.trim() : null;
		return R.ok().put("data", adminAnalyticsService.overviewForBizDate(d));
	}

	@GetMapping("/revenue7d")
	@Operation(summary = "近7日营收（会员订单+按次预约）")
	public R revenue7d() {
		List<Map<String, Object>> rows = adminAnalyticsService.revenueLastDays(7);
		return R.ok().put("data", rows);
	}

	@GetMapping("/today-revenue")
	@Operation(summary = "今日实时营收")
	public R todayRevenue() {
		return R.ok().put("data", adminAnalyticsService.todayRevenue());
	}
}
