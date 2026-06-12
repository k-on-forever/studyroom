package com.selfstudy.modules.bas.controller;

import com.selfstudy.common.annotation.SysLog;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.bas.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/analytics")
@RequiredArgsConstructor
@Tag(name = "管理-营收与利用率")
public class SysAnalyticsController {

	private final AdminAnalyticsService adminAnalyticsService;

	@GetMapping("/revenue/days")
	@Operation(summary = "按日营收序列")
	public R revenueDays(@RequestParam(value = "past", defaultValue = "30") int past) {
		List<Map<String, Object>> rows = adminAnalyticsService.revenueLastDays(past);
		return R.ok().put("data", rows);
	}

	@GetMapping("/revenue/weeks")
	@Operation(summary = "按周汇总营收")
	public R revenueWeeks(@RequestParam(value = "past", defaultValue = "8") int past) {
		return R.ok().put("data", adminAnalyticsService.revenueByWeeks(past));
	}

	@GetMapping("/revenue/months")
	@Operation(summary = "按月汇总营收")
	public R revenueMonths(@RequestParam(value = "past", defaultValue = "12") int past) {
		return R.ok().put("data", adminAnalyticsService.revenueByMonths(past));
	}

	@GetMapping("/utilization")
	@Operation(summary = "热门座位与高峰时段")
	public R utilization(@RequestParam(value = "from", required = false) String from,
			@RequestParam(value = "to", required = false) String to,
			@RequestParam(value = "top", defaultValue = "15") int top) {
		return R.ok().put("data", adminAnalyticsService.utilization(from, to, top));
	}

	@GetMapping("/revenue/export-summary")
	@SysLog("导出营收汇总CSV")
	@Operation(summary = "导出营收汇总 CSV（按月，指定月数，默认 12 个月）")
	public void exportRevenueSummary(@RequestParam(value = "past", defaultValue = "12") int past,
			HttpServletResponse response) throws IOException {
		List<Map<String, Object>> rows = adminAnalyticsService.revenueByMonths(past);
		String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("text/csv;charset=UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"revenue_summary_" + stamp + ".csv\"");
		try (PrintWriter w = response.getWriter()) {
			w.write('\ufeff');
			w.println("月份,会员卡收入(元),按次预约收入(元),合计(元),订单笔数");
			for (Map<String, Object> row : rows) {
				w.println(String.join(",",
						csv(row.get("month")),
						csv(row.get("membershipYuan")),
						csv(row.get("appointmentYuan")),
						csv(row.get("totalYuan")),
						csv(row.get("orderCount"))));
			}
		}
	}

	private static String csv(Object o) {
		if (o == null) return "";
		String s = o instanceof BigDecimal ? ((BigDecimal) o).setScale(2).toString() : String.valueOf(o);
		if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0) {
			return "\"" + s.replace("\"", "\"\"") + "\"";
		}
		return s;
	}
}
