package com.selfstudy.modules.bas.controller;

import com.selfstudy.common.annotation.RequiresPerm;
import com.selfstudy.common.annotation.SysLog;
import com.selfstudy.common.base.PageResult;
import com.selfstudy.common.utils.R;
import com.selfstudy.modules.bas.service.BasAppointmentService;
import com.selfstudy.modules.reservation.SeatBitmapRebuildService;
import com.selfstudy.modules.bas.vo.BasAppointmentAdminVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/sys/bas/appointment")
@Tag(name = "管理-预约")
public class SysBasAppointmentController {

	private static final String CSV_FILENAME_PREFIX = "appointments";

	private final BasAppointmentService basAppointmentService;
	private final SeatBitmapRebuildService seatBitmapRebuildService;

	@GetMapping("/page")
	@RequiresPerm("bas:appointment:list")
	@Operation(summary = "分页列表")
	public R page(
			@RequestParam(value = "page", defaultValue = "1") long page,
			@RequestParam(value = "limit", defaultValue = "10") long limit,
			@RequestParam(value = "bizDate", required = false) String bizDate,
			@RequestParam(value = "seatState", required = false) Integer seatState,
			@RequestParam(value = "keyword", required = false) String keyword) {
		PageResult<BasAppointmentAdminVO> pr = basAppointmentService.adminPage(page, limit, bizDate, seatState, keyword);
		return R.ok().put("list", pr.getList()).put("totalCount", pr.getTotalCount());
	}

	@GetMapping("/export")
	@RequiresPerm("bas:appointment:list")
	@SysLog("导出预约列表CSV")
	@Operation(summary = "导出 CSV（与筛选条件一致，最多5000条）")
	public void export(
			@RequestParam(value = "bizDate", required = false) String bizDate,
			@RequestParam(value = "seatState", required = false) Integer seatState,
			@RequestParam(value = "keyword", required = false) String keyword,
			HttpServletResponse response) throws IOException {
		List<BasAppointmentAdminVO> rows = basAppointmentService.adminExport(bizDate, seatState, keyword);
		String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType("text/csv;charset=UTF-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"" + CSV_FILENAME_PREFIX + "_" + stamp + ".csv\"");
		SimpleDateFormat dtf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try (PrintWriter w = response.getWriter()) {
			w.write('\ufeff');
			w.println("ID,楼层,自习室,座位,预约时段,业务日,预约人,手机,用户ID,状态,创建时间,签到时间,结束学习");
			for (BasAppointmentAdminVO vo : rows) {
				w.println(String.join(",",
						csv(vo.getId()),
						csv(vo.getFloor()),
						csv(vo.getRoomName()),
						csv(vo.getSeatLabel()),
						csv(vo.getSeatDay()),
						csv(vo.getBizDate()),
						csv(vo.getSeatName()),
						csv(vo.getSeatPhone()),
						csv(vo.getUserId()),
						csv(stateLabel(vo.getSeatState())),
						csv(vo.getCreateTime() == null ? "" : dtf.format(vo.getCreateTime())),
						csv(vo.getCheckInAt() == null ? "" : dtf.format(vo.getCheckInAt())),
						csv(vo.getStudyEndAt() == null ? "" : dtf.format(vo.getStudyEndAt()))));
			}
		}
	}

	private static String stateLabel(Integer s) {
		if (s == null) {
			return "";
		}
		return switch (s) {
			case 0 -> "待签到";
			case 1 -> "使用中";
			case 2 -> "已取消";
			case 3 -> "已完成";
			case 4 -> "未签到取消";
			default -> String.valueOf(s);
		};
	}

	private static String csv(Object o) {
		String s = o == null ? "" : String.valueOf(o);
		if (s.indexOf(',') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0 || s.indexOf('"') >= 0) {
			return "\"" + s.replace("\"", "\"\"") + "\"";
		}
		return s;
	}

	@PostMapping("/rebuild-bitmaps")
	@RequiresPerm("bas:appointment:list")
	@SysLog("重建座位位图")
	@Operation(summary = "按数据库待签到/使用中预约重建 Redis 位图（Redis 异常或清空后运维用）")
	public R rebuildBitmaps() {
		int n = seatBitmapRebuildService.rebuildFromDatabase();
		return R.ok().put("rebuiltAppointments", n);
	}

	@PostMapping("/admin-cancel")
	@RequiresPerm("bas:appointment:list")
	@SysLog("强制取消预约")
	@Operation(summary = "强制取消（待签到/使用中），释放座位占用")
	public R adminCancel(@RequestBody Map<String, Long> body) {
		Long id = body == null ? null : body.get("id");
		if (id == null) {
			return R.error("id 不能为空");
		}
		return basAppointmentService.adminCancel(id) ? R.ok() : R.error("取消失败（仅待签到可强制取消）");
	}
}
