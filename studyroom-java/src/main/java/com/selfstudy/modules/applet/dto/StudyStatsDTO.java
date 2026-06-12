package com.selfstudy.modules.applet.dto;

import lombok.Data;

import java.util.List;

@Data
public class StudyStatsDTO {
	/** 累计学习时长（分钟） */
	private long totalMinutes;
	/** 连续有学习记录的天数（含今日若已完成） */
	private int streakDays;
	/** 最近若干周热力格子：按日期从早到晚，前端可用 7 列网格排版 */
	private List<StudyDayCellDTO> heatmapDays;
}
