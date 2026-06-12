package com.selfstudy.modules.applet.controller;

import com.selfstudy.common.utils.R;
import com.selfstudy.modules.applet.annotation.Login;
import com.selfstudy.modules.applet.dto.StudyStatsDTO;
import com.selfstudy.modules.applet.service.StudyStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applet/studyStats")
@Tag(name = "学习统计")
public class AppStudyStatsController {

	private final StudyStatsService studyStatsService;

	public AppStudyStatsController(StudyStatsService studyStatsService) {
		this.studyStatsService = studyStatsService;
	}

	@Login
	@GetMapping
	@Operation(summary = "累计时长、连续天数、热力图（最近 84 天）")
	public R stats(@RequestAttribute("userId") Long userId) {
		StudyStatsDTO dto = studyStatsService.statsForUser(userId);
		return R.ok().put("data", dto);
	}
}
